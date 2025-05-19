import { Component, Input, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatListModule } from '@angular/material/list';
import { PlatformMetrics } from '../app/app.component';
import { SidenavService } from '../services/sidenav.service';

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
export class AgentsPanelComponent implements AfterViewInit {
  @Input() metrics!: PlatformMetrics;

  @ViewChild('sidenav') sidenav!: MatSidenav;

  constructor(private sidenavService: SidenavService) {}

  ngAfterViewInit(): void {
    this.sidenavService.registerSidenav('agents', this.sidenav);
  }

  toggleSidenav() {
    this.sidenavService.toggle('agents');
  }
}
