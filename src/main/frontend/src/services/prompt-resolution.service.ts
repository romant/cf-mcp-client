import { Injectable, Inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { Observable } from 'rxjs';

export interface PromptResolutionRequest {
  promptId: string;
  arguments: { [key: string]: any };
}

export interface PromptMessage {
  role: string;
  content: string;
}

export interface ResolvedPrompt {
  content: string;
  messages?: PromptMessage[];
  metadata?: { [key: string]: any };
}

@Injectable({
  providedIn: 'root'
})
export class PromptResolutionService {

  constructor(
    private httpClient: HttpClient,
    @Inject(DOCUMENT) private document: Document
  ) {}

  /**
   * Resolve a prompt with the provided arguments
   */
  resolvePrompt(request: PromptResolutionRequest): Observable<ResolvedPrompt> {
    const { protocol, host } = this.getApiBaseUrl();

    return this.httpClient.post<ResolvedPrompt>(
      `${protocol}//${host}/prompts/resolve`,
      request
    );
  }

  /**
   * Get the API base URL based on current environment
   */
  private getApiBaseUrl(): { protocol: string; host: string } {
    let host: string;
    let protocol: string;

    if (this.document.location.hostname === 'localhost') {
      host = 'localhost:8080';
    } else {
      host = this.document.location.host;
    }
    protocol = this.document.location.protocol;

    return { protocol, host };
  }
}
