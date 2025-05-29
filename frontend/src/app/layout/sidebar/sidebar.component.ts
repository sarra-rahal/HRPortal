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
  isLeaveMenuOpen = false;
  isTrainingMenuOpen = false;
  isManageTrainingMenuOpen = false;

  constructor(private authService: AuthService) {}

  get role(): string | null {
    return this.authService.getRole();
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  toggleLeaveMenu(): void {
    this.isLeaveMenuOpen = !this.isLeaveMenuOpen;
  }

  toggleTrainingMenu(): void {
    this.isTrainingMenuOpen = !this.isTrainingMenuOpen;
  }

  toggleManageTrainingMenu(): void {
    this.isManageTrainingMenuOpen = !this.isManageTrainingMenuOpen;
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
