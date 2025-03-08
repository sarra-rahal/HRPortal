import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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
  loading = false;
  message: string | null = null;
  isSuccess: boolean = false;

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
  }

  passwordMatchValidator(form: FormGroup) {
    return form.get('password')?.value === form.get('confirmPassword')?.value
      ? null : { mismatch: true };
  }

  onSubmit() {
    if (this.signUpForm.invalid) return;

    this.loading = true;
    this.http.post<SignUpResponse>('http://localhost:8080/api/auth/signup', this.signUpForm.value).subscribe({
      next: (response) => {
        this.loading = false;
        this.isSuccess = response.success;
        this.message = response.message;
        if (response.success) {
          setTimeout(() => this.router.navigate(['/sign-in']), 3000);
        }
      },
      error: (error) => {
        this.loading = false;
        this.isSuccess = false;
        this.message = error.error?.message || 'An error occurred. Please try again.';
        console.error('Signup error:', error); // Debug log
      }
    });
  }
}
