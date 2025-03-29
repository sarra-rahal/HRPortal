import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { AuthService } from '../../core/services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-welcome',
  standalone: true,
  imports: [CommonModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './welcome.component.html',
  styleUrls: ['./welcome.component.scss']
})
export class WelcomeComponent {
  constructor(private authService: AuthService, private router: Router) {}

  get role(): string | null {
    return this.authService.getRole();
  }

  get firstname(): string | null {
    return this.authService.getFirstname();
  }

  get lastname(): string | null {
    return this.authService.getLastname();
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }
}
