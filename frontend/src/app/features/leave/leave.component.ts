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
import { ActivatedRoute, Router } from '@angular/router';

interface LeaveBalance {
  annual: number;
  sick: number;
}

interface LeaveRequest {
  id: number;
  employeeCin: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  duration: number;
  reason: string;
  status: string;
  submittedAt: string;
  hrComment: string;
  certificate?: string;
}

@Component({
  selector: 'app-leave',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './leave.component.html',
  styleUrls: ['./leave.component.scss']
})
export class LeaveComponent implements OnInit, OnDestroy {
  leaveForm: FormGroup;
  sickRequests: LeaveRequest[] = [];
  annualRequests: LeaveRequest[] = [];
  filteredSickRequests: LeaveRequest[] = [];
  filteredAnnualRequests: LeaveRequest[] = [];
  leaveBalance: LeaveBalance | null = null;
  error: string | null = null;
  isLoading: boolean = false;
  isSubmitting: boolean = false;
  isEditing: boolean = false;
  editingRequestId: number | null = null;
  startDateFilter: string = '';
  endDateFilter: string = '';
  statusFilter: string = '';
  cancellingRequest: number | null = null;
  currentView: 'sick' | 'annual' | 'history' = 'history';
  certificateFile: File | null = null;
  private wsSub?: Subscription;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private apiService: LeaveService,
    private websocketService: WebsocketService,
    private toastService: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.leaveForm = this.fb.group({
      leaveType: ['', Validators.required],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      reason: [''],
      certificate: [null, this.currentView === 'sick' ? Validators.required : null]
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.currentView = params['view'] || 'sick';
      this.initializeForm();
      this.loadRequests();
      this.loadLeaveBalance();
    });
    this.setupWebSocket();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  navigateToView(view: 'sick' | 'annual' | 'history'): void {
    this.currentView = view;
    this.initializeForm();
    this.router.navigate([], { queryParams: { view } });
  }

  initializeForm(): void {
    const certificateValidator = this.currentView === 'sick' && !this.isEditing ? Validators.required : null;
    this.leaveForm = this.fb.group({
      leaveType: [this.currentView === 'sick' ? 'SICK' : 'ANNUAL', Validators.required],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      reason: [''],
      certificate: [null, certificateValidator]
    });
    console.log('Form initialized:', this.leaveForm.value, 'Valid:', this.leaveForm.valid);
  }

  loadRequests(): void {
    this.isLoading = true;
    this.apiService.getMyLeaveRequests().subscribe({
      next: (data) => {
        this.sickRequests = data.filter(req => req.leaveType === 'SICK');
        this.annualRequests = data.filter(req => req.leaveType === 'ANNUAL');
        this.filteredSickRequests = this.sickRequests;
        this.filteredAnnualRequests = this.annualRequests;
        this.isLoading = false;
        this.filterRequests();
      },
      error: (err) => {
        this.error = err.error?.error || 'Error loading leave requests';
        this.isLoading = false;
        if (this.error) {
          this.toastService.show(this.error, 'error');
        }
      }
    });
  }

  loadLeaveBalance(): void {
    this.isLoading = true;
    this.apiService.getLeaveBalance().subscribe({
      next: (data) => {
        this.leaveBalance = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.error = err.error?.error || 'Error loading leave balance';
        this.isLoading = false;
        if (this.error) {
          this.toastService.show(this.error, 'error');
        }
      }
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length) {
      this.certificateFile = input.files[0];
      this.leaveForm.patchValue({ certificate: this.certificateFile });
      this.leaveForm.get('certificate')?.updateValueAndValidity();
      console.log('Certificate set:', this.certificateFile, 'Form valid:', this.leaveForm.valid);
    }
  }

  onSubmit(): void {
    if (this.leaveForm.invalid) {
      console.log('Form invalid:', this.leaveForm.errors, this.leaveForm.value);
      this.leaveForm.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.error = null;

    const startDate = new Date(this.leaveForm.get('startDate')?.value).toISOString().split('T')[0];
    const endDate = new Date(this.leaveForm.get('endDate')?.value).toISOString().split('T')[0];
    const reason = this.leaveForm.get('reason')?.value || '';

    if (this.currentView === 'sick') {
      const formData = new FormData();
      formData.append('startDate', startDate);
      formData.append('endDate', endDate);
      formData.append('reason', reason);
      if (this.certificateFile) {
        formData.append('certificate', this.certificateFile);
      }

      const request = this.isEditing && this.editingRequestId
        ? this.apiService.updateSickLeaveRequest(this.editingRequestId, formData)
        : this.apiService.submitSickLeaveRequest(formData);

      request.subscribe({
        next: (data) => {
          this.toastService.show(`Sick leave request ${this.isEditing ? 'updated' : 'submitted'} successfully`, 'success');
          this.leaveForm.reset();
          this.certificateFile = null;
          this.isEditing = false;
          this.editingRequestId = null;
          this.initializeForm();
          this.loadRequests();
          this.loadLeaveBalance();
          this.isSubmitting = false;
        },
        error: (err) => {
          this.error = err.error?.error || `Failed to ${this.isEditing ? 'update' : 'submit'} sick leave request`;
          console.error('Sick leave request error:', err);
          this.toastService.show(this.error || 'Unknown error', 'error');
          this.isSubmitting = false;
        }
      });
    } else {
      // For annual leave, send parameters directly
      const request = this.isEditing && this.editingRequestId
        ? this.apiService.updateAnnualLeaveRequest(this.editingRequestId, { startDate, endDate, reason })
        : this.apiService.submitAnnualLeaveRequest({ startDate, endDate, reason });

      request.subscribe({
        next: (data) => {
          this.toastService.show(`Annual leave request ${this.isEditing ? 'updated' : 'submitted'} successfully`, 'success');
          this.leaveForm.reset();
          this.certificateFile = null;
          this.isEditing = false;
          this.editingRequestId = null;
          this.initializeForm();
          this.loadRequests();
          this.loadLeaveBalance();
          this.isSubmitting = false;
        },
        error: (err) => {
          this.error = err.error?.error || `Failed to ${this.isEditing ? 'update' : 'submit'} annual leave request`;
          console.error('Annual leave request error:', err);
          this.toastService.show(this.error || 'Unknown error', 'error');
          this.isSubmitting = false;
        }
      });
    }
  }

  cancelRequest(id: number, action: 'C' | 'D'): void {
    if (confirm(`Are you sure you want to ${action === 'C' ? 'cancel' : 'delete'} this request?`)) {
      this.cancellingRequest = id;
      this.apiService.cancelLeaveRequest(id).subscribe({
        next: () => {
          this.toastService.show(`Leave request ${action === 'C' ? 'cancelled' : 'deleted'} successfully`, 'success');
          this.loadRequests();
          this.loadLeaveBalance();
          this.cancellingRequest = null;
        },
        error: (err) => {
          this.error = err.error?.error || `Error ${action === 'C' ? 'cancelling' : 'deleting'} request`;
          this.toastService.show(this.error || 'Unknown error', 'error');
          this.cancellingRequest = null;
        }
      });
    }
  }

  editRequest(request: LeaveRequest): void {
    this.isEditing = true;
    this.editingRequestId = request.id;
    this.currentView = request.leaveType === 'SICK' ? 'sick' : 'annual';
    this.initializeForm();
    this.leaveForm.patchValue({
      leaveType: request.leaveType,
      startDate: request.startDate,
      endDate: request.endDate,
      reason: request.reason || '',
      certificate: null // Certificate not patched for editing
    });
    this.navigateToView(this.currentView);
    console.log('Editing form:', this.leaveForm.value, 'Valid:', this.leaveForm.valid);
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.editingRequestId = null;
    this.certificateFile = null;
    this.initializeForm();
  }

  filterRequests(): void {
    this.filteredSickRequests = this.sickRequests.filter(req => {
      const matchesStartDate = !this.startDateFilter || req.startDate.includes(this.startDateFilter);
      const matchesEndDate = !this.endDateFilter || req.endDate.includes(this.endDateFilter);
      const matchesStatus = !this.statusFilter || req.status === this.statusFilter;
      return matchesStartDate && matchesEndDate && matchesStatus;
    });
    this.filteredAnnualRequests = this.annualRequests.filter(req => {
      const matchesStartDate = !this.startDateFilter || req.startDate.includes(this.startDateFilter);
      const matchesEndDate = !this.endDateFilter || req.endDate.includes(this.endDateFilter);
      const matchesStatus = !this.statusFilter || req.status === this.statusFilter;
      return matchesStartDate && matchesEndDate && matchesStatus;
    });
  }

  private setupWebSocket(): void {
    const email = this.authService.getEmail();
    if (!email) {
      console.error('No email found for WebSocket connection');
      return;
    }
    this.wsSub = this.websocketService.connect(`/user/${email}/topic/notifications`).subscribe({
      next: () => {
        this.loadRequests();
      },
      error: (err) => console.error('WebSocket error:', err)
    });
  }
}
