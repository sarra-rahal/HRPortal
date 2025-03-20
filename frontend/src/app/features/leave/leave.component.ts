import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-leave',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './leave.component.html',
  styleUrls: ['./leave.component.scss']
})
export class LeaveComponent implements OnInit {
  leaveForm: FormGroup;
  requests: any[] = [];
  error: string | null = null;
  success: string | null = null;
  // Track which sections are open
  expandedSections: { [key: string]: boolean } = {
    submitRequest: false,
    history: true
  };

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private apiService: ApiService
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
  }

  toggleSection(section: string): void {
    this.expandedSections[section] = !this.expandedSections[section];
  }

  loadRequests(): void {
    this.apiService.getMyLeaveRequests().subscribe({
      next: (data) => {
        this.requests = data.map(req => ({
          ...req,
          duration: this.calculateDuration(req.startDate, req.endDate)
        }));
      },
      error: (err) => this.error = err.message
    });
  }

  onSubmit(): void {
    if (this.leaveForm.invalid) return;

    this.error = null;
    this.success = null;

    const request = this.leaveForm.value;
    this.apiService.submitLeaveRequest(request).subscribe({
      next: (data) => {
        this.success = `Your request for ${data.leaveType} from ${data.startDate} to ${data.endDate} has been submitted.`;
        this.leaveForm.reset();
        this.loadRequests();
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to submit leave request. Please try again.';
      }
    });
  }

  cancelRequest(id: number): void {
    if (confirm('Are you sure you want to cancel this request?')) {
      this.apiService.cancelLeaveRequest(id).subscribe({
        next: () => this.loadRequests(),
        error: (err) => this.error = err.message
      });
    }
  }

  calculateDuration(startDate: string, endDate: string): number {
    const start = new Date(startDate);
    const end = new Date(endDate);
    return Math.ceil((end.getTime() - start.getTime()) / (1000 * 3600 * 24)) + 1;
  }
}
