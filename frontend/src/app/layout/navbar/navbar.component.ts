import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit, OnDestroy {
  notifications: string[] = [];
  showNotifications = false;
  private notificationSub?: Subscription;

  constructor(
    private authService: AuthService,
    private router: Router,
    private websocketService: WebsocketService
  ) {}

  ngOnInit(): void {
    this.notificationSub = this.websocketService.notifications$.subscribe(
      (notifications) => this.notifications = notifications
    );
  }

  ngOnDestroy(): void {
    this.notificationSub?.unsubscribe();
    this.websocketService.disconnect();
  }

  get firstname(): string | null { return this.authService.getFirstname(); }
  get lastname(): string | null { return this.authService.getLastname(); }
  get isHR(): boolean { return this.authService.getRole() === 'HR_MANAGER'; }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
  }

  clearNotifications(): void {
    this.websocketService.clearNotifications();
    this.showNotifications = false;
  }

  logout(): void {
    this.authService.clearUserData();
    this.router.navigate(['/sign-in'], { replaceUrl: true });
  }
}
