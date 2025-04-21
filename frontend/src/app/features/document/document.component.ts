import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { Document, DocumentRequest } from '../../core/models/document.model';
import { saveAs } from 'file-saver';
import { ToastService } from '../../core/services/toast.service';
import { DocumentService } from './document.service';
import { WebsocketService } from '../../core/services/websocket.service';

@Component({
  selector: 'app-document',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './document.component.html',
  styleUrls: ['./document.component.scss']
})
export class DocumentComponent implements OnInit {
  documents: Document[] = [];
  filteredDocuments: Document[] = [];
  documentRequests: DocumentRequest[] = [];
  filteredRequests: DocumentRequest[] = [];
  errorMessage: string = '';
  isLoading: boolean = false;
  currentView: 'documents' | 'requests' = 'documents';
  searchTerm: string = '';
  year: string = '';
  month: string = '';
  day: string = '';
  newRequest: DocumentRequest = {
    type: 'work_certificate',
    description: '',
    status: 'PENDING',
    requestDate: '',
    urgency: false,
    employeeEmail: ''
  };
  requestTypes: string[] = ['work_certificate', 'leave_certificate', 'other'];
  years: string[] = Array.from({ length: 10 }, (_, i) => (2025 - i).toString());
  months: string[] = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'];
  days: string[] = Array.from({ length: 31 }, (_, i) => (i + 1).toString().padStart(2, '0'));

  constructor(
    private documentService: DocumentService,
    private toastService: ToastService,
    private websocketService: WebsocketService
  ) {}

  ngOnInit(): void {
    this.fetchDocuments();
    this.fetchRequests();
    this.subscribeToNotifications();
  }

  fetchDocuments(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.documentService.getAllDocuments().subscribe({
      next: (docs) => {
        this.documents = docs;
        this.filteredDocuments = docs;
        this.isLoading = false;
        this.filterDocuments();
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  fetchRequests(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.documentService.getMyRequests().subscribe({
      next: (requests) => {
        this.documentRequests = requests;
        this.filteredRequests = requests;
        this.isLoading = false;
        this.filterRequests();
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  subscribeToNotifications(): void {
    const email = localStorage.getItem('email');
    if (email) {
      this.websocketService.connect(`/topic/notifications/${email}`).subscribe({
        next: (notification) => {
          this.toastService.show(notification.message, 'success');
          if (notification.message.includes('document request')) {
            this.fetchRequests();
          } else if (notification.message.includes('New document')) {
            this.fetchDocuments();
          }
        },
        error: (err) => {
          console.error('WebSocket error:', err);
        }
      });
    }
  }

  downloadDocument(id: number, name: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.documentService.downloadDocument(id).subscribe({
      next: (blob) => {
        saveAs(blob, name);
        this.isLoading = false;
        this.toastService.show(`Downloaded "${name}" successfully`, 'success');
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  filterDocuments(): void {
    this.filteredDocuments = this.documents.filter(doc => {
      const matchesSearch = !this.searchTerm ||
        doc.name.toLowerCase().includes(this.searchTerm.toLowerCase());
      const uploadDate = new Date(doc.uploadDate);
      const matchesYear = !this.year || uploadDate.getFullYear().toString() === this.year;
      const matchesMonth = !this.month || (uploadDate.getMonth() + 1).toString().padStart(2, '0') === this.month;
      const matchesDay = !this.day || uploadDate.getDate().toString().padStart(2, '0') === this.day;
      return matchesSearch && matchesYear && matchesMonth && matchesDay;
    });
  }

  filterRequests(): void {

    this.filteredRequests = this.documentRequests.filter(req => {
      var matchesSearch = !this.searchTerm ||
        req.type.toLowerCase().includes(this.searchTerm.toLowerCase());
      if(req.description != undefined){
        matchesSearch = !this.searchTerm ||
          req.type.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
          req.description.toLowerCase().includes(this.searchTerm.toLowerCase());
      }
      const requestDate = new Date(req.requestDate);
      const matchesYear = !this.year || requestDate.getFullYear().toString() === this.year;
      const matchesMonth = !this.month || (requestDate.getMonth() + 1).toString().padStart(2, '0') === this.month;
      const matchesDay = !this.day || requestDate.getDate().toString().padStart(2, '0') === this.day;
      return matchesSearch && matchesYear && matchesMonth && matchesDay;
    });
  }

  requestDocument(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.documentService.requestDocument(this.newRequest).subscribe({
      next: (request) => {
        this.documentRequests.push(request);
        this.filteredRequests = this.documentRequests;
        this.filterRequests();
        this.toastService.show('Document request submitted successfully', 'success');
        this.newRequest = { id: 0, type: 'work_certificate', description: '', status: 'PENDING', requestDate: '', urgency: false, employeeEmail: '' };
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  cancelRequest(id: number | undefined): void {
    if(id == undefined){
      this.toastService.show('Request id not found', 'error');
    }
    else{
      if (confirm('Are you sure you want to cancel this request?')) {
        this.isLoading = true;
        this.errorMessage = '';
        this.documentService.cancelRequest(id).subscribe({
          next: () => {
            this.documentRequests = this.documentRequests.filter(req => req.id !== id);
            this.filteredRequests = this.documentRequests;
            this.filterRequests();
            this.toastService.show('Request cancelled successfully', 'success');
            this.isLoading = false;
          },
          error: (err) => {
            this.errorMessage = err.message;
            this.toastService.show(err.message, 'error');
            this.isLoading = false;
          }
        });
      }
    }
  }
}
