import { Component, DestroyRef, Inject, inject } from '@angular/core';
import { MatToolbar } from '@angular/material/toolbar';
import { MemoryPanelComponent } from '../memory-panel/memory-panel.component';
import { DocumentPanelComponent } from '../document-panel/document-panel.component';
import { ChatboxComponent } from '../chatbox/chatbox.component';
import { HttpClient } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [MatToolbar, MemoryPanelComponent, DocumentPanelComponent, ChatboxComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'pulseui';
  currentDocumentId: string = '';
  conversationId: string = 'default';
  private destroyRef = inject(DestroyRef);

  // Metrics data to be shared with memory panel
  metrics: PlatformMetrics = {
    memoryService: '',
    contextSize: 0,
    humanBlockValue: '',
    personaBlockValue: '',
    chatModel: '',
    embeddingModel: '',
    vectorDatabase: ''
  };

  // Memory panel visibility control
  memoryPanelDisabled = true;

  constructor(private httpClient: HttpClient, @Inject(DOCUMENT) private document: Document) {
    this.initMetricsPolling();
  }

  // Method to handle document selection from DocumentPanelComponent
  onDocumentSelected(documentId: string) {
    this.currentDocumentId = documentId;
    console.log('Document selected with ID:', documentId);
  }

  // Moved from memory-panel.component.ts
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

    this.httpClient.get<PlatformMetrics>(`${protocol}//${host}/memory/metrics/${this.conversationId}`)
      .subscribe({
        next: (data) => {
          this.metrics = data;
          this.memoryPanelDisabled = false;
        },
        error: (error) => {
          console.error('Error fetching memory metrics:', error);
          if (error.status === 501) {
            this.memoryPanelDisabled = true;
          }
        }
      });
  }

  // Method to update conversationId (will be called from memory-panel)
  updateConversationId(newId: string) {
    this.conversationId = newId;
    this.fetchMetrics();
  }
}

export interface PlatformMetrics {
  memoryService: string;
  contextSize: number;
  humanBlockValue: string;
  personaBlockValue: string;
  chatModel: string;
  embeddingModel: string;
  vectorDatabase: string;
}

