import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { Subscription } from 'rxjs';
import { Training, TrainingEnrollment } from '../../core/models/training.model';
import {NavbarComponent} from '../../layout/navbar/navbar.component';
import {SidebarComponent} from '../../layout/sidebar/sidebar.component';
import {FooterComponent} from '../../layout/footer/footer.component';
import {AuthService} from '../../core/services/auth.service';

@Component({
  selector: 'app-training',
  standalone: true,
  imports: [CommonModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './training.component.html',
  styleUrls: ['./training.component.scss']
})
export class TrainingComponent implements OnInit, OnDestroy {
  trainings: Training[] = [];
  enrollments: TrainingEnrollment[] = [];
  employeeEmail: string = localStorage.getItem('email') || '';
  errorMessage: string = '';
  private wsSub!: Subscription;

  constructor(
    private apiService: ApiService,
    private websocketService: WebsocketService,
    private authService: AuthService
  ) {
    this.employeeEmail = this.authService.getEmail() || '';
  }
  ngOnInit(): void {
    console.log('Employee Email:', this.employeeEmail);
    if (!this.employeeEmail) return;
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
    this.apiService.getMyEnrollments(this.employeeEmail).subscribe({
      next: (enrollments) => this.enrollments = enrollments,
      error: (err) => this.errorMessage = 'Error fetching enrollments: ' + err.message
    });
  }

  enroll(trainingId: number): void {
    this.apiService.enroll(trainingId).subscribe({
      next: () => this.fetchEnrollments(),
      error: (err) => this.errorMessage = 'Error enrolling: ' + err.message
    });
  }

  isEnrollButtonDisabled(training: Training): boolean {
    return this.enrollments.some(enrollment => enrollment.training.id === training.id) ||
      training.enrolledCount >= training.capacity;
  }

  private setupWebSocket(): void {
    this.wsSub = this.websocketService.connect(`/topic/notifications/${this.employeeEmail}`).subscribe({
      next: () => this.fetchEnrollments(),
      error: (err) => console.error('WebSocket error:', err)
    });
  }
}
