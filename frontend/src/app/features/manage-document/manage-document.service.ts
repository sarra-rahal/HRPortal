import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Document } from '../../core/models/document.model';

@Injectable({
  providedIn: 'root'
})
export class ManageDocumentService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Token being sent for request:', token);
    if (!token) {
      console.error('No token found in localStorage');
      throw new Error('No authentication token');
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token.trim()}`
    });
  }

  uploadDocument(file: File, accessibleBy: any): Observable<Document> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('accessibleBy', JSON.stringify(accessibleBy));
    console.log('Upload FormData:', { file: file.name, accessibleBy });

    const headers = this.getAuthHeaders();
    console.log('Upload Headers:', headers.get('Authorization')); // Debug header

    return this.http.post<Document>(`${this.apiUrl}/documents/upload`, formData, { headers })
      .pipe(
        tap(() => console.log('Document uploaded successfully')),
        catchError(this.handleError)
      );
  }

  generateDocument(name: string, content: string, accessibleBy: any, templateType: string): Observable<Document> {
    const params = { name, content, accessibleBy: JSON.stringify(accessibleBy), templateType };
    return this.http.post<Document>(`${this.apiUrl}/documents/generate`, null, { headers: this.getAuthHeaders(), params })
      .pipe(
        tap(() => console.log('Document generated successfully')),
        catchError(this.handleError)
      );
  }

  getDepartments(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/departments`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(depts => console.log('Fetched departments:', depts)),
        catchError(this.handleError)
      );
  }

  getEmployees(department: string): Observable<{ cin: string, name: string }[]> {
    const url = department ? `${this.apiUrl}/employees?department=${department}` : `${this.apiUrl}/employees`;
    return this.http.get<{ cin: string, name: string }[]>(url, { headers: this.getAuthHeaders() })
      .pipe(
        tap(emps => console.log('Fetched employees:', emps)),
        catchError(this.handleError)
      );
  }

  private handleError(error: any): Observable<never> {
    let errorMessage = 'An unexpected error occurred';
    if (error.error instanceof ErrorEvent) {
      errorMessage = error.error.message;
    } else {
      errorMessage = error.error?.message || error.message || `Error ${error.status}`;
      if (error.status === 400) {
        errorMessage = error.error.message || 'Invalid request';
      } else if (error.status === 403) {
        errorMessage = 'You do not have permission to perform this action';
      } else if (error.status === 401) {
        errorMessage = 'Authentication failed, please sign in again';
      }
    }
    console.error('ManageDocument API Error:', error);
    return throwError(() => new Error(errorMessage));
  }
}
