import { Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatToolbar } from '@angular/material/toolbar';
import { MemoryPanelComponent } from '../memory-panel/memory-panel.component';
import { DocumentPanelComponent } from '../document-panel/document-panel.component';
import { ChatboxComponent } from '../chatbox/chatbox.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [MatButton, MatToolbar, MemoryPanelComponent, DocumentPanelComponent, ChatboxComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'pulseui';
}
