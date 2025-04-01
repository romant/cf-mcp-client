import { Component, DestroyRef, Inject, inject, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { CommonModule, DOCUMENT } from '@angular/common';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FileSizePipe } from '../pipes/file-size.pipe';

@Component({
  selector: 'app-document-panel',
  standalone: true,
  imports: [
    CommonModule, MatSidenavModule, MatButtonModule, MatIconModule,
    MatListModule, MatSnackBarModule, FileSizePipe
  ],
  templateUrl: './document-panel.component.html',
  styleUrl: './document-panel.component.css'
})
export class DocumentPanelComponent {
  documents: DocumentInfo[] = [];
  private destroyRef = inject(DestroyRef);

  @ViewChild('sidenav') sidenav!: MatSidenav;

  constructor(
    private httpClient: HttpClient,
    @Inject(DOCUMENT) private document: Document,
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

    let host: string;
    let protocol: string;

    if (this.document.location.hostname == 'localhost') {
      host = 'localhost:8080';
    } else {
      host = this.document.location.host;
    }
    protocol = this.document.location.protocol;

    this.httpClient.post<DocumentInfo>(`${protocol}//${host}/upload`, formData, {
      reportProgress: true,
      observe: 'events'
    }).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          // Handle upload progress if needed
        } else if (event.type === HttpEventType.Response) {
          this.snackBar.open('File uploaded successfully', 'Close', {
            duration: 3000
          });
          this.fetchDocuments();
        }
      },
      error: (error) => {
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

    if (this.document.location.hostname == 'localhost') {
      host = 'localhost:8080';
    } else {
      host = this.document.location.host;
    }
    protocol = this.document.location.protocol;

    this.httpClient.get<DocumentInfo[]>(`${protocol}//${host}/documents`)
      .subscribe({
        next: (data) => {
          this.documents = data;
        },
        error: (error) => {
          console.error('Error fetching documents:', error);
        }
      });
  }

  deleteDocument(id: string) {
    let host: string;
    let protocol: string;

    if (this.document.location.hostname == 'localhost') {
      host = 'localhost:8080';
    } else {
      host = this.document.location.host;
    }
    protocol = this.document.location.protocol;

    this.httpClient.delete(`${protocol}//${host}/documents/${id}`)
      .subscribe({
        next: () => {
          this.snackBar.open('Document deleted', 'Close', {
            duration: 3000
          });
          this.fetchDocuments();
        },
        error: (error) => {
          console.error('Error deleting document:', error);
          this.snackBar.open('Error deleting document', 'Close', {
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
