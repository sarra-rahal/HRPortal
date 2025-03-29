import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { AuthService } from '../../core/services/auth.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { Subscription } from 'rxjs';
import { LeaveService } from './leave.service';

@Component({
  selector: 'app-leave',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './leave.component.html',
  styleUrls: ['./leave.component.scss']
})
export class LeaveComponent implements OnInit, OnDestroy {
  leaveForm: FormGroup;
  requests: any[] = [];
  error: string | null = null;
  success: string | null = null;
  expandedSections: { [key: string]: boolean } = { submitRequest: false, history: true };
  private wsSub?: Subscription;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private apiService: LeaveService,
    private websocketService: WebsocketService
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

  toggleSection(section: string): void {
    this.expandedSections[section] = !this.expandedSections[section];
  }

  loadRequests(): void {
    this.apiService.getMyLeaveRequests().subscribe({
      next: (data) => this.requests = data,
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
      error: (err) => this.error = err.error?.error || 'Failed to submit leave request.'
    });
  }

  cancelRequest(id: number): void {
    if (confirm('Are you sure you want to cancel this request?')) {
      this.apiService.cancelLeaveRequest(id).subscribe({
        next: () => {
          this.success = 'Leave request canceled successfully.';
          this.loadRequests();
        },
        error: (err) => this.error = err.message
      });
    }
  }

  private setupWebSocket(): void {
    const email = this.authService.getEmail() || '';
    this.wsSub = this.websocketService.connect(`/topic/notifications/${email}`).subscribe({
      next: () => this.loadRequests(),
      error: (err) => console.error('WebSocket error:', err)
    });
  }
}
