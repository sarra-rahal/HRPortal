import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';

interface SignUpResponse {
  success: boolean;
  message: string;
  errorCode?: string;
}

@Component({
  selector: 'app-sign-up',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './sign-up.component.html',
  styleUrls: ['./sign-up.component.scss']
})
export class SignUpComponent implements OnInit {
  signUpForm!: FormGroup;
  verifyForm!: FormGroup;
  loading = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;
  showVerifyForm = false;
  signupData: any = null;
  maskedEmail: string | null = null;
  showPassword = false;
  showConfirmPassword = false;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit() {
    this.signUpForm = this.fb.group({
      cin: ['', [Validators.required, Validators.pattern('^[0-9]{8}$')]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.pattern('^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$')]],
      confirmPassword: ['', Validators.required]
    }, { validator: this.passwordMatchValidator });

    this.verifyForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
    });
  }

  passwordMatchValidator(form: FormGroup) {
    return form.get('password')?.value === form.get('confirmPassword')?.value
      ? null : { mismatch: true };
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit() {
    if (this.signUpForm.invalid) return;

    this.loading = true;
    this.successMessage = null;
    this.errorMessage = null;

    this.signupData = this.signUpForm.value;
    this.http.post<SignUpResponse>('http://localhost:8080/api/auth/signup', this.signupData).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          this.successMessage = response.message;
          this.maskedEmail = response.message.split("email.")[1]?.replace(".", "") || '';
          this.showVerifyForm = true;
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'An error occurred. Please try again.';
      }
    });
  }

  onVerifySubmit() {
    if (this.verifyForm.invalid || !this.signupData) return;

    this.loading = true;
    this.successMessage = null;
    this.errorMessage = null;

    const verifyData = {
      email: this.signupData.email,
      code: this.verifyForm.value.code
    };

    this.http.post<SignUpResponse>('http://localhost:8080/api/auth/verify', this.signupData, {
      params: verifyData
    }).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          this.successMessage = response.message;
          setTimeout(() => this.router.navigate(['/sign-in']), 2000);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Invalid or expired code.';
      }
    });
  }

  goToSignIn() {
    this.router.navigate(['/sign-in']);
  }
}
