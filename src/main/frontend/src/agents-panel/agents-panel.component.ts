import { Component, Input, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatListModule } from '@angular/material/list';
import { PlatformMetrics, Agent } from '../app/app.component';
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

  /**
   * Get the overall status class for styling
   */
  getOverallStatusClass(): string {
    if (this.metrics.agents.length === 0) {
      return 'status-red';
    }

    const hasUnhealthy = this.metrics.agents.some(agent => !agent.healthy);
    const hasHealthy = this.metrics.agents.some(agent => agent.healthy);

    if (hasUnhealthy && hasHealthy) {
      return 'status-orange'; // Mixed health status
    } else if (hasHealthy) {
      return 'status-green'; // All healthy
    } else {
      return 'status-red'; // All unhealthy
    }
  }

  /**
   * Get the overall status icon
   */
  getOverallStatusIcon(): string {
    if (this.metrics.agents.length === 0) {
      return 'error';
    }

    const hasUnhealthy = this.metrics.agents.some(agent => !agent.healthy);
    const hasHealthy = this.metrics.agents.some(agent => agent.healthy);

    if (hasUnhealthy && hasHealthy) {
      return 'warning'; // Mixed health status
    } else if (hasHealthy) {
      return 'check_circle'; // All healthy
    } else {
      return 'error'; // All unhealthy
    }
  }

  /**
   * Get the overall status text
   */
  getOverallStatusText(): string {
    if (this.metrics.agents.length === 0) {
      return 'Not Available';
    }

    const healthyCount = this.metrics.agents.filter(agent => agent.healthy).length;
    const totalCount = this.metrics.agents.length;

    if (healthyCount === totalCount) {
      return 'All Healthy';
    } else if (healthyCount === 0) {
      return 'All Unhealthy';
    } else {
      return `${healthyCount}/${totalCount} Healthy`;
    }
  }
}
