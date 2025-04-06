import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatSidenav, MatSidenavModule} from '@angular/material/sidenav';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {FormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from "@angular/material/tooltip";
import {PlatformMetrics} from '../app/app.component';

@Component({
  selector: 'app-memory-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatSidenavModule,
    MatButtonModule,
    MatIconModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule
  ],
  templateUrl: './memory-panel.component.html',
  styleUrl: './memory-panel.component.css'
})
export class MemoryPanelComponent {
  // Input properties from the parent (app) component
  @Input() metrics!: PlatformMetrics;

  // Output to update conversation ID in the parent component
  @Output() conversationIdChanged = new EventEmitter<string>();

  // Local conversationId property with getter/setter
  private _conversationId: string = 'default';
  get conversationId(): string {
    return this._conversationId;
  }

  set conversationId(value: string) {
    this._conversationId = value;
    this.conversationIdChanged.emit(value);
  }

  @ViewChild('sidenav') sidenav!: MatSidenav;

  toggleSidenav() {
    this.sidenav.toggle();
  }
}
