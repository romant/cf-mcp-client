import { Component, Input, ViewChild, AfterViewInit, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatListModule } from '@angular/material/list';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatBadgeModule } from '@angular/material/badge';
import { MatChipsModule } from '@angular/material/chips';
import { PlatformMetrics } from '../app/app.component';
import { SidenavService } from '../services/sidenav.service';
import { PromptService, McpPrompt } from '../services/prompt.service';

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
export class PromptsPanelComponent implements AfterViewInit, OnInit {
  @Input() metrics!: PlatformMetrics;

  @ViewChild('sidenav') sidenav!: MatSidenav;

  // Component state
  promptsByServer: { [serverId: string]: McpPrompt[] } = {};
  serverIds: string[] = [];
  totalPrompts = 0;
  loading = true;
  error: string | null = null;

  constructor(
    private sidenavService: SidenavService,
    private promptService: PromptService
  ) {}

  ngOnInit(): void {
    this.loadPrompts();
  }

  ngAfterViewInit(): void {
    this.sidenavService.registerSidenav('prompts', this.sidenav);
  }

  toggleSidenav() {
    this.sidenavService.toggle('prompts');
  }

  /**
   * Load prompts from the backend
   */
  loadPrompts(): void {
    this.loading = true;
    this.error = null;

    this.promptService.getPromptsByServer().subscribe({
      next: (data) => {
        this.promptsByServer = data;
        this.serverIds = Object.keys(data).sort();
        this.totalPrompts = Object.values(data)
          .reduce((total, prompts) => total + prompts.length, 0);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading prompts:', error);
        this.error = 'Failed to load prompts';
        this.loading = false;
        this.promptsByServer = {};
        this.serverIds = [];
        this.totalPrompts = 0;
      }
    });
  }

  /**
   * Get the overall status class for prompts
   */
  getOverallStatusClass(): string {
    if (this.metrics?.prompts?.available) {
      return 'status-green';
    }
    return 'status-red';
  }

  /**
   * Get the overall status icon for prompts
   */
  getOverallStatusIcon(): string {
    if (this.metrics?.prompts?.available) {
      return 'check_circle';
    }
    return 'error';
  }

  /**
   * Get the overall status text for prompts
   */
  getOverallStatusText(): string {
    if (this.metrics?.prompts?.available) {
      return `${this.totalPrompts} Available`;
    }
    return 'Not Available';
  }

  /**
   * Get prompts for a specific server
   */
  getPromptsForServer(serverId: string): McpPrompt[] {
    return this.promptsByServer[serverId] || [];
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

  /**
   * Refresh prompts data
   */
  refreshPrompts(): void {
    this.loadPrompts();
  }
}
