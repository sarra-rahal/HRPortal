import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  isSidebarOpen = false;

  constructor(private authService: AuthService) {}

  get role(): string | null {
    return this.authService.getRole();
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const sidebar = document.querySelector('.sidebar');
    const sidebarBtn = document.querySelector('.sidebar-btn');
    if (this.isSidebarOpen && sidebar && sidebarBtn && !sidebar.contains(event.target as Node) && !sidebarBtn.contains(event.target as Node)) {
      this.isSidebarOpen = false;
    }
  }
}
