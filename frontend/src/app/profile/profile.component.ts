import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../services/api.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  role = localStorage.getItem('role');
  firstname = localStorage.getItem('firstname');
  lastname = localStorage.getItem('lastname');
  profile: any = null;
  error: string | null = null;
  isSidebarOpen = false;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    if (this.role === 'EMPLOYEE') {
      this.loadEmployeeProfile();
    } else if (this.role === 'HR_MANAGER') {
      this.loadHrProfile();
    }
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
