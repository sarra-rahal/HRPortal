import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { Notification } from '../../core/models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NavbarService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Token being sent:',token)
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
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
}
