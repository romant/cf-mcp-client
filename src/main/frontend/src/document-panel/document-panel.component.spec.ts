import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DocumentPanelComponent } from './document-panel.component';

describe('DocumentPanelComponent', () => {
  let component: DocumentPanelComponent;
  let fixture: ComponentFixture<DocumentPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DocumentPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
