import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Token being sent:', token);
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
    });
  }

  getEmployeeProfile(): Observable<any> {
    return this.http.get(`${this.apiUrl}/profile`, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  updateEmployeeProfile(profile: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/profile`, profile, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  private handleError(error: any): Observable<never> {
    console.error('API Error:', error);
    let errorMessage = 'Something went wrong; please try again later.';
    if (error.status === 403) {
      errorMessage = 'Access denied. Insufficient permissions.';
    } else if (error.status === 401) {
      errorMessage = 'UNAUTHORIZED.';
    }
    return throwError(() => new Error(errorMessage));
  }
}
