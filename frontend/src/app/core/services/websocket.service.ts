import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private stompClient: Client;
  private notificationsSubject = new BehaviorSubject<string[]>([]);
  notifications$ = this.notificationsSubject.asObservable();

  constructor(private authService: AuthService) {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: this.getConnectHeaders(),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => console.log('STOMP Debug:', str)
    });
    this.initializeWebSocket();
  }

  private getConnectHeaders(): { [key: string]: string } {
    const token = this.authService.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  private initializeWebSocket(): void {
    const role = this.authService.getRole();
    const cin = this.authService.getCin();
    const token = this.authService.getToken();

    if (!token) {
      console.error('No token available for WebSocket connection');
      return;
    }

    console.log('Connecting with token:', token); // Log the token (mask sensitive parts if logging publicly)
    console.log('Role:', role, 'CIN:', cin);

    this.stompClient.onConnect = () => {
      console.log('WebSocket connected successfully');
      if (role === 'HR_MANAGER') {
        this.stompClient.subscribe('/topic/hr-notifications', (message) => {
          console.log('HR Notification:', message.body);
          this.addNotification(message.body);
        });
      }
      if (cin) {
        this.stompClient.subscribe(`/topic/employee-notifications/${cin}`, (message) => {
          console.log('Employee Notification:', message.body);
          this.addNotification(message.body);
        });
      }
    };

    this.stompClient.onStompError = (frame) => console.error('STOMP Error:', frame);
    this.stompClient.onWebSocketError = (error) => console.error('WebSocket Error:', error);
    this.stompClient.activate();
  }

  private addNotification(message: string): void {
    const current = this.notificationsSubject.value;
    this.notificationsSubject.next([...current, message]);
  }

  clearNotifications(): void {
    this.notificationsSubject.next([]);
  }

  disconnect(): void {
    this.stompClient?.deactivate();
    console.log('WebSocket disconnected');
  }
}
