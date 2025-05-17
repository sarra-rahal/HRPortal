import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Document, DocumentRequest } from '../../core/models/document.model';


interface StringListDictionary {
  [key: string]: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ManageDocumentService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Sending token:', token);
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  // uploadDocument(file: File, accessibleBy: any): Observable<Document> {
  //   const formData = new FormData();
  //   formData.append('file', file);
  //   formData.append('accessibleBy', JSON.stringify(accessibleBy));
  //   return this.http.post<Document>(`${this.apiUrl}/documents/upload`, formData, { headers: this.getAuthHeaders() })
  //     .pipe(
  //       tap(() => console.log('Document uploaded successfully')),
  //       catchError(this.handleError)
  //     );
  // }
  upload(file: File, access: StringListDictionary): Observable<Document> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('access', JSON.stringify(access));
    return this.http.post<Document>(`${this.apiUrl}/documents/upload`, formData, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log('Document uploaded successfully')),
        catchError(this.handleError)
      );
  }

  // generate(name: string, parameters: any, access: any, templateType: string): Observable<Document> {
  //   const formData = new FormData();
  //   formData.append('name', name);
  //   formData.append('parameters', JSON.stringify(parameters));
  //   formData.append('accessibleBy', JSON.stringify(access));
  //   formData.append('templateType', templateType);
  //   return this.http.post<Document>(`${this.apiUrl}/documents/generate`, formData, { headers: this.getAuthHeaders() })
  //     .pipe(
  //       tap(() => console.log('Document generated successfully')),
  //       catchError(this.handleError)
  //     );
  // }

  generateDocument(name: string, parameters: any, accessibleBy: any, templateType: string): Observable<Document> {
    const formData = new FormData();
    formData.append('name', name);
    formData.append('parameters', JSON.stringify(parameters));
    formData.append('accessibleBy', JSON.stringify(accessibleBy));
    formData.append('templateType', templateType);
    return this.http.post<Document>(`${this.apiUrl}/documents/generate`, formData, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log('Document generated successfully')),
        catchError(this.handleError)
      );
  }

  getPendingRequests(): Observable<DocumentRequest[]> {
    return this.http.get<DocumentRequest[]>(`${this.apiUrl}/documents/pending-requests`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(requests => console.log('Fetched pending requests:', requests)),
        catchError(this.handleError)
      );
  }

  reviewRequest(id: number, status: string): Observable<DocumentRequest> {
    const formData = new FormData();
    formData.append('status', status);
    return this.http.post<DocumentRequest>(`${this.apiUrl}/documents/review-request/${id}`, formData, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log(`Request ${id} reviewed as ${status}`)),
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
      }
    }
    console.error('ManageDocument API Error:', error);
    return throwError(() => new Error(errorMessage));
  }
}
