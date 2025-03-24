import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private stompClient: Client;

  constructor() {
    this.stompClient = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('token')}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => console.log(str)
    });
  }

  connect(topic: string): Observable<any> {
    return new Observable(observer => {
      if (!localStorage.getItem('token')) {
        observer.error('No token available for WebSocket connection');
        return;
      }
      this.stompClient.onConnect = () => {
        console.log('WebSocket connected');
        this.stompClient.subscribe(topic, (message) => {
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
      this.stompClient.activate();
      return () => this.stompClient.deactivate();
    });
  }
}
