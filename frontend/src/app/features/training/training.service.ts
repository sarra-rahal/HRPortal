import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Training, TrainingEnrollment, TrainingProposal } from '../../core/models/training.model';

@Injectable({
  providedIn: 'root'
})
export class TrainingService {
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

  getAllTrainings(): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.apiUrl}/training/all`, {
      headers: this.getAuthHeaders()
    });
  }

  getMyEnrollments(): Observable<TrainingEnrollment[]> {
    return this.http.get<TrainingEnrollment[]>(`${this.apiUrl}/training/my-enrollments`, {
      headers: this.getAuthHeaders()
    });
  }

  enroll(trainingId: number): Observable<TrainingEnrollment> {
    return this.http.post<TrainingEnrollment>(`${this.apiUrl}/training/enroll/${trainingId}`, null, {
      headers: this.getAuthHeaders()
    });
  }

  cancelEnrollment(enrollmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/training/enrollment/cancel/${enrollmentId}`, {
      headers: this.getAuthHeaders()
    });
  }

  proposeTraining(proposal: TrainingProposal): Observable<TrainingProposal> {
    return this.http.post<TrainingProposal>(`${this.apiUrl}/training/propose`, proposal, {
      headers: this.getAuthHeaders()
    });
  }

  updateTrainingProposal(proposalId: number, proposal: TrainingProposal): Observable<TrainingProposal> {
    return this.http.put<TrainingProposal>(`${this.apiUrl}/training/proposal/update/${proposalId}`, proposal, {
      headers: this.getAuthHeaders()
    });
  }

  cancelTrainingProposal(proposalId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/training/proposal/cancel/${proposalId}`, {
      headers: this.getAuthHeaders()
    });
  }

  getMyProposals(): Observable<TrainingProposal[]> {
    return this.http.get<TrainingProposal[]>(`${this.apiUrl}/training/my-proposals`, {
      headers: this.getAuthHeaders()
    });
  }
}
