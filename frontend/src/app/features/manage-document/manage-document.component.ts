import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { Document, DocumentRequest } from '../../core/models/document.model';
import { saveAs } from 'file-saver';
import { ToastService } from '../../core/services/toast.service';
import { DocumentService } from '../document/document.service';
import { ManageDocumentService } from './manage-document.service';
import { WebsocketService } from '../../core/services/websocket.service';
import {ManageEmployeesService} from '../manage-employees/manage-employees.service';
import {Department} from '../../core/models/Employee-departments.model';

@Component({
  selector: 'app-manage-document',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './manage-document.component.html',
  styleUrls: ['./manage-document.component.scss']
})
export class ManageDocumentComponent implements OnInit {
  documents: Document[] = [];
  departments: Department[] = [];
  filteredDocuments: Document[] = [];
  documentRequests: DocumentRequest[] = [];
  filteredRequests: DocumentRequest[] = [];

  fileToUpload: File | null = null;
  employeeCin: string = '';
  newDocument: { cin: string; templateType: string } = {
    cin: '',
    templateType: 'work_certificate'
  };
  templateTypes: string[] = ['work_certificate', 'leave_certificate'];
  errorMessage: string = '';
  isLoading: boolean = false;
  currentView: 'upload' | 'generate' | 'list' | 'requests' = 'requests';
  searchTerm: string = '';
  year: string = '';
  month: string = '';
  day: string = '';
  years: string[] = Array.from({ length: 10 }, (_, i) => (2025 - i).toString());
  months: string[] = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'];
  days: string[] = Array.from({ length: 31 }, (_, i) => (i + 1).toString().padStart(2, '0'));

  constructor(
    private documentService: DocumentService,
    private manageDocumentService: ManageDocumentService,
    private manageEmployeesService: ManageEmployeesService,
    private toastService: ToastService,
    private websocketService: WebsocketService
  ) {}

  ngOnInit(): void {
    this.fetchDocuments();
    this.fetchDepartments()
    this.fetchPendingRequests();
    this.subscribeToNotifications();
  }

  fetchDepartments(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.manageEmployeesService.getDepartments().subscribe({
      next: (departments) => {
        this.departments = departments;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
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

  fetchPendingRequests(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.manageDocumentService.getPendingRequests().subscribe({
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
    const email = localStorage.getItem('email') || 'hr@example.com';
    this.websocketService.connect(`/topic/notifications/${email}`).subscribe({
      next: (notification) => {
        this.toastService.show(notification.message, 'success');
        if (notification.message.includes('document request')) {
          this.fetchPendingRequests();
        }
      },
      error: (err) => {
        console.error('WebSocket error:', err);
      }
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fileToUpload = input.files?.[0] || null;
  }

  isValidCin(): boolean {
    const cinRegex = /^[0-9]{8}$/;
    return cinRegex.test(this.newDocument.cin) || (this.currentView === 'upload' && cinRegex.test(this.employeeCin));
  }

  uploadDocument(): void {
    if (!this.fileToUpload || !this.employeeCin) {
      this.errorMessage = 'Please select a file and enter a valid CIN';
      this.toastService.show('Please select a file and enter a valid CIN', 'error');
      return;
    }
    if (!this.isValidCin()) {
      this.errorMessage = 'CIN must be 8 digits';
      this.toastService.show('CIN must be 8 digits', 'error');
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    const accessibleBy = { type: 'employees', value: [this.employeeCin] };
    this.manageDocumentService.uploadDocument(this.fileToUpload, accessibleBy).subscribe({
      next: (doc) => {
        this.toastService.show(`Uploaded "${doc.name}" successfully`, 'success');
        this.fileToUpload = null;
        this.employeeCin = '';
        this.fetchDocuments();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  generateDocument(): void {
    if (!this.isValidCin()) {
      this.errorMessage = 'CIN must be 8 digits';
      this.toastService.show('CIN must be 8 digits', 'error');
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    const documentName = this.newDocument.templateType === 'work_certificate' ? 'Work Certificate' : 'Leave Certificate';
    const parameters = {
      cin: this.newDocument.cin,
      leaveType: this.newDocument.templateType === 'leave_certificate' ? 'Annual Leave' : null,
      startDate: this.newDocument.templateType === 'leave_certificate' ? new Date().toISOString().split('T')[0] : null,
      endDate: this.newDocument.templateType === 'leave_certificate' ? new Date().toISOString().split('T')[0] : null
    };
    const accessibleBy = { type: 'employees', value: [this.newDocument.cin] };
    this.manageDocumentService.generateDocument(documentName, parameters, accessibleBy, this.newDocument.templateType).subscribe({
      next: (doc) => {
        this.toastService.show(`Generated "${doc.name}" successfully`, 'success');
        this.newDocument = { cin: '', templateType: 'work_certificate' };
        this.fetchDocuments();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
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

  reviewRequest(id: number | undefined, status: string): void {
    if(id == undefined){
      this.toastService.show('Request id not found', 'error');
    }
    else{
      this.isLoading = true;
      this.errorMessage = '';
      this.manageDocumentService.reviewRequest(id, status).subscribe({
        next: () => {
          this.toastService.show(`The document is ready info sent to employee successfully`, 'success');
          this.fetchPendingRequests();
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
