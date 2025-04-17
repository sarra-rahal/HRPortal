// src/app/core/services/toast.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root' // Global instance
})
export class ToastService {
  private toastSubject = new BehaviorSubject<{ message: string; type: 'success' | 'error' } | null>(null);
  toast$ = this.toastSubject.asObservable();

  show(message: string, type: 'success' | 'error') {
    console.log('Toast displayed:', { message, type });
    this.toastSubject.next({ message, type });
    setTimeout(() => this.toastSubject.next(null), 3000);
  }
}
