import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Training, TrainingEnrollment } from '../../core/models/training.model';

@Injectable({
  providedIn: 'root'
})
export class TrainingService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Token being sent for request:', token);
    if (!token) {
      console.error('No token found in localStorage');
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  getAllTrainings(): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.apiUrl}/training/all`, { headers: this.getAuthHeaders() });
  }

  enroll(trainingId: number): Observable<TrainingEnrollment> {
    return this.http.post<TrainingEnrollment>(`${this.apiUrl}/training/enroll/${trainingId}`, null, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  getMyEnrollments(): Observable<TrainingEnrollment[]> {
    return this.http.get<TrainingEnrollment[]>(`${this.apiUrl}/training/my-enrollments`, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  cancelEnrollment(enrollmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/training/cancel/${enrollmentId}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(this.handleError)
    );
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
