import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { Observable } from 'rxjs';
import { TokenService } from './token.service';

@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private stompClient: Client | null = null;

  constructor(private tokenService: TokenService) {}

  private initializeStompClient(token: string): Client {
    return new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        console.log('STOMP Debug:', str);
        console.log('Connect Headers:', this.stompClient?.connectHeaders);
      },
      webSocketFactory: () => new WebSocket('ws://localhost:8080/ws') // Explicitly use native WebSocket
    });
  }

  connect(topic: string): Observable<any> {
    return new Observable(observer => {
      const token = this.tokenService.getCurrentToken();
      console.log('Token in connect:', token);
      if (!token) {
        console.error('No token available for WebSocket connection');
        observer.error('No token available for WebSocket connection');
        return;
      }

      this.stompClient = this.initializeStompClient(token);
      this.stompClient.onConnect = () => {
        console.log('WebSocket connected');
        this.stompClient?.subscribe(topic, (message) => {
          observer.next(JSON.parse(message.body));
        });
      };
      this.stompClient.onStompError = (frame) => {
        console.error('WebSocket error:', frame);
        observer.error(frame);
      };
      this.stompClient.onWebSocketError = (error) => {
        console.error('WebSocket connection error:', error);
      };
      this.stompClient.onWebSocketClose = (event) => {
        console.error('WebSocket closed:', event);
      };
      this.stompClient.activate();

      return () => {
        this.stompClient?.deactivate();
        this.stompClient = null;
      };
    });
  }
}
