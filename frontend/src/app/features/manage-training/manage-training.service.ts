import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Training, TrainingEnrollment, TrainingProposal } from '../../core/models/training.model';

@Injectable({
  providedIn: 'root'
})
export class ManageTrainingService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    if (!token) {
      console.error('No token found in localStorage - redirecting to sign-in');
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  createTraining(training: Training): Observable<Training> {
    return this.http.post<Training>(`${this.apiUrl}/training/create`, training, { headers: this.getAuthHeaders() });
  }

  updateTraining(training: Training): Observable<Training> {
    return this.http.put<Training>(`${this.apiUrl}/training/update/${training.id}`, training, { headers: this.getAuthHeaders() });
  }

  deleteTraining(trainingId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/training/delete/${trainingId}`, { headers: this.getAuthHeaders() });
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

  getPendingEnrollments(): Observable<TrainingEnrollment[]> {
    return this.http.get<TrainingEnrollment[]>(`${this.apiUrl}/training/pending-enrollments`, {
      headers: this.getAuthHeaders()
    });
  }

  reviewTrainingProposal(proposalId: number, status: string): Observable<TrainingProposal> {
    return this.http.put<TrainingProposal>(`${this.apiUrl}/training/proposal/review/${proposalId}`, null, {
      headers: this.getAuthHeaders(),
      params: { status }
    });
  }

  getPendingProposals(): Observable<TrainingProposal[]> {
    return this.http.get<TrainingProposal[]>(`${this.apiUrl}/training/pending-proposals`, {
      headers: this.getAuthHeaders()
    });
  }
}
