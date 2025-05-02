import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

interface ForgotPasswordResponse {
  success: boolean;
  message: string;
  errorCode?: string;
}

interface ResetPasswordResponse {
  success: boolean;
  message: string;
  errorCode?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  setUserData(token: string, firstname: string, lastname: string, role: string, email: string): void {
    localStorage.setItem('token', token);
    localStorage.setItem('firstname', firstname);
    localStorage.setItem('lastname', lastname);
    localStorage.setItem('role', role);
    localStorage.setItem('email', email);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
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

  getEmail(): string | null {
    return localStorage.getItem('email');
  }

  clearUserData(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('firstname');
    localStorage.removeItem('lastname');
    localStorage.removeItem('role');
    localStorage.removeItem('email');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  forgotPassword(employeeNumber: string, email: string): Observable<ForgotPasswordResponse> {
    return this.http.post<ForgotPasswordResponse>(`${this.apiUrl}/forgot-password`, { employeeNumber, email });
  }

  resetPassword(token: string, newPassword: string): Observable<ResetPasswordResponse> {
    return this.http.post<ResetPasswordResponse>(`${this.apiUrl}/reset-password`, { newPassword }, { params: { token } });
  }
}
