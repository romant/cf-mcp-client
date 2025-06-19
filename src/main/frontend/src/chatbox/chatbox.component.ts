import {
  afterNextRender,
  Component,
  ElementRef,
  Inject,
  Injector,
  Input,
  NgZone,
  runInInjectionContext,
  ViewChild,
  signal,
  computed,
  effect
} from '@angular/core';
import {DOCUMENT} from '@angular/common';
import {HttpParams} from '@angular/common/http';
import {MatButton, MatIconButton} from '@angular/material/button';
import {FormsModule} from '@angular/forms';
import {MatFormField} from '@angular/material/form-field';
import {MatInput, MatInputModule} from '@angular/material/input';
import {MatCard, MatCardContent} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatDialog} from '@angular/material/dialog';
import {MarkdownComponent} from 'ngx-markdown';
import {PlatformMetrics} from '../app/app.component';
import {
  PromptSelectionDialogComponent,
  PromptSelectionResult
} from '../prompt-selection-dialog/prompt-selection-dialog.component';
import {PromptResolutionService} from '../services/prompt-resolution.service';
import {MatTooltip} from '@angular/material/tooltip';

interface ChatboxMessage {
  text: string;
  persona: 'user' | 'bot';
  typing?: boolean;
}

@Component({
  selector: 'app-chatbox',
  standalone: true,
  imports: [MatButton, FormsModule, MatFormField, MatInput, MatCard, MatCardContent, MarkdownComponent, MatInputModule, MatIconModule, MatIconButton, MatTooltip],
  templateUrl: './chatbox.component.html',
  styleUrl: './chatbox.component.css'
})
export class ChatboxComponent {
  @Input() documentId: string = '';

  // Convert metrics input to signal for reactivity
  @Input() set metrics(value: PlatformMetrics) {
    this._metricsInput.set(value);
  }
  get metrics(): PlatformMetrics {
    return this._metricsInput();
  }

  private readonly _metricsInput = signal<PlatformMetrics>({
    conversationId: '',
    chatModel: '',
    embeddingModel: '',
    vectorStoreName: '',
    agents: [],
    prompts: {
      totalPrompts: 0,
      serversWithPrompts: 0,
      available: false,
      promptsByServer: {}
    }
  });

  // State signals
  private readonly _messages = signal<ChatboxMessage[]>([]);
  private readonly _chatMessage = signal<string>('');
  private readonly _isStreaming = signal<boolean>(false);
  private readonly _isConnecting = signal<boolean>(false);

  // Public readonly signals
  readonly messages = this._messages.asReadonly();
  readonly chatMessage = this._chatMessage.asReadonly();

  // Computed signals for derived state
  readonly canSendMessage = computed(() =>
      this._chatMessage().trim().length > 0 &&
      !this._isStreaming() &&
      !this._isConnecting()
    // Removed chat model check - let users try even without a model
  );

  readonly isBusy = computed(() =>
    this._isStreaming() || this._isConnecting()
  );

  readonly lastBotMessage = computed(() => {
    const msgs = this._messages();
    for (let i = msgs.length - 1; i >= 0; i--) {
      if (msgs[i].persona === 'bot') {
        return msgs[i];
      }
    }
    return null;
  });

  readonly hasAvailablePrompts = computed(() => {
    const metrics = this._metricsInput();
    return metrics &&
      metrics.prompts &&
      metrics.prompts.available &&
      metrics.prompts.totalPrompts > 0;
  });

  readonly sendButtonText = computed(() => {
    if (this._isConnecting()) return 'Connecting...';
    if (this._isStreaming()) return 'Streaming...';
    return 'Send';
  });

  readonly sendButtonTooltip = computed(() => {
    if (this._isStreaming() || this._isConnecting()) {
      return 'Please wait for current message to complete';
    }
    if (!this.canSendMessage()) {
      return 'Enter a message to send';
    }
    return 'Send message';
  });

  private host = '';
  private protocol = '';

  @ViewChild("chatboxMessages") private chatboxMessages?: ElementRef<HTMLDivElement>;

  constructor(
    private injector: Injector,
    @Inject(DOCUMENT) private document: Document,
    private ngZone: NgZone,
    private dialog: MatDialog,
    private promptResolutionService: PromptResolutionService
  ) {
    // Set up host and protocol
    if (this.document.location.hostname === 'localhost') {
      this.host = 'localhost:8080';
    } else {
      this.host = this.document.location.host;
    }
    this.protocol = this.document.location.protocol;

    // Effects for side effects
    this.setupEffects();
  }

  private setupEffects(): void {
    // Auto-scroll when messages change
    effect(() => {
      const messages = this._messages();
      if (messages.length > 0) {
        // Use a small delay to ensure DOM is updated
        setTimeout(() => this.scrollChatToBottom(), 10);
      }
    });

    // Log streaming state changes for debugging
    effect(() => {
      const streaming = this._isStreaming();
      const connecting = this._isConnecting();
      if (streaming || connecting) {
        console.log('Chat state:', { streaming, connecting });
      }
    });

    // Debug metrics changes
    effect(() => {
      const metrics = this._metricsInput();
      console.log('Chatbox received metrics:', {
        hasPrompts: !!metrics.prompts,
        available: metrics.prompts?.available,
        totalPrompts: metrics.prompts?.totalPrompts,
        hasAvailablePrompts: this.hasAvailablePrompts()
      });
    });

    // Validate chat model availability
    effect(() => {
      const metrics = this._metricsInput();
      const hasModel = metrics.chatModel !== '';
      if (!hasModel && this._chatMessage().trim().length > 0) {
        console.warn('Chat model not available');
      }
    });
  }

  // Method to update chat message (for template binding)
  updateChatMessage(message: string): void {
    this._chatMessage.set(message);
  }

  async sendChatMessage(): Promise<void> {
    if (!this.canSendMessage()) return;

    const messageText = this._chatMessage();

    // Add user message to the conversation
    this.addUserMessage(messageText);

    // Create and add bot message placeholder
    this.addBotMessagePlaceholder();
// Clear input and set connecting state
    this._chatMessage.set('');
    this._isConnecting.set(true);

    // Create HTTP params
    let params: HttpParams = new HttpParams().set('chat', messageText);
    if (this.documentId.length > 0) {
      params = params.set('documentId', this.documentId);
    }

    try {
      // Check if chat model is available
      const metrics = this._metricsInput();
      if (!metrics.chatModel) {
        this.handleChatError('No chat model is available');
        return;
      }

      // Stream the chat response
      await this.streamChatResponse(params);

    } catch (error) {
      console.error('Chat request error:', error);
      this.handleChatError('Sorry, I encountered an error processing your request.');
    }
  }

  /**
   * Open the prompt selection dialog
   */
  openPromptSelection(): void {
    if (!this.hasAvailablePrompts()) {
      return;
    }

    const dialogRef = this.dialog.open(PromptSelectionDialogComponent, {
      data: { metrics: this._metricsInput() },
      width: '90vw',
      maxWidth: '800px',
      maxHeight: '80vh',
      panelClass: 'prompt-selection-dialog-container'
    });

    dialogRef.afterClosed().subscribe((result: PromptSelectionResult) => {
      if (result) {
        this.handlePromptSelection(result);
      }
    });
  }

  private addUserMessage(text: string): void {
    this._messages.update(msgs => [
      ...msgs,
      { text, persona: 'user' }
    ]);
  }

  private addBotMessagePlaceholder(): ChatboxMessage {
    const botMessage: ChatboxMessage = { text: '', persona: 'bot', typing: true };
    this._messages.update(msgs => [...msgs, botMessage]);
    return botMessage;
  }

  private updateBotMessage(content: string, typing: boolean = false): void {
    this._messages.update(msgs => {
      const lastIndex = msgs.length - 1;
      if (lastIndex >= 0 && msgs[lastIndex].persona === 'bot') {
        const updatedMessage = {
          ...msgs[lastIndex],
          text: msgs[lastIndex].text + content,
          typing
        };
        return [
          ...msgs.slice(0, lastIndex),
          updatedMessage
        ];
      }
      return msgs;
    });
  }

  private setBotMessageTyping(typing: boolean): void {
    this._messages.update(msgs => {
      const lastIndex = msgs.length - 1;
      if (lastIndex >= 0 && msgs[lastIndex].persona === 'bot') {
        const updatedMessage = {
          ...msgs[lastIndex],
          typing
        };
        return [
          ...msgs.slice(0, lastIndex),
          updatedMessage
        ];
      }
      return msgs;
    });
  }

  private handleChatError(errorMessage: string): void {
    this.ngZone.run(() => {
      this.setBotMessageTyping(false);
      if (this.lastBotMessage()?.text === '') {
        this._messages.update(msgs => {
          const lastIndex = msgs.length - 1;
          if (lastIndex >= 0 && msgs[lastIndex].persona === 'bot') {
            return [
              ...msgs.slice(0, lastIndex),
              { ...msgs[lastIndex], text: errorMessage, typing: false }
            ];
          }
          return msgs;
        });
      }
      this._isStreaming.set(false);
      this._isConnecting.set(false);
    });
  }

  /**
   * Handle the result from prompt selection dialog
   */
  private handlePromptSelection(result: PromptSelectionResult): void {
    const promptId = `${result.prompt.serverId}:${result.prompt.name}`;

    // If prompt has no arguments, use it directly
    if (!result.prompt.arguments || result.prompt.arguments.length === 0) {
      this.insertPromptIntoChat(result.prompt.name, result.prompt.description);
      return;
    }

    // Resolve prompt with arguments
    this.promptResolutionService.resolvePrompt({
      promptId: promptId,
      arguments: result.arguments
    }).subscribe({
      next: (resolvedPrompt) => {
        this.insertResolvedPromptIntoChat(resolvedPrompt);
      },
      error: (error) => {
        console.error('Error resolving prompt:', error);
        // Fallback: insert prompt name
        this.insertPromptIntoChat(result.prompt.name, 'Failed to resolve prompt with arguments');
      }
    });
  }

  private insertPromptIntoChat(promptName: string, description?: string): void {
    const content = description || promptName;
    this._chatMessage.set(content);
    this.sendChatMessage();
  }

  private insertResolvedPromptIntoChat(resolvedPrompt: any): void {
    let content: string;

    if (resolvedPrompt.messages && resolvedPrompt.messages.length > 0) {
      // Use structured messages
      content = resolvedPrompt.messages
        .map((msg: any) => msg.content)
        .join('\n\n');
    } else if (resolvedPrompt.content) {
      // Use direct content
      content = resolvedPrompt.content;
    } else {
      content = 'Resolved prompt content';
    }

    this._chatMessage.set(content);
    this.sendChatMessage();
  }

  private streamChatResponse(params: HttpParams): Promise<void> {
    return new Promise((resolve, reject) => {
      const url = `${this.protocol}//${this.host}/chat?${params.toString()}`;

      const eventSource = new EventSource(url, {
        withCredentials: true
      });

      let isFirstChunk = true;

      eventSource.onopen = () => {
        console.log('EventSource connection opened');
        this.ngZone.run(() => {
          this._isConnecting.set(false);
          this._isStreaming.set(true);
        });
      };

      eventSource.onmessage = (event) => {
        this.ngZone.run(() => {
          if (isFirstChunk) {
            this.setBotMessageTyping(false);
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
            this.updateBotMessage(chunk);
          }
        });
      };

      eventSource.onerror = (error) => {
        console.error('EventSource error:', error);
        eventSource.close();
        this.handleChatError('Sorry, I encountered an error processing your request.');
        reject(error);
      };

      // Listen for successful completion
      eventSource.addEventListener('close', () => {
        eventSource.close();
        this.ngZone.run(() => {
          this._isStreaming.set(false);
          this._isConnecting.set(false);
        });
        resolve();
      });
    });
  }

  private scrollChatToBottom(): void {
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
      });
    });
  }
}
