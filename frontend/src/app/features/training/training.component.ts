import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WebsocketService } from '../../core/services/websocket.service';
import { Subscription } from 'rxjs';
import { Training, TrainingEnrollment } from '../../core/models/training.model';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { AuthService } from '../../core/services/auth.service';
import { TrainingService } from './training.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-training',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './training.component.html',
  styleUrls: ['./training.component.scss']
})
export class TrainingComponent implements OnInit, OnDestroy {
  trainings: Training[] = [];
  filteredTrainings: Training[] = [];
  enrollments: TrainingEnrollment[] = [];
  employeeEmail: string = '';
  errorMessage: string = '';
  isLoading: boolean = false;
  searchTerm: string = '';
  selectedCategory: string = '';
  selectedStatus: string = '';
  categories: string[] = [];
  private wsSub!: Subscription;
  currentView: 'available' | 'enrollments' = 'available';
  cancellingEnrollment: number | null = null;

  constructor(
    private apiService: TrainingService,
    private websocketService: WebsocketService,
    private authService: AuthService,
    private toastService: ToastService
  ) {
    this.employeeEmail = this.authService.getEmail() || '';
  }

  ngOnInit(): void {
    if (!this.employeeEmail) {
      this.toastService.show('Please sign in to view trainings', 'error');
      return;
    }
    this.isLoading = true;
    this.currentView = 'available';
    this.fetchTrainings();
    this.fetchEnrollments();
    this.setupWebSocket();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  fetchTrainings(): void {
    this.apiService.getAllTrainings().subscribe({
      next: (trainings) => {
        this.trainings = trainings;
        this.filteredTrainings = trainings;
        this.categories = [...new Set(trainings.map(t => t.category))];
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Error fetching trainings: ' + err.message;
        this.toastService.show(this.errorMessage, 'error');
        this.isLoading = false;
      }
    });
  }

  fetchEnrollments(): void {
    this.apiService.getMyEnrollments().subscribe({
      next: (enrollments) => {
        this.enrollments = enrollments;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Error fetching enrollments: ' + err.message;
        this.toastService.show(this.errorMessage, 'error');
        this.isLoading = false;
      }
    });
  }

  enroll(trainingId: number): void {
    this.apiService.enroll(trainingId).subscribe({
      next: () => {
        this.toastService.show('Enrollment submitted successfully', 'success');
        this.fetchEnrollments();
        this.fetchTrainings();
      },
      error: (err) => {
        this.errorMessage = 'Error enrolling: ' + err.message;
        this.toastService.show(this.errorMessage, 'error');
      }
    });
  }

  cancelEnrollment(enrollmentId: number): void {
    if (confirm('Are you sure you want to cancel this enrollment?')) {
      this.cancellingEnrollment = enrollmentId;
      this.apiService.cancelEnrollment(enrollmentId).subscribe({
        next: () => {
          this.toastService.show('Enrollment cancelled successfully', 'success');
          this.fetchEnrollments();
          this.fetchTrainings();
          this.cancellingEnrollment = null;
        },
        error: (err) => {
          this.errorMessage = 'Error cancelling enrollment: ' + err.message;
          this.toastService.show(this.errorMessage, 'error');
          this.cancellingEnrollment = null;
        }
      });
    }
  }

  filterTrainings(): void {
    this.filteredTrainings = this.trainings.filter(training => {
      const matchesSearch = !this.searchTerm ||
        training.title.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        training.description.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchesCategory = !this.selectedCategory || training.category === this.selectedCategory;
      const matchesStatus = !this.selectedStatus || this.getTrainingStatus(training) === this.selectedStatus;
      return matchesSearch && matchesCategory && matchesStatus;
    });
  }

  getTrainingStatus(training: Training): string {
    const now = new Date();
    const start = new Date(training.startDate);
    const end = new Date(training.endDate);
    if (now < start) return 'UPCOMING';
    if (now > end) return 'COMPLETED';
    return 'IN_PROGRESS';
  }

  isEnrollButtonDisabled(training: Training): boolean {
    return this.enrollments.some(e => e.training.id === training.id) ||
      training.enrolledCount >= training.capacity ||
      this.getTrainingStatus(training) !== 'UPCOMING';
  }

  showDetails(training: Training): void {
    console.log('Show details for:', training);
  }

  private setupWebSocket(): void {
    this.wsSub = this.websocketService.connect(`/topic/notifications/${this.employeeEmail}`).subscribe({
      next: (message) => {
        console.log('WebSocket Notification:', message);
        this.fetchEnrollments();
        this.fetchTrainings();
      },
      error: (err) => console.error('WebSocket error:', err)
    });
  }
}
