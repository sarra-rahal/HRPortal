import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { ManageLeavesService } from './manage-leaves.service';
import { ToastService } from '../../core/services/toast.service';

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

  constructor(private apiService: ManageLeavesService, private toastService: ToastService) {}

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
        this.toastService.show(`Error fetching leave requests: ${err.message}`, 'error');
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
      next: () => {
        this.loadPendingRequests();
        this.toastService.show(`Leave request ${status.toLowerCase()} successfully`, 'success');
      },
      error: (err) => {
        this.error = `Error reviewing request: ${err.message}`;
        this.toastService.show(`Error reviewing request: ${err.message}`, 'error');
      }
    });
  }

  sendnote(id: number,  comment: string): void {
    this.apiService.sendnote(id, comment).subscribe({
      next: () => {
        this.loadPendingRequests();
        this.toastService.show(`Note send successfully`, 'success');
      },
      error: (err) => {
        this.error = `Error sending note: ${err.message}`;
        this.toastService.show(`Error sending note: ${err.message}`, 'error');
      }
    });
  }
}
