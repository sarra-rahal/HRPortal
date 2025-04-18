import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { AuthService } from '../../core/services/auth.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { ToastService } from '../../core/services/toast.service';
import { Subscription } from 'rxjs';
import { LeaveService } from './leave.service';

@Component({
  selector: 'app-leave',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './leave.component.html',
  styleUrls: ['./leave.component.scss']
})
export class LeaveComponent implements OnInit, OnDestroy {
  leaveForm: FormGroup;
  requests: any[] = [];
  filteredRequests: any[] = [];
  error: string | null = null;
  isLoading: boolean = false;
  isSubmitting: boolean = false;
  isEditing: boolean = false;
  editingRequestId: number | null = null;
  startDateFilter: string = '';
  endDateFilter: string = '';
  statusFilter: string = '';
  currentView: 'submit' | 'history' = 'submit';
  cancellingRequest: number | null = null;
  private wsSub?: Subscription;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private apiService: LeaveService,
    private websocketService: WebsocketService,
    private toastService: ToastService
  ) {
    this.leaveForm = this.fb.group({
      leaveType: ['', Validators.required],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      reason: ['']
    });
  }

  ngOnInit(): void {
    this.loadRequests();
    this.setupWebSocket();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  loadRequests(): void {
    this.isLoading = true;
    this.apiService.getMyLeaveRequests().subscribe({
      next: (data) => {
        this.requests = data;
        this.filteredRequests = data;
        this.isLoading = false;
        this.filterRequests();
      },
      error: (err) => {
        this.error = err.message;
        this.isLoading = false;
        this.toastService.show('Error loading leave requests: ' + err.message, 'error');
      }
    });
  }

  onSubmit(): void {
    if (this.leaveForm.invalid) return;
    this.isSubmitting = true;
    this.error = null;

    const request = this.leaveForm.value;
    if (this.isEditing && this.editingRequestId) {
      this.apiService.updateLeaveRequest(this.editingRequestId, request).subscribe({
        next: (data) => {
          this.toastService.show(`Leave request for ${data.leaveType} updated successfully`, 'success');
          this.leaveForm.reset();
          this.isEditing = false;
          this.editingRequestId = null;
          this.loadRequests();
          this.isSubmitting = false;
        },
        error: (err) => {
          this.error = err.error?.error || 'Failed to update leave request.';
          this.toastService.show('Error updating leave request: ' + err.message, 'error');
          this.isSubmitting = false;
        }
      });
    } else {
      this.apiService.submitLeaveRequest(request).subscribe({
        next: (data) => {
          this.toastService.show(`Leave request for ${data.leaveType} submitted successfully`, 'success');
          this.leaveForm.reset();
          this.loadRequests();
          this.isSubmitting = false;
        },
        error: (err) => {
          this.error = err.error?.error || 'Failed to submit leave request.';
          this.toastService.show('Error submitting leave request: ' + err.message, 'error');
          this.isSubmitting = false;
        }
      });
    }
  }

  cancelRequest(id: number): void {
    if (confirm('Are you sure you want to cancel this request?')) {
      this.cancellingRequest = id;
      this.apiService.cancelLeaveRequest(id).subscribe({
        next: () => {
          this.toastService.show('Leave request cancelled successfully', 'success');
          this.loadRequests();
          this.cancellingRequest = null;
        },
        error: (err) => {
          this.error = err.message;
          this.toastService.show('Error cancelling request: ' + err.message, 'error');
          this.cancellingRequest = null;
        }
      });
    }
  }

  editRequest(request: any): void {
    this.isEditing = true;
    this.editingRequestId = request.id;
    this.currentView = 'submit';
    this.leaveForm.patchValue({
      leaveType: request.leaveType,
      startDate: request.startDate,
      endDate: request.endDate,
      reason: request.reason || ''
    });
  }

  filterRequests(): void {
    this.filteredRequests = this.requests.filter(req => {
      const matchesStartDate = !this.startDateFilter || req.startDate.includes(this.startDateFilter);
      const matchesEndDate = !this.endDateFilter || req.endDate.includes(this.endDateFilter);
      const matchesStatus = !this.statusFilter || req.status === this.statusFilter;
      return matchesStartDate && matchesEndDate && matchesStatus;
    });
  }

  private setupWebSocket(): void {
    const email = this.authService.getEmail() || '';
    this.wsSub = this.websocketService.connect(`/topic/notifications/${email}`).subscribe({
      next: () => this.loadRequests(),
      error: (err) => console.error('WebSocket error:', err)
    });
  }
}
