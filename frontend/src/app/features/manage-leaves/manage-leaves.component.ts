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
  employeeName?: string;
  certificate?: string;
}

@Component({
  selector: 'app-manage-leaves',
  standalone: true,
  imports: [CommonModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './manage-leaves.component.html',
  styleUrls: ['./manage-leaves.component.scss']
})
export class ManageLeavesComponent implements OnInit {
  sickRequests: LeaveRequest[] = [];
  annualRequests: LeaveRequest[] = [];
  error: string | null = null;
  loading: boolean = false;
  processingRequests: Set<number> = new Set();
  currentView: 'sick' | 'annual' = 'sick';

  constructor(private apiService: ManageLeavesService, private toastService: ToastService) {}

  ngOnInit(): void {
    this.loadPendingRequests();
  }

  loadPendingRequests(): void {
    this.loading = true;
    this.error = null;
    this.apiService.getAllLeaveRequests().subscribe({
      next: (data) => {
        this.sickRequests = data
          .filter(req => req.status === 'PENDING' && req.leaveType === 'SICK')
          .map(req => ({
            ...req,
            employeeName: req.employeeName || req.employeeCin
          }));
        this.annualRequests = data
          .filter(req => req.status === 'PENDING' && req.leaveType === 'ANNUAL')
          .map(req => ({
            ...req,
            employeeName: req.employeeName || req.employeeCin
          }));
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || 'Error fetching leave requests';
        this.loading = false;
        if (this.error) {
          this.toastService.show(this.error, 'error');
        }
      }
    });
  }

  confirmReview(id: number, status: string, comment: string, inputElement: HTMLInputElement): void {
    const action = status.toLowerCase();
    if (confirm(`Are you sure you want to ${action} this leave request?`)) {
      this.reviewRequest(id, status, comment, inputElement);
    }
  }

  reviewRequest(id: number, status: string, comment: string, inputElement: HTMLInputElement): void {
    this.processingRequests.add(id);
    this.apiService.reviewLeaveRequest(id, { status, hrComment: comment }).subscribe({
      next: () => {
        this.processingRequests.delete(id);
        this.loadPendingRequests();
        const message = status === 'APPROVED' ? 'Leave request approved successfully' : 'Leave request denied successfully';
        this.toastService.show(message, 'success');
        inputElement.value = '';
      },
      error: (err) => {
        this.processingRequests.delete(id);
        this.error = err.error?.error || `Error ${status.toLowerCase()} request`;
        if (this.error) {
          this.toastService.show(this.error, 'error');
        }
        inputElement.value = '';
      }
    });
  }

  confirmSendNote(id: number, comment: string, inputElement: HTMLInputElement): void {
    if (!comment.trim()) {
      this.toastService.show('Please enter a note before sending.', 'error');
      return;
    }
    if (confirm('Are you sure you want to send this note?')) {
      this.sendNote(id, comment, inputElement);
    }
  }

  sendNote(id: number, comment: string, inputElement: HTMLInputElement): void {
    this.processingRequests.add(id);
    this.apiService.sendNote(id, comment).subscribe({
      next: () => {
        this.processingRequests.delete(id);
        this.loadPendingRequests();
        this.toastService.show('Note sent successfully', 'success');
        inputElement.value = '';
      },
      error: (err) => {
        this.processingRequests.delete(id);
        this.error = err.error?.error || 'Error sending note';
        if (this.error) {
          this.toastService.show(this.error, 'error');
        }
        inputElement.value = '';
      }
    });
  }
}

