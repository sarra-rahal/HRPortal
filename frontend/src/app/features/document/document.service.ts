import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Document, DocumentRequest } from '../../core/models/document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private apiUrl = 'http://localhost:8080/api/documents';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Token being sent for request:', token);
    if (!token) {
      console.error('No token found in localStorage');
      throw new Error('No authentication token');
    }
    return new HttpHeaders({ 'Authorization': `Bearer ${token.trim()}` });
  }

  getAllDocuments(): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/all`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(docs => console.log('Fetched documents:', docs)),
        catchError(this.handleError)
      );
  }

  downloadDocument(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${id}`, {
      headers: this.getAuthHeaders(),
      responseType: 'blob'
    }).pipe(
      tap(() => console.log(`Downloading document ID: ${id}`)),
      catchError(this.handleError)
    );
  }

  uploadDocument(file: File, access: { [key: string]: string[] }): Observable<Document> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('access', JSON.stringify(access));
    return this.http.post<Document>(`${this.apiUrl}/upload`, formData, { headers: this.getAuthHeaders() })
      .pipe(
        tap(doc => console.log('Uploaded document:', doc)),
        catchError(this.handleError)
      );
  }

  requestDocument(request: DocumentRequest): Observable<DocumentRequest> {
    return this.http.post<DocumentRequest>(`${this.apiUrl}/request`, request, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log('Document request submitted')),
        catchError(this.handleError)
      );
  }

  getMyRequests(): Observable<DocumentRequest[]> {
    return this.http.get<DocumentRequest[]>(`${this.apiUrl}/my-requests`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(requests => console.log('Fetched requests:', requests)),
        catchError(this.handleError)
      );
  }

  cancelRequest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/cancel-request/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log(`Request ${id} cancelled`)),
        catchError(this.handleError)
      );
  }

  private handleError(error: any): Observable<never> {
    let errorMessage = 'An unexpected error occurred';
    if (error.error instanceof ErrorEvent) {
      errorMessage = error.error.message;
    } else {
      errorMessage = error.error?.message || error.message || `Error ${error.status}`;
      if (error.status === 401) {
        errorMessage = 'Authentication failed, please sign in again';
      } else if (error.status === 403) {
        errorMessage = 'You do not have permission to access this resource';
      }
    }
    console.error('Document API Error:', error);
    return throwError(() => new Error(errorMessage));
  }
}
