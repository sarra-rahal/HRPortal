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
    console.log('Token being sent:')
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
    return this.http.get(`${this.apiUrl}/profile`, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  submitLeaveRequest(request: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/leave/submit`, request, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  cancelLeaveRequest(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/leave/cancel/${id}`, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  getMyLeaveRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/leave/my-requests`, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  private handleError(error: any): Observable<never> {
    console.error('API Error:', error);
    let errorMessage = 'Something went wrong; please try again later.';
    if (error.status === 403) {
      errorMessage = 'Access denied. Insufficient permissions.';
    } else if (error.status === 401) {
      errorMessage = 'Session expired. Please sign in again.';
      window.location.href = '/sign-in';
    }
    return throwError(() => new Error(errorMessage));
  }
}
