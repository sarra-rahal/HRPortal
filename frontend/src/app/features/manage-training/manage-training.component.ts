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
import { ManageTrainingService } from './manage-training.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-manage-training',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './manage-training.component.html',
  styleUrls: ['./manage-training.component.scss']
})
export class ManageTrainingComponent implements OnInit, OnDestroy {
  trainings: Training[] = [];
  enrollments: TrainingEnrollment[] = [];
  newTraining: Training = {
    id: 0,
    title: '',
    description: '',
    startDate: '',
    endDate: '',
    capacity: 0,
    enrolledCount: 0,
    category: '',
    rating: 0
  };
  errorMessage: string = '';
  isLoading: boolean = false;
  isEditing: boolean = false;
  currentView: 'create' | 'list' | 'enrollments' = 'create';
  private wsSub!: Subscription;

  constructor(
    private apiService: ManageTrainingService,
    private authService: AuthService,
    private websocketService: WebsocketService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.setView('create');
    this.fetchTrainings();
    this.fetchEnrollments();
    this.setupWebSocket();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  setView(view: 'create' | 'list' | 'enrollments'): void {
    this.currentView = view;
    if (view === 'create') {
      this.cancelEdit();
    }
  }

  fetchTrainings(): void {
    this.isLoading = true;
    this.apiService.getAllTrainings().subscribe({
      next: (trainings) => {
        this.trainings = trainings;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show('Error fetching trainings: ' + err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  fetchEnrollments(): void {
    this.isLoading = true;
    this.apiService.getPendingEnrollments().subscribe({
      next: (enrollments) => {
        this.enrollments = enrollments;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show('Error fetching enrollments: ' + err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  createTraining(): void {
    this.isLoading = true;
    this.apiService.createTraining(this.newTraining).subscribe({
      next: () => {
        this.toastService.show('Training created successfully', 'success');
        this.fetchTrainings();
        this.resetForm();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  updateTraining(): void {
    this.isLoading = true;
    this.apiService.updateTraining(this.newTraining).subscribe({
      next: () => {
        this.toastService.show('Training updated successfully', 'success');
        this.fetchTrainings();
        this.resetForm();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  deleteTraining(trainingId: number): void {
    if (!confirm('Are you sure you want to delete this training?')) {
      return;
    }
    this.isLoading = true;
    this.apiService.deleteTraining(trainingId).subscribe({
      next: () => {
        this.toastService.show('Training deleted successfully', 'success');
        this.fetchTrainings();
        this.fetchEnrollments();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  editTraining(training: Training): void {
    this.newTraining = { ...training };
    this.isEditing = true;
    this.currentView = 'create';
  }

  cancelEdit(): void {
    this.resetForm();
    this.isEditing = false;
  }

  private resetForm(): void {
    this.newTraining = {
      id: 0,
      title: '',
      description: '',
      startDate: '',
      endDate: '',
      capacity: 0,
      enrolledCount: 0,
      category: '',
      rating: 0
    };
    this.isEditing = false;
  }

  reviewEnrollment(enrollmentId: number, status: string): void {
    this.isLoading = true;
    this.apiService.reviewEnrollment(enrollmentId, status).subscribe({
      next: () => {
        this.toastService.show(`Enrollment ${status.toLowerCase()} successfully`, 'success');
        this.fetchEnrollments();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  private setupWebSocket(): void {
    const hrEmail = this.authService.getEmail() || '';
    this.wsSub = this.websocketService.connect(`/topic/notifications/${hrEmail}`).subscribe({
      next: (message) => {
        console.log('WebSocket Notification:', message);
        this.fetchEnrollments();
      },
      error: (err) => console.error('WebSocket error:', err)
    });
  }
}
