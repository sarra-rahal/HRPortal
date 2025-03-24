import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {Training, TrainingEnrollment} from '../models/training.model';
import { Notification } from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Token being sent:',token)
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  // Leave Requests
  getMyLeaveRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/leave/my-requests`, { headers: this.getAuthHeaders() });
  }

  submitLeaveRequest(request: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/leave/submit`, request, { headers: this.getAuthHeaders() });
  }

  cancelLeaveRequest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/leave/cancel/${id}`, { headers: this.getAuthHeaders() });
  }

  getAllLeaveRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/leave/pending`, { headers: this.getAuthHeaders() });
  }

  reviewLeaveRequest(id: number, reviewData: { status: string; hrComment: string }): Observable<any> {
    return this.http.put(`${this.apiUrl}/leave/review/${id}`, reviewData, { headers: this.getAuthHeaders() });
  }

  // Training
  getAllTrainings(): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.apiUrl}/training/all`, { headers: this.getAuthHeaders() });
  }

  createTraining(training: Training): Observable<Training> {
    return this.http.post<Training>(`${this.apiUrl}/training/create`, training, { headers: this.getAuthHeaders() });
  }

  enroll(trainingId: number): Observable<TrainingEnrollment> {
    return this.http.post<TrainingEnrollment>(`${this.apiUrl}/training/enroll/${trainingId}`, null, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  reviewEnrollment(enrollmentId: number, status: string): Observable<TrainingEnrollment> {
    return this.http.put<TrainingEnrollment>(`${this.apiUrl}/training/review/${enrollmentId}`, null, {
      headers: this.getAuthHeaders(),
      params: { status }
    });
  }

  getMyEnrollments(employeeEmail: string): Observable<TrainingEnrollment[]> {
    return this.http.get<TrainingEnrollment[]>(`${this.apiUrl}/training/my-enrollments`, {
      headers: this.getAuthHeaders(),
      params: { employeeEmail }
    });
  }

  // Notifications
  getMyNotifications(email: string): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiUrl}/notifications/my-notifications`, {
      headers: this.getAuthHeaders(),
      params: { email }
    });
  }

  deleteNotification(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/notifications/${id}`, { headers: this.getAuthHeaders() });
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
