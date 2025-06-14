import { Component, Input, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatListModule } from '@angular/material/list';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatBadgeModule } from '@angular/material/badge';
import { MatChipsModule } from '@angular/material/chips';
import { PlatformMetrics, McpPrompt } from '../app/app.component';
import { SidenavService } from '../services/sidenav.service';

@Component({
  selector: 'app-prompts-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatSidenavModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatListModule,
    MatExpansionModule,
    MatBadgeModule,
    MatChipsModule
  ],
  templateUrl: './prompts-panel.component.html',
  styleUrl: './prompts-panel.component.css'
})
export class PromptsPanelComponent implements AfterViewInit {
  @Input() metrics!: PlatformMetrics;

  @ViewChild('sidenav') sidenav!: MatSidenav;

  constructor(private sidenavService: SidenavService) {}

  ngAfterViewInit(): void {
    this.sidenavService.registerSidenav('prompts', this.sidenav);
  }

  toggleSidenav() {
    this.sidenavService.toggle('prompts');
  }

  /**
   * Get the overall status class for prompts
   */
  getOverallStatusClass(): string {
    if (this.metrics && this.metrics.prompts && this.metrics.prompts.available) {
      return 'status-green';
    }
    return 'status-red';
  }

  /**
   * Get the overall status icon for prompts
   */
  getOverallStatusIcon(): string {
    if (this.metrics && this.metrics.prompts && this.metrics.prompts.available) {
      return 'check_circle';
    }
    return 'error';
  }

  /**
   * Get the overall status text for prompts
   */
  getOverallStatusText(): string {
    if (this.metrics && this.metrics.prompts && this.metrics.prompts.available && this.metrics.prompts.totalPrompts > 0) {
      return `${this.metrics.prompts.totalPrompts} Available`;
    }
    return 'Not Available';
  }

  /**
   * Check if we should show empty state
   */
  shouldShowEmptyState(): boolean {
    return !this.metrics || !this.metrics.prompts || !this.metrics.prompts.available || this.metrics.prompts.totalPrompts === 0;
  }

  /**
   * Check if we have prompts to display
   */
  hasPrompts(): boolean {
    return this.metrics && this.metrics.prompts && this.metrics.prompts.available && this.metrics.prompts.totalPrompts > 0;
  }

  /**
   * Get total prompts count
   */
  getTotalPrompts(): number {
    return this.metrics && this.metrics.prompts ? this.metrics.prompts.totalPrompts : 0;
  }

  /**
   * Get server IDs that have prompts
   */
  getServerIds(): string[] {
    if (!this.metrics || !this.metrics.prompts || !this.metrics.prompts.promptsByServer) {
      return [];
    }
    return Object.keys(this.metrics.prompts.promptsByServer).sort();
  }

  /**
   * Get prompts for a specific server
   */
  getPromptsForServer(serverId: string): McpPrompt[] {
    if (!this.metrics || !this.metrics.prompts || !this.metrics.prompts.promptsByServer) {
      return [];
    }
    return this.metrics.prompts.promptsByServer[serverId] || [];
  }

  /**
   * Get the display name for a server (server name if available, otherwise server ID)
   */
  getServerDisplayName(serverId: string): string {
    const prompts = this.getPromptsForServer(serverId);
    if (prompts.length > 0 && prompts[0].serverName) {
      return prompts[0].serverName;
    }
    return serverId;
  }

  /**
   * Get the count of required arguments for a prompt
   */
  getRequiredArgCount(prompt: McpPrompt): number {
    if (!prompt.arguments) {
      return 0;
    }
    return prompt.arguments.filter(arg => arg.required).length;
  }

  /**
   * Get the total argument count for a prompt
   */
  getTotalArgCount(prompt: McpPrompt): number {
    return prompt.arguments?.length || 0;
  }

  /**
   * Check if a prompt has any arguments
   */
  hasArguments(prompt: McpPrompt): boolean {
    return prompt.arguments && prompt.arguments.length > 0;
  }

  /**
   * Handle prompt selection (placeholder for future chat integration)
   */
  selectPrompt(prompt: McpPrompt): void {
    console.log('Selected prompt:', prompt);
    // TODO: Integrate with chat interface
    // This could emit an event or call a service to handle prompt selection
  }
}
