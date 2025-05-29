import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LeaveService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  getMyLeaveRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/leave/my-requests`, { headers: this.getAuthHeaders() });
  }

  submitSickLeaveRequest(formData: FormData): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/leave/submit/sick`, formData, { headers: this.getAuthHeaders() });
  }

  submitAnnualLeaveRequest(data: { startDate: string; endDate: string; reason?: string }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/leave/submit/annual`, data, { headers: this.getAuthHeaders() });
  }

  cancelLeaveRequest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/leave/cancel/${id}`, { headers: this.getAuthHeaders() });
  }

  updateSickLeaveRequest(id: number, formData: FormData): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/leave/update/sick/${id}`, formData, { headers: this.getAuthHeaders() });
  }

  updateAnnualLeaveRequest(id: number, data: { startDate: string; endDate: string; reason?: string }): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/leave/update/annual/${id}`, data, { headers: this.getAuthHeaders() });
  }

  getLeaveBalance(): Observable<{ annual: number; sick: number }> {
    return this.http

      .get<{ annual: number; sick: number }>(`${this.apiUrl}/leave/balance`, { headers: this.getAuthHeaders() });
  }
}
