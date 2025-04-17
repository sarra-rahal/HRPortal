import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../core/services/toast.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.scss']
})
export class ToastComponent implements OnInit, OnDestroy {
  toast: { message: string; type: 'success' | 'error' } | null = null;
  private toastSub?: Subscription;

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    this.toastSub = this.toastService.toast$.subscribe(toast => {
      this.toast = toast;
    });
  }

  ngOnDestroy(): void {
    this.toastSub?.unsubscribe();
  }
}
