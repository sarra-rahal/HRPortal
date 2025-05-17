import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TokenService {
  private tokenSubject = new BehaviorSubject<string | null>(localStorage.getItem('token'));

  setToken(token: string | null) {
    if (token) {
      localStorage.setItem('token', token);
      this.tokenSubject.next(token);
    } else {
      localStorage.removeItem('token');
      this.tokenSubject.next(null);
    }
  }

  getToken(): Observable<string | null> {
    return this.tokenSubject.asObservable();
  }

  getCurrentToken(): string | null {
    return this.tokenSubject.value;
  }
}
