import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { AuthService } from '../../core/services/auth.service';
import {ProfileService} from './profile.service';

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

  constructor(private authService: AuthService, private apiService: ProfileService) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.apiService.getEmployeeProfile().subscribe({
        next: (data) => {
          this.profile = data;
          console.log('Profile data:', data); // Debug log
        },
        error: (err) => {
          this.error = 'Failed to load profile: ' + err.message;
          console.error('Profile fetch error:', err); // Debug log
        }
      });
    } else {
      this.error = 'User not authenticated';
    }
  }

  get role(): string | null {
    return this.authService.getRole();
  }
}
