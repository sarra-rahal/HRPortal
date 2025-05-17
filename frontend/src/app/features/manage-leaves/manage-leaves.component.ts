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
  employeeName?: string; // Optional field for employee name
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
  processingRequests: Set<number> = new Set(); // Track requests being processed

  constructor(private apiService: ManageLeavesService, private toastService: ToastService) {}

  ngOnInit(): void {
    this.loadPendingRequests();
  }

  loadPendingRequests(): void {
    this.loading = true;
    this.error = null;
    this.apiService.getAllLeaveRequests().subscribe({
      next: (data) => {
        this.requests = data
          .filter(req => req.status === 'PENDING')
          .map(req => ({
            ...req,
            employeeName: req.employeeName || req.employeeCin // Fallback to CIN if name unavailable
          }));
        this.loading = false;
      },
      error: (err) => {
        this.error = `Error fetching leave requests: ${err.error?.error || err.message}`;
        this.loading = false;
        this.toastService.show(this.error, 'error');
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
        const message = status === 'APPROVED'
          ? 'Leave request approved successfully'
          : 'Leave request denied successfully';
        this.toastService.show(message, 'success');
        inputElement.value = ''; // Clear comment field
      },
      error: (err) => {
        this.processingRequests.delete(id);
        const errorMessage = err.error?.error || `Error ${status.toLowerCase()} request: ${err.message}`;
        this.toastService.show(errorMessage, 'error');
        inputElement.value = ''; // Clear comment field even on error
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
    this.apiService.sendnote(id, comment).subscribe({
      next: () => {
        this.processingRequests.delete(id);
        this.loadPendingRequests();
        this.toastService.show('Note sent successfully', 'success');
        inputElement.value = ''; // Clear comment field
      },
      error: (err) => {
        this.processingRequests.delete(id);
        const errorMessage = err.error?.error || `Error sending note: ${err.message}`;
        this.toastService.show(errorMessage, 'error');
        inputElement.value = ''; // Clear comment field even on error
      }
    });
  }
}
