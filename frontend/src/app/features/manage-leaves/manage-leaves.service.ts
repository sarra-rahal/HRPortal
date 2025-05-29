import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

interface LeaveRequest {
  id: number;
  employeeCin: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  duration: number;
  reason: string;
  status: string;
  employeeName?: string;
  certificate?: string;
}

interface ReviewRequest {
  status: string;
  hrComment: string;
}

@Injectable({
  providedIn: 'root'
})
export class ManageLeavesService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  getAllLeaveRequests(): Observable<LeaveRequest[]> {
    return this.http.get<LeaveRequest[]>(`${this.apiUrl}/leave/all`, { headers: this.getAuthHeaders() });
  }

  reviewLeaveRequest(id: number, review: ReviewRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/leave/review/${id}`, review, { headers: this.getAuthHeaders() });
  }

  sendNote(id: number, note: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/leave/send-note/${id}`, note, { headers: this.getAuthHeaders() });
  }
}
