import { Component, DestroyRef, EventEmitter, Inject, inject, Output, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { CommonModule, DOCUMENT } from '@angular/common';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FileSizePipe } from '../pipes/file-size.pipe';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@Component({
  selector: 'app-document-panel',
  standalone: true,
  imports: [
    CommonModule, MatSidenavModule, MatButtonModule, MatIconModule,
    MatListModule, MatSnackBarModule, FileSizePipe, MatProgressBarModule
  ],
  templateUrl: './document-panel.component.html',
  styleUrl: './document-panel.component.css'
})
export class DocumentPanelComponent {
  documents: DocumentInfo[] = [];
  private destroyRef = inject(DestroyRef);

  // Add Output event emitter for document selection
  @Output() documentSelected = new EventEmitter<string>();

  // Add properties for upload progress
  uploadProgress = 0;
  isUploading = false;
  currentFileName = '';

  @ViewChild('sidenav') sidenav!: MatSidenav;

  constructor(
    private httpClient: HttpClient,
    @Inject(DOCUMENT) private htmlDocument: Document,
    private snackBar: MatSnackBar
  ) {
    this.fetchDocuments();
  }

  toggleSidenav() {
    this.sidenav.toggle();
  }

  onFileSelected(event: Event) {
    const fileInput = event.target as HTMLInputElement;
    if (fileInput.files && fileInput.files.length > 0) {
      const file = fileInput.files[0];
      this.uploadFile(file);
      fileInput.value = ''; // Reset the input
    }
  }

  uploadFile(file: File) {
    const formData = new FormData();
    formData.append('file', file);

    // Reset and initialize progress tracking
    this.uploadProgress = 0;
    this.isUploading = true;
    this.currentFileName = file.name;

    let host: string;
    let protocol: string;

    if (this.htmlDocument.location.hostname == 'localhost') {
      host = 'localhost:8080';
    } else {
      host = this.htmlDocument.location.host;
    }
    protocol = this.htmlDocument.location.protocol;

    this.httpClient.post<DocumentInfo>(`${protocol}//${host}/upload`, formData, {
      reportProgress: true,
      observe: 'events'
    }).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          // Calculate and update progress percentage
          if (event.total) {
            this.uploadProgress = Math.round(100 * event.loaded / event.total);
          }
        } else if (event.type === HttpEventType.Response) {
          // Upload complete
          this.isUploading = false;
          this.snackBar.open('File uploaded successfully', 'Close', {
            duration: 3000
          });
          this.fetchDocuments();
        }
      },
      error: (error) => {
        // Reset progress state on error
        this.isUploading = false;
        console.error('Error uploading file:', error);
        this.snackBar.open('Error uploading file', 'Close', {
          duration: 3000
        });
      }
    });
  }

  fetchDocuments() {
    let host: string;
    let protocol: string;

    if (this.htmlDocument.location.hostname == 'localhost') {
      host = 'localhost:8080';
    } else {
      host = this.htmlDocument.location.host;
    }
    protocol = this.htmlDocument.location.protocol;

    this.httpClient.get<DocumentInfo[]>(`${protocol}//${host}/documents`)
      .subscribe({
        next: (data) => {
          this.documents = data;
          // If documents were retrieved, emit the ID of the first document
          if (this.documents.length > 0) {
            this.documentSelected.emit(this.documents[0].id);
          }
        },
        error: (error) => {
          console.error('Error fetching documents:', error);
        }
      });
  }

  deleteAllDocuments() {
    let host: string;
    let protocol: string;

    if (this.htmlDocument.location.hostname == 'localhost') {
      host = 'localhost:8080';
    } else {
      host = this.htmlDocument.location.host;
    }
    protocol = this.htmlDocument.location.protocol;

    this.httpClient.delete(`${protocol}//${host}/documents`)
      .subscribe({
        next: () => {
          this.snackBar.open('All documents deleted', 'Close', {
            duration: 3000
          });
          this.fetchDocuments();
        },
        error: (error) => {
          console.error('Error deleting all documents:', error);
          this.snackBar.open('Error deleting all documents', 'Close', {
            duration: 3000
          });
        }
      });
  }
}

interface DocumentInfo {
  id: string;
  name: string;
  size: number;
  uploadDate: string;
}
