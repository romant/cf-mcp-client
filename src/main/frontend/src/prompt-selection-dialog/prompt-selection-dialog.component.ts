import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormControl, Validators } from '@angular/forms';
import { McpPrompt, PromptArgument, PlatformMetrics } from '../app/app.component';

export interface PromptSelectionDialogData {
  metrics: PlatformMetrics;
}

export interface PromptSelectionResult {
  prompt: McpPrompt;
  arguments: { [key: string]: any };
}

@Component({
  selector: 'app-prompt-selection-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatCardModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './prompt-selection-dialog.component.html',
  styleUrl: './prompt-selection-dialog.component.css'
})
export class PromptSelectionDialogComponent implements OnInit {
  searchTerm: string = '';
  selectedPrompt: McpPrompt | null = null;
  argumentForm: FormGroup | null = null;
  showArgumentForm: boolean = false;

  constructor(
    public dialogRef: MatDialogRef<PromptSelectionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PromptSelectionDialogData,
    private formBuilder: FormBuilder
  ) {}

  ngOnInit(): void {
    // Dialog is ready
  }

  /**
   * Get all prompts grouped by server
   */
  getServerIds(): string[] {
    if (!this.data.metrics || !this.data.metrics.prompts || !this.data.metrics.prompts.promptsByServer) {
      return [];
    }
    return Object.keys(this.data.metrics.prompts.promptsByServer)
      .filter(serverId => this.getPromptsForServer(serverId).length > 0)
      .sort();
  }

  /**
   * Get prompts for a specific server, filtered by search term
   */
  getPromptsForServer(serverId: string): McpPrompt[] {
    if (!this.data.metrics || !this.data.metrics.prompts || !this.data.metrics.prompts.promptsByServer) {
      return [];
    }

    const serverPrompts = this.data.metrics.prompts.promptsByServer[serverId] || [];

    if (!this.searchTerm.trim()) {
      return serverPrompts;
    }

    const searchLower = this.searchTerm.toLowerCase();
    return serverPrompts.filter(prompt =>
      prompt.name.toLowerCase().includes(searchLower) ||
      (prompt.description && prompt.description.toLowerCase().includes(searchLower))
    );
  }

  /**
   * Get the display name for a server
   */
  getServerDisplayName(serverId: string): string {
    const prompts = this.data.metrics.prompts.promptsByServer[serverId];
    if (prompts && prompts.length > 0 && prompts[0].serverName) {
      return prompts[0].serverName;
    }
    return serverId;
  }

  /**
   * Handle prompt selection
   */
  selectPrompt(prompt: McpPrompt): void {
    this.selectedPrompt = prompt;
    this.showArgumentForm = false;

    if (this.hasArguments(prompt)) {
      this.createArgumentForm(prompt);
      this.showArgumentForm = true;
    } else {
      // No arguments needed, can proceed directly
      this.proceedWithPrompt();
    }
  }

  /**
   * Check if prompt has arguments
   */
  hasArguments(prompt: McpPrompt): boolean {
    return prompt.arguments && prompt.arguments.length > 0;
  }

  /**
   * Get argument summary for display
   */
  getArgumentSummary(prompt: McpPrompt): string {
    if (!prompt.arguments || prompt.arguments.length === 0) {
      return '';
    }

    const required = prompt.arguments.filter(arg => arg.required).length;
    const optional = prompt.arguments.length - required;

    const parts = [];
    if (required > 0) parts.push(`${required} required`);
    if (optional > 0) parts.push(`${optional} optional`);

    return parts.join(', ') + ' args';
  }

  /**
   * Create dynamic form for prompt arguments
   */
  private createArgumentForm(prompt: McpPrompt): void {
    const formControls: { [key: string]: FormControl } = {};

    if (prompt.arguments) {
      for (const arg of prompt.arguments) {
        const validators = arg.required ? [Validators.required] : [];
        const defaultValue = arg.defaultValue || '';
        formControls[arg.name] = new FormControl(defaultValue, validators);
      }
    }

    this.argumentForm = this.formBuilder.group(formControls);
  }

  /**
   * Handle form submission or direct prompt usage
   */
  proceedWithPrompt(): void {
    if (!this.selectedPrompt) return;

    let promptArguments: { [key: string]: any } = {};

    if (this.argumentForm && this.showArgumentForm) {
      if (this.argumentForm.invalid) {
        // Mark all fields as touched to show validation errors
        this.argumentForm.markAllAsTouched();
        return;
      }
      promptArguments = this.argumentForm.value;
    }

    const result: PromptSelectionResult = {
      prompt: this.selectedPrompt,
      arguments: promptArguments
    };

    this.dialogRef.close(result);
  }

  /**
   * Cancel selection
   */
  cancel(): void {
    this.dialogRef.close();
  }

  /**
   * Go back to prompt selection from argument form
   */
  goBack(): void {
    this.selectedPrompt = null;
    this.argumentForm = null;
    this.showArgumentForm = false;
  }

  /**
   * Check if we should show empty state
   */
  shouldShowEmptyState(): boolean {
    return this.getServerIds().length === 0 ||
      this.getServerIds().every(serverId => this.getPromptsForServer(serverId).length === 0);
  }

  /**
   * Get total filtered prompts count
   */
  getTotalFilteredPrompts(): number {
    return this.getServerIds().reduce((total, serverId) =>
      total + this.getPromptsForServer(serverId).length, 0);
  }
}
