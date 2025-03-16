import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  getFirstname(): string | null {
    return localStorage.getItem('firstname');
  }

  getLastname(): string | null {
    return localStorage.getItem('lastname');
  }

  getRole(): string | null {
    return localStorage.getItem('role');
  }

  setUserData(firstname: string, lastname: string, role: string): void {
    localStorage.setItem('firstname', firstname);
    localStorage.setItem('lastname', lastname);
    localStorage.setItem('role', role);
  }

  clearUserData(): void {
    localStorage.removeItem('firstname');
    localStorage.removeItem('lastname');
    localStorage.removeItem('role');
  }

  isLoggedIn(): boolean {
    return !!this.getRole(); // True if role exists, false if cleared
  }
}
