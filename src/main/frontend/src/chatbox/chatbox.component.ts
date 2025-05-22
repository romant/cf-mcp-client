import {
  afterNextRender,
  Component,
  ElementRef,
  Inject,
  Injector,
  Input,
  NgZone,
  runInInjectionContext,
  ViewChild
} from '@angular/core';
import {DOCUMENT} from '@angular/common';
import {HttpClient, HttpParams} from '@angular/common/http';
import {MatButton} from '@angular/material/button';
import {FormsModule} from '@angular/forms';
import {MatFormField} from '@angular/material/form-field';
import {MatInput, MatInputModule} from '@angular/material/input';
import {MatCard, MatCardContent} from '@angular/material/card';
import {MarkdownComponent} from 'ngx-markdown';
import {PlatformMetrics} from '../app/app.component';

@Component({
  selector: 'app-chatbox',
  standalone: true,
  imports: [MatButton, FormsModule, MatFormField, MatInput, MatCard, MatCardContent, MarkdownComponent, MatInputModule],
  templateUrl: './chatbox.component.html',
  styleUrl: './chatbox.component.css'
})
export class ChatboxComponent {
  @Input() documentId: string = '';
  @Input() metrics!: PlatformMetrics;

  messages: ChatboxMessage[] = [];
  chatMessage = '';
  host = '';
  protocol = '';

  @ViewChild("chatboxMessages") private chatboxMessages?: ElementRef<HTMLDivElement>;

  constructor(private httpClient: HttpClient,
              private injector: Injector,
              @Inject(DOCUMENT) private document: Document,
              private ngZone: NgZone) {
    if (this.document.location.hostname == 'localhost') {
      this.host = 'localhost:8080';
    } else this.host = this.document.location.host;
    this.protocol = this.document.location.protocol;
  }

  async sendChatMessage() {
    if (!this.chatMessage.trim()) return;

    this.messages.push({text: this.chatMessage, persona: 'user'});
    let botMessage: ChatboxMessage = {text: '', persona: 'bot', typing: true};
    this.messages.push(botMessage);
    this.scrollChatToBottom();

    // Create HTTP params without conversationId (handled by session)
    let params: HttpParams = new HttpParams().set('chat', this.chatMessage);
    if (this.documentId.length > 0) {
      params = params.set('documentId', this.documentId);
    }

    this.chatMessage = '';

    try {
      if (this.metrics.chatModel == '') {
        this.ngZone.run(() => {
          if (botMessage.text === '') {
            botMessage.typing = false;
            botMessage.text = 'No chat model available';
          }
          this.scrollChatToBottom();
        });
        return;
      }

      // Use Server-Sent Events for streaming
      await this.streamChatResponse(params, botMessage);

    } catch (error) {
      this.ngZone.run(() => {
        botMessage.typing = false;
        botMessage.text = "Sorry, I encountered an error processing your request.";
        console.error('Chat request error:', error);
        this.scrollChatToBottom();
      });
    }
  }

  private streamChatResponse(params: HttpParams, botMessage: ChatboxMessage): Promise<void> {
    return new Promise((resolve, reject) => {
      const url = `${this.protocol}//${this.host}/chat?${params.toString()}`;

      const eventSource = new EventSource(url, {
        withCredentials: true
      });

      let isFirstChunk = true;

      eventSource.onmessage = (event) => {
        this.ngZone.run(() => {
          if (isFirstChunk) {
            botMessage.typing = false;
            isFirstChunk = false;
          }

          // Handle JSON chunks
          let chunk: string;
          try {
            const parsed = JSON.parse(event.data);
            chunk = parsed.content || event.data;
          } catch (e) {
            chunk = event.data;
          }

          if (chunk && chunk.length > 0) {
            botMessage.text += chunk;
          }

          // Scroll to bottom after each update
          setTimeout(() => {
            this.scrollChatToBottom();
          }, 0);
        });
      };

      eventSource.onerror = (error) => {
        console.error('EventSource error:', error);
        eventSource.close();
        this.ngZone.run(() => {
          if (botMessage.text === '') {
            botMessage.typing = false;
            botMessage.text = "Sorry, I encountered an error processing your request.";
          }
          this.scrollChatToBottom();
        });
        reject(error);
      };

      eventSource.onopen = () => {
        console.log('EventSource connection opened');
      };

      // Listen for the end of stream
      eventSource.addEventListener('close', () => {
        eventSource.close();
        resolve();
      });

      // Also close on 'error' event to handle completion
      eventSource.addEventListener('error', (event: any) => {
        if (event.readyState === EventSource.CLOSED) {
          eventSource.close();
          resolve();
        }
      });
    });
  }

  scrollChatToBottom() {
    runInInjectionContext(this.injector, () => {
      afterNextRender({
        read: () => {
          if (this.chatboxMessages) {
            this.chatboxMessages.nativeElement.lastElementChild?.scrollIntoView({
              behavior: "smooth",
              block: "start"
            });
          }
        }
      })
    })
  }
}

interface ChatboxMessage {
  text: string;
  persona: 'user' | 'bot';
  typing?: boolean;
}
