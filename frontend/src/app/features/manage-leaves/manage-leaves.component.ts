import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

interface LeaveRequest {
  id: number;
  employeeCin: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  reason: string;
  status: string;
  hrComment?: string;
}

@Component({
  selector: 'app-manage-leaves',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './manage-leaves.component.html',
  styleUrls: ['./manage-leaves.component.scss']
})
export class ManageLeavesComponent implements OnInit {
  requests: LeaveRequest[] = [];
  error: string | null = null;

  constructor(private http: HttpClient, private authService: AuthService) {}

  ngOnInit(): void {
    this.loadPendingRequests();
  }

  loadPendingRequests(): void {
    this.http.get<LeaveRequest[]>('http://localhost:8080/api/leave/pending', {
      headers: { Authorization: `Bearer ${this.authService.getToken()}` }
    }).subscribe({
      next: (data) => this.requests = data,
      error: (err) => this.error = err.message
    });
  }

  reviewRequest(id: number, status: string, comment: string): void {
    this.http.put(`http://localhost:8080/api/leave/review/${id}`, null, {
      headers: { Authorization: `Bearer ${this.authService.getToken()}` },
      params: { status, hrComment: comment || '' }
    }).subscribe({
      next: () => this.loadPendingRequests(),
      error: (err) => this.error = err.message
    });
  }
}
