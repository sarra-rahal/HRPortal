import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {Training, TrainingEnrollment} from '../../core/models/training.model';

@Injectable({
  providedIn: 'root'
})
export class ManageTrainingService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Token being sent for request:', token);
    if (!token) {
      console.error('No token found in localStorage - redirecting to sign-in');
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  // Training
  createTraining(training: Training): Observable<Training> {
    return this.http.post<Training>(`${this.apiUrl}/training/create`, training, { headers: this.getAuthHeaders() });
  }

  getAllTrainings(): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.apiUrl}/training/all`, { headers: this.getAuthHeaders() });
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
  getPendingEnrollments(): Observable<TrainingEnrollment[]> {
    return this.http.get<TrainingEnrollment[]>(`${this.apiUrl}/training/pending-enrollments`, {
      headers: this.getAuthHeaders()
    });
  }
}
