import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private stompClient: Client;

  constructor() {
    this.stompClient = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });
  }

  connect(topic: string): Observable<any> {
    return new Observable(observer => {
      const headers = { Authorization: `Bearer ${localStorage.getItem('token')}` };
      this.stompClient.onConnect = () => {
        this.stompClient.subscribe(topic, (message) => {
          observer.next(JSON.parse(message.body));
        });
      };
      this.stompClient.onStompError = (frame) => observer.error(frame);
      this.stompClient.activate();
      return () => this.stompClient.deactivate();
    });
  }
}
