import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ManageLeavesService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Token being sent:',token)
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  // Managing Leave Requests
  getAllLeaveRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/leave/pending`, { headers: this.getAuthHeaders() });
  }

  reviewLeaveRequest(id: number, reviewData: { status: string; hrComment: string }): Observable<any> {
    return this.http.put(`${this.apiUrl}/leave/review/${id}`, reviewData, { headers: this.getAuthHeaders() });
  }
}
