import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import {ManageLeavesService} from './manage-leaves.service';

interface LeaveRequest {
  id: number;
  employeeCin: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  duration: number;
  reason: string;
  status: string;
}

@Component({
  selector: 'app-manage-leaves',
  standalone: true,
  imports: [CommonModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './manage-leaves.component.html',
  styleUrls: ['./manage-leaves.component.scss']
})
export class ManageLeavesComponent implements OnInit {
  requests: LeaveRequest[] = [];
  error: string | null = null;
  loading: boolean = false;

  constructor(private apiService: ManageLeavesService) {}

  ngOnInit(): void {
    this.loadPendingRequests();
  }

  loadPendingRequests(): void {
    this.loading = true;
    this.error = null;
    this.apiService.getAllLeaveRequests().subscribe({
      next: (data) => {
        this.requests = data.filter(req => req.status === 'PENDING');
        this.loading = false;
      },
      error: (err) => {
        this.error = `Error fetching leave requests: ${err.message}`;
        this.loading = false;
      }
    });
  }

  confirmReview(id: number, status: string, comment: string): void {
    const action = status.toLowerCase();
    if (confirm(`Are you sure you want to ${action} this request?`)) {
      this.reviewRequest(id, status, comment);
    }
  }

  reviewRequest(id: number, status: string, comment: string): void {
    this.apiService.reviewLeaveRequest(id, { status, hrComment: comment }).subscribe({
      next: () => this.loadPendingRequests(),
      error: (err) => this.error = `Error reviewing request: ${err.message}`
    });
  }
}
