import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { PromptsPanelComponent } from './prompts-panel.component';

describe('PromptsPanelComponent', () => {
  let component: PromptsPanelComponent;
  let fixture: ComponentFixture<PromptsPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        PromptsPanelComponent,
        HttpClientTestingModule,
        NoopAnimationsModule
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(PromptsPanelComponent);
    component = fixture.componentInstance;

    // Mock the metrics input
    component.metrics = {
      conversationId: 'test-conversation',
      chatModel: 'test-model',
      embeddingModel: 'test-embedding',
      vectorStoreName: 'test-vector-store',
      agents: [],
      prompts: {
        totalPrompts: 0,
        serversWithPrompts: 0,
        available: false
      }
    };

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
