import { Component, Input, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatListModule } from '@angular/material/list';
import { PlatformMetrics } from '../app/app.component';

@Component({
  selector: 'app-agents-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatSidenavModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatListModule
  ],
  templateUrl: './agents-panel.component.html',
  styleUrl: './agents-panel.component.css'
})
export class AgentsPanelComponent {
  @Input() metrics: PlatformMetrics = {
    memoryService: '',
    contextSize: 0,
    humanBlockValue: '',
    personaBlockValue: '',
    chatModel: '',
    embeddingModel: '',
    vectorDatabase: '',
    agents: []
  };

  @ViewChild('sidenav') sidenav!: MatSidenav;

  toggleSidenav() {
    this.sidenav.toggle();
  }
}
