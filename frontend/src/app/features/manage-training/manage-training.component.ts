import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { Subscription } from 'rxjs';
import { Training, TrainingEnrollment } from '../../core/models/training.model';
import { Router } from '@angular/router';
import {NavbarComponent} from '../../layout/navbar/navbar.component';
import {SidebarComponent} from '../../layout/sidebar/sidebar.component';
import {FooterComponent} from '../../layout/footer/footer.component';
import {AuthService} from '../../core/services/auth.service';

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
  newTraining: Training = { id: 0, title: '', description: '', startDate: '', endDate: '', capacity: 0, enrolledCount: 0 };
  errorMessage: string = '';
  private wsSub!: Subscription;

  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private websocketService: WebsocketService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (localStorage.getItem('role') !== 'HR_MANAGER') {
      this.router.navigate(['/unauthorized']);
      return;
    }
    this.fetchTrainings();
    this.fetchEnrollments();
    this.setupWebSocket();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  fetchTrainings(): void {
    this.apiService.getAllTrainings().subscribe({
      next: (trainings) => this.trainings = trainings,
      error: (err) => this.errorMessage = 'Error fetching trainings: ' + err.message
    });
  }

  fetchEnrollments(): void {
    this.apiService.getMyEnrollments('').subscribe({
      next: (enrollments) => this.enrollments = enrollments,
      error: (err) => this.errorMessage = 'Error fetching enrollments: ' + err.message
    });
  }

  createTraining(): void {
    this.apiService.createTraining(this.newTraining).subscribe({
      next: () => {
        this.fetchTrainings();
        this.newTraining = { id: 0, title: '', description: '', startDate: '', endDate: '', capacity: 0, enrolledCount: 0 };
      },
      error: (err) => this.errorMessage = 'Error creating training: ' + err.message
    });
  }

  reviewEnrollment(enrollmentId: number, status: string): void {
    this.apiService.reviewEnrollment(enrollmentId, status).subscribe({
      next: () => this.fetchEnrollments(),
      error: (err) => this.errorMessage = 'Error reviewing enrollment: ' + err.message
    });
  }

  private setupWebSocket(): void {
    const hrEmail = this.authService.getEmail() || '';
    this.wsSub = this.websocketService.connect(`/topic/notifications/${hrEmail}`).subscribe({
      next: () => this.fetchEnrollments(),
      error: (err) => console.error('WebSocket error:', err)
    });
  }
}
