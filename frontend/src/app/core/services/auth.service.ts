import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  setUserData(token: string, cin: string, firstname: string, lastname: string, role: string): void {
    localStorage.setItem('token', token);
    localStorage.setItem('cin', cin);
    localStorage.setItem('firstname', firstname);
    localStorage.setItem('lastname', lastname);
    localStorage.setItem('role', role);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getCin(): string | null {
    return localStorage.getItem('cin');
  }

  getFirstname(): string | null {
    return localStorage.getItem('firstname');
  }

  getLastname(): string | null {
    return localStorage.getItem('lastname');
  }

  getRole(): string | null {
    return localStorage.getItem('role');
  }

  clearUserData(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('cin');
    localStorage.removeItem('firstname');
    localStorage.removeItem('lastname');
    localStorage.removeItem('role');
  }

  isLoggedIn(): boolean {
    return !!this.getToken(); // Check token instead of role for JWT
  }
}
