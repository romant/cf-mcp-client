import { Injectable, Inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PromptService {
  private baseUrl: string;

  constructor(
    private httpClient: HttpClient,
    @Inject(DOCUMENT) private document: Document
  ) {
    // Determine the base URL for API calls
    if (this.document.location.hostname === 'localhost') {
      this.baseUrl = `${this.document.location.protocol}//localhost:8080`;
    } else {
      this.baseUrl = `${this.document.location.protocol}//${this.document.location.host}`;
    }
  }

  /**
   * Get all available prompts from all MCP servers
   */
  getAllPrompts(): Observable<McpPrompt[]> {
    return this.httpClient.get<McpPrompt[]>(`${this.baseUrl}/prompts`);
  }

  /**
   * Get prompts grouped by server
   */
  getPromptsByServer(): Observable<{ [serverId: string]: McpPrompt[] }> {
    return this.httpClient.get<{ [serverId: string]: McpPrompt[] }>(`${this.baseUrl}/prompts/by-server`);
  }

  /**
   * Get prompts from a specific server
   */
  getPromptsByServerId(serverId: string): Observable<McpPrompt[]> {
    return this.httpClient.get<McpPrompt[]>(`${this.baseUrl}/prompts/servers/${serverId}`);
  }

  /**
   * Get details for a specific prompt
   */
  getPromptById(promptId: string): Observable<McpPrompt> {
    const encodedPromptId = encodeURIComponent(promptId);
    return this.httpClient.get<McpPrompt>(`${this.baseUrl}/prompts/${encodedPromptId}`);
  }

  /**
   * Resolve a prompt with provided arguments
   */
  resolvePrompt(request: PromptResolutionRequest): Observable<ResolvedPrompt> {
    return this.httpClient.post<ResolvedPrompt>(`${this.baseUrl}/prompts/resolve`, request);
  }

  /**
   * Get prompt system status
   */
  getPromptStatus(): Observable<PromptStatus> {
    return this.httpClient.get<PromptStatus>(`${this.baseUrl}/prompts/status`);
  }
}

// Interfaces matching the backend models
export interface McpPrompt {
  serverId: string;
  name: string;
  description: string;
  arguments: PromptArgument[];
  id?: string; // computed: serverId:name
}

export interface PromptArgument {
  name: string;
  description: string;
  required: boolean;
  defaultValue?: any;
  schema?: any; // JSON Schema for validation
}

export interface PromptResolutionRequest {
  promptId: string;
  arguments: { [key: string]: any };
}

export interface ResolvedPrompt {
  content: string;
  messages: PromptMessage[];
  metadata: { [key: string]: any };
}

export interface PromptMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface PromptStatus {
  promptCount: number;
  serverCount: number;
  available: boolean;
}
