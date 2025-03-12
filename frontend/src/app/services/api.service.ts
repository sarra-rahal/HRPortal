import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  // HR Manager endpoint: Fetch all employees
  getEmployees(): Observable<any> {
    return this.http.get(`${this.apiUrl}/hr/employees`, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  // Employee endpoint: Fetch own profile
  getEmployeeProfile(): Observable<any> {
    return this.http.get(`${this.apiUrl}/employee/profile`, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  private handleError(error: any): Observable<never> {
    console.error('API Error:', error);
    if (error.status === 403) {
      // Redirect to sign-in on invalid/expired token
      window.location.href = '/sign-in';
    }
    return throwError(() => new Error('Something went wrong; please try again later.'));
  }
}
