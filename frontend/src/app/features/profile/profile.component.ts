import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  profile: any = null;
  error: string | null = null;

  constructor(private apiService: ApiService, private authService: AuthService) {}

  ngOnInit(): void {
    if (this.role === 'EMPLOYEE') {
      this.loadEmployeeProfile();
    } else if (this.role === 'HR_MANAGER') {
      this.loadHrProfile();
    }
  }

  get role(): string | null {
    return this.authService.getRole();
  }

  get firstname(): string | null {
    return this.authService.getFirstname();
  }

  get lastname(): string | null {
    return this.authService.getLastname();
  }

  loadEmployeeProfile(): void {
    this.apiService.getEmployeeProfile().subscribe({
      next: (data) => this.profile = data,
      error: (err) => this.error = err.message
    });
  }

  loadHrProfile(): void {
    this.profile = {
      cin: '10000001',
      departement: 'HR',
      jobtitle: 'HR Manager',
      firstname: this.firstname,
      lastname: this.lastname
    };
  }
}
