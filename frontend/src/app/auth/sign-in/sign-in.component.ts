import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service'; // Import AuthService

interface SignInResponse {
  success: boolean;
  message: string;
  token?: string;
  role?: string;
  cin?: string;
  firstname?: string;
  lastname?: string;
  errorCode?: string;
}

@Component({
  selector: 'app-sign-in',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './sign-in.component.html',
  styleUrls: ['./sign-in.component.scss']
})
export class SignInComponent implements OnInit {
  signInForm: FormGroup;
  loading = false;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router,
    private authService: AuthService // Inject AuthService
  ) {
    this.signInForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {}

  onSubmit(): void {
    if (this.signInForm.invalid) return;

    this.loading = true;
    this.errorMessage = null;

    const signInData = this.signInForm.value;
    this.http.post<SignInResponse>('http://localhost:8080/api/auth/signin', signInData).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.token) {
          this.authService.setUserData(
            response.token,
            response.cin || '',
            response.firstname || '',
            response.lastname || '',
            response.role || ''
          );
          this.redirectToDashboard(response.role);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'An error occurred. Please try again.';
      }
    });
  }

  private redirectToDashboard(role: string | undefined): void {
    if (role === 'HR_MANAGER' || role === 'EMPLOYEE') {
      this.router.navigate(['/welcome']);
    } else {
      this.router.navigate(['/sign-in']);
    }
  }
}
