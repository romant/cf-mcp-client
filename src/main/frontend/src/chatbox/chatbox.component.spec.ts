import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatboxComponent } from './chatbox.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ChatboxComponent', () => {
  let component: ChatboxComponent;
  let fixture: ComponentFixture<ChatboxComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatboxComponent, HttpClientTestingModule]
    })
      .compileComponents();

    fixture = TestBed.createComponent(ChatboxComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
