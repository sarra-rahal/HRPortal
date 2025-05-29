import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WebsocketService } from '../../core/services/websocket.service';
import { Subscription } from 'rxjs';
import { Training, TrainingEnrollment, TrainingProposal } from '../../core/models/training.model';
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
  proposals: TrainingProposal[] = [];
  newProposal: TrainingProposal = {
    id: 0,
    title: '',
    description: '',
    category: '',
    proposedBy: '',
    status: 'PENDING',
    createdAt: '',
    updatedAt: '',
    hostCompany: '',
    type: 'INTERNAL'
  };
  employeeEmail: string = '';
  errorMessage: string = '';
  isLoading: boolean = false;
  searchTerm: string = '';
  selectedCategory: string = '';
  selectedStatus: string = '';
  categories: string[] = [];
  private wsSub?: Subscription;
  currentView: 'available' | 'enrollments' | 'propose' = 'available';
  cancellingEnrollment: number | null = null;
  editingProposal: boolean = false;

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
    this.fetchProposals();
    this.setupWebSocket();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  setView(view: 'available' | 'enrollments' | 'propose'): void {
    this.currentView = view;
    if (view === 'propose') {
      this.cancelEditProposal();
    }
  }

  fetchTrainings(): void {
    this.isLoading = true;
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
    this.isLoading = true;
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

  fetchProposals(): void {
    this.isLoading = true;
    this.apiService.getMyProposals().subscribe({
      next: (proposals) => {
        this.proposals = proposals;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Error fetching proposals: ' + err.message;
        this.toastService.show(this.errorMessage, 'error');
        this.isLoading = false;
      }
    });
  }

  proposeTraining(): void {
    this.isLoading = true;
    this.newProposal.proposedBy = this.employeeEmail;
    this.apiService.proposeTraining(this.newProposal).subscribe({
      next: () => {
        this.toastService.show('Training proposal submitted successfully', 'success');
        this.fetchProposals();
        this.resetProposalForm();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(this.errorMessage, 'error');
        this.isLoading = false;
      }
    });
  }

  updateProposal(): void {
    this.isLoading = true;
    this.apiService.updateTrainingProposal(this.newProposal.id, this.newProposal).subscribe({
      next: () => {
        this.toastService.show('Training proposal updated successfully', 'success');
        this.fetchProposals();
        this.resetProposalForm();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(this.errorMessage, 'error');
        this.isLoading = false;
      }
    });
  }

  cancelProposal(proposalId: number): void {
    if (confirm('Are you sure you want to cancel this proposal?')) {
      this.isLoading = true;
      this.apiService.cancelTrainingProposal(proposalId).subscribe({
        next: () => {
          this.toastService.show('Training proposal cancelled successfully', 'success');
          this.fetchProposals();
          this.isLoading = false;
        },
        error: (err) => {
          this.errorMessage = err.message;
          this.toastService.show(this.errorMessage, 'error');
          this.isLoading = false;
        }
      });
    }
  }

  editProposal(proposal: TrainingProposal): void {
    this.newProposal = { ...proposal };
    this.editingProposal = true;
    this.currentView = 'propose';
  }

  cancelEditProposal(): void {
    this.resetProposalForm();
    this.editingProposal = false;
  }

  private resetProposalForm(): void {
    this.newProposal = {
      id: 0,
      title: '',
      description: '',
      category: '',
      proposedBy: this.employeeEmail,
      status: 'PENDING',
      createdAt: '',
      updatedAt: '',
      hostCompany: '',
      type: 'INTERNAL'
    };
    this.editingProposal = false;
  }

  enroll(trainingId: number): void {
    this.isLoading = true;
    this.apiService.enroll(trainingId).subscribe({
      next: () => {
        this.toastService.show('Enrollment successful', 'success');
        this.fetchEnrollments();
        this.fetchTrainings();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Error enrolling: ' + err.message;
        this.toastService.show(this.errorMessage, 'error');
        this.isLoading = false;
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
    if (now < start) {
      return 'UPCOMING';
    } else if (now > end) {
      return 'COMPLETED';
    } else {
      return 'IN_PROGRESS';
    }
  }

  isEnrollButtonDisabled(training: Training): boolean {
    return this.enrollments.some(e => e.training?.id === training.id) ||
      training.enrolledCount >= training.capacity ||
      this.getTrainingStatus(training) !== 'UPCOMING';
  }

  private setupWebSocket(): void {
    this.wsSub = this.websocketService.connect(`/topic/notifications/${this.employeeEmail}`).subscribe({
      next: (message) => {
        console.log('WebSocket Notification:', message);
        this.fetchEnrollments();
        this.fetchTrainings();
        this.fetchProposals();
      },
      error: (err) => console.error('WebSocket error:', err)
    });
  }
}
