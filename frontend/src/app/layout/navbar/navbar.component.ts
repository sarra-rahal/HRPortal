import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { Subscription } from 'rxjs';
import { Notification } from '../../core/models/notification.model';
import {NavbarService} from './navbar.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  showNotifications = false;
  private wsSub?: Subscription;
  private email = localStorage.getItem('email') || '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private websocketService: WebsocketService,
    private apiService: NavbarService
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
    this.setupWebSocket();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  loadNotifications(): void {
    this.apiService.getMyNotifications(this.email).subscribe({
      next: (notifications) => this.notifications = notifications,
      error: (err) => console.error('Error fetching notifications:', err)
    });
  }

  setupWebSocket(): void {
    this.wsSub = this.websocketService.connect('/topic/notifications/${this.email}').subscribe({
      next: () => this.loadNotifications(),
      error: (err) => console.error('WebSocket error:', err)
    });
  }

  get firstname(): string | null { return this.authService.getFirstname(); }
  get lastname(): string | null { return this.authService.getLastname(); }
  get isHR(): boolean { return this.authService.getRole() === 'HR_MANAGER'; }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
  }

  deleteNotification(id: number): void {
    this.apiService.deleteNotification(id).subscribe({
      next: () => this.loadNotifications(),
      error: (err) => console.error('Error deleting notification:', err)
    });
  }

  logout(): void {
    this.authService.clearUserData();
    this.router.navigate(['/sign-in'], { replaceUrl: true });
  }
}
