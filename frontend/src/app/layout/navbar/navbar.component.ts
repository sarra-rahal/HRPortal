import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {RouterLink, Router, NavigationExtras} from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  constructor(private authService: AuthService, private router: Router) {}

  get firstname(): string | null {
    return this.authService.getFirstname();
  }

  get lastname(): string | null {
    return this.authService.getLastname();
  }

  logout(): void {
    this.authService.clearUserData(); // Clears token and user data
    const navigationExtras: NavigationExtras = {
      replaceUrl: true // Replaces current history entry
    };
    this.router.navigate(['/sign-in'], navigationExtras);
  }
}
