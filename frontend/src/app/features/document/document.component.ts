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
  startDate: string = '';
  endDate: string = '';
  newRequest: DocumentRequest = {
    id: 0,
    type: 'work_certificate',
    description: '',
    status: 'PENDING',
    requestDate: '',
    urgency: false
  };
  requestTypes: string[] = ['work_certificate', 'leave_certificate', 'other'];

  // Mock data
  private mockDocuments: Document[] = [
    { id: 1, name: 'Employee Handbook.pdf', uploadedBy: 'admin@example.com', accessibleBy: JSON.stringify({ type: 'all_departments', value: null }), uploadDate: '2025-04-01' },
    { id: 2, name: 'Training Certificate.pdf', uploadedBy: 'hr@example.com', accessibleBy: JSON.stringify({ type: 'department', value: 'HR' }), uploadDate: '2025-04-10' },
    { id: 3, name: 'Leave Policy.docx', uploadedBy: 'manager@example.com', accessibleBy: JSON.stringify({ type: 'employees', value: ['E123', 'E456'] }), uploadDate: '2025-04-15' }
  ];
  private mockRequests: DocumentRequest[] = [
    { id: 1, type: 'work_certificate', description: 'Need for visa application', status: 'PENDING', requestDate: '2025-04-16', urgency: true },
    { id: 2, type: 'leave_certificate', description: 'Proof of leave for insurance', status: 'APPROVED', requestDate: '2025-04-14', urgency: false },
    { id: 3, type: 'other', description: 'Custom document for audit', status: 'REJECTED', requestDate: '2025-04-13', urgency: false }
  ];

  constructor(
    private documentService: DocumentService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.documents = this.mockDocuments;
    this.filteredDocuments = this.documents;
    this.documentRequests = this.mockRequests;
    this.filteredRequests = this.documentRequests;
    // Uncomment when backend is ready
    // this.fetchDocuments();
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

  downloadDocument(id: number, name: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    // Mock download
    const blob = new Blob(['Mock document content'], { type: 'text/plain' });
    saveAs(blob, name);
    this.isLoading = false;
    // Uncomment when backend is ready
    /*
    this.documentService.downloadDocument(id).subscribe({
      next: (blob) => {
        saveAs(blob, name);
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
    */
  }

  filterDocuments(): void {
    this.filteredDocuments = this.documents.filter(doc => {
      const matchesSearch = !this.searchTerm ||
        doc.name.toLowerCase().includes(this.searchTerm.toLowerCase());
      const uploadDate = new Date(doc.uploadDate);
      const start = this.startDate ? new Date(this.startDate) : null;
      const end = this.endDate ? new Date(this.endDate) : null;
      const matchesDate = (!start || uploadDate >= start) && (!end || uploadDate <= end);
      return matchesSearch && matchesDate;
    });
  }

  filterRequests(): void {
    this.filteredRequests = this.documentRequests.filter(req => {
      const matchesSearch = !this.searchTerm ||
        req.type.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        req.description.toLowerCase().includes(this.searchTerm.toLowerCase());
      const requestDate = new Date(req.requestDate);
      const start = this.startDate ? new Date(this.startDate) : null;
      const end = this.endDate ? new Date(this.endDate) : null;
      const matchesDate = (!start || requestDate >= start) && (!end || requestDate <= end);
      return matchesSearch && matchesDate;
    });
  }

  requestDocument(): void {
    if (!this.newRequest.description.trim()) {
      this.toastService.show('Description is required', 'error');
      return;
    }
    this.isLoading = true;
    // Mock request
    const newId = this.documentRequests.length + 1;
    const request: DocumentRequest = {
      ...this.newRequest,
      id: newId,
      requestDate: new Date().toISOString(),
      status: 'PENDING'
    };
    this.documentRequests.push(request);
    this.filteredRequests = this.documentRequests;
    this.filterRequests();
    this.toastService.show('Document request submitted successfully', 'success');
    this.newRequest = { id: 0, type: 'work_certificate', description: '', status: 'PENDING', requestDate: '', urgency: false };
    this.isLoading = false;
  }

  cancelRequest(id: number): void {
    if (confirm('Are you sure you want to cancel this request?')) {
      this.isLoading = true;
      // Mock cancel
      this.documentRequests = this.documentRequests.filter(req => req.id !== id);
      this.filteredRequests = this.documentRequests;
      this.filterRequests();
      this.toastService.show('Request cancelled successfully', 'success');
      this.isLoading = false;
    }
  }

  formatAccessibleBy(accessibleBy: string): string {
    try {
      const json = JSON.parse(accessibleBy);
      if (json.type === 'all_departments') return 'All Departments';
      if (json.type === 'department') return `Department: ${json.value}`;
      if (json.type === 'employees') return `Employees: ${json.value.join(', ')}`;
      return 'Unknown';
    } catch {
      return accessibleBy;
    }
  }
}
