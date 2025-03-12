import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-welcome',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './welcome.component.html',
  styleUrls: ['./welcome.component.scss']
})
export class WelcomeComponent {
  role = localStorage.getItem('role');
  firstname = localStorage.getItem('firstname');
  lastname = localStorage.getItem('lastname');
  isSidebarOpen = false;

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
