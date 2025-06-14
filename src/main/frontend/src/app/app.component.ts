import { Component, DestroyRef, Inject, inject } from '@angular/core';
import { MatToolbar } from '@angular/material/toolbar';
import { ChatPanelComponent } from '../chat-panel/chat-panel.component';
import { MemoryPanelComponent } from '../memory-panel/memory-panel.component';
import { DocumentPanelComponent } from '../document-panel/document-panel.component';
import { AgentsPanelComponent } from '../agents-panel/agents-panel.component';
import { PromptsPanelComponent } from '../prompts-panel/prompts-panel.component';
import { ChatboxComponent } from '../chatbox/chatbox.component';
import { HttpClient } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [MatToolbar, ChatPanelComponent, MemoryPanelComponent, DocumentPanelComponent, AgentsPanelComponent, PromptsPanelComponent, ChatboxComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'pulseui';
  currentDocumentId: string = '';
  private destroyRef = inject(DestroyRef);

  // Metrics data to be shared with all components
  metrics: PlatformMetrics = {
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
  };

  constructor(private httpClient: HttpClient, @Inject(DOCUMENT) private document: Document) {
    this.initMetricsPolling();
  }

  // Method to handle document selection from DocumentPanelComponent
  onDocumentSelected(documentId: string) {
    this.currentDocumentId = documentId;
    console.log('Document selected with ID:', documentId);
  }

  // Initialize metrics polling
  private initMetricsPolling(): void {
    // Set up interval to fetch metrics every 5 seconds
    this.fetchMetrics();
    interval(5000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.fetchMetrics();
      });
  }

  fetchMetrics() {
    let host: string;
    let protocol: string;

    if (this.document.location.hostname == 'localhost') {
      host = 'localhost:8080';
    } else {
      host = this.document.location.host;
    }
    protocol = this.document.location.protocol;

    this.httpClient.get<PlatformMetrics>(`${protocol}//${host}/metrics`)
      .subscribe({
        next: (data) => {
          this.metrics = data;
        },
        error: (error) => {
          console.error('Error fetching memory metrics:', error);
          this.metrics = {
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
          };
        }
      });
  }
}

export interface Tool {
  name: string;
  description: string;
}

export interface Agent {
  name: string;
  healthy: boolean;
  tools: Tool[];
}

export interface PromptArgument {
  name: string;
  description: string;
  required: boolean;
  defaultValue?: any;
  schema?: any;
}

export interface McpPrompt {
  serverId: string;
  name: string;
  description: string;
  arguments: PromptArgument[];
}

export interface EnhancedPromptMetrics {
  totalPrompts: number;
  serversWithPrompts: number;
  available: boolean;
  promptsByServer: { [serverId: string]: McpPrompt[] };
}

export interface PlatformMetrics {
  conversationId: string;
  chatModel: string;
  embeddingModel: string;
  vectorStoreName: string;
  agents: Agent[];
  prompts: EnhancedPromptMetrics;
}
