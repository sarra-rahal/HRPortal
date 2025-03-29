import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LeaveService {
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

}
