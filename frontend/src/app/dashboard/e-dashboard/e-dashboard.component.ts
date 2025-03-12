import {Component, OnInit} from '@angular/core';
import {ApiService} from '../../services/api.service';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-e-dashboard',
  imports: [CommonModule],
  templateUrl: './e-dashboard.component.html',
  styleUrl: './e-dashboard.component.scss'
})
export class EDashboardComponent implements OnInit{
  profile: any = null;
  error: string | null = null;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.apiService.getEmployeeProfile().subscribe({
      next: (data) => {
        this.profile = data;
      },
      error: (err) => {
        this.error = err.message;
      }
    });
  }
}
