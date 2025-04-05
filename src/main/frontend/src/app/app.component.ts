import { Component } from '@angular/core';
import { MatToolbar } from '@angular/material/toolbar';
import { MemoryPanelComponent } from '../memory-panel/memory-panel.component';
import { DocumentPanelComponent } from '../document-panel/document-panel.component';
import { ChatboxComponent } from '../chatbox/chatbox.component';

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

  // Method to handle document selection from DocumentPanelComponent
  onDocumentSelected(documentId: string) {
    this.currentDocumentId = documentId;
    console.log('Document selected with ID:', documentId);
  }
}
