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
import { ManageEmployeesService } from '../manage-employees/manage-employees.service';
import { Employee as ModelEmployee } from '../../core/models/Employee-departments.model';

interface StringListDictionary {
  [key: string]: string[];
}

@Component({
  selector: 'app-manage-document',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './manage-document.component.html',
  styleUrls: ['./manage-document.component.scss']
})
export class ManageDocumentComponent implements OnInit {
  documents: Document[] = [];
  employees: ModelEmployee[] = [];
  access: StringListDictionary = { emp: [], dep: [], job: [], all: [] };
  Cin: string = '';
  filteredEmployees: ModelEmployee[] = [];
  showAutocomplete: boolean = false;
  filteredDocuments: Document[] = [];
  documentRequests: DocumentRequest[] = [];
  filteredRequests: DocumentRequest[] = [];
  fileToUpload: File | null = null;
  destinationType: 'employee' | 'department' | 'job' | 'all' = 'employee';
  selectedDepartment: string = '';
  selectedJob: string = '';
  departments: string[] = ['Engineering', 'QA', 'DevOps', 'Product Management', 'HR'];
  jobs: string[] = ['Software Engineer', 'Senior Software Engineer', 'QA Engineer', 'DevOps Engineer', 'Product Manager'];
  newDocument: {
    cin: string;
    templateType: string;
    fileName: string;
    trainingRequestId?: string;
    reviewId?: string;
  } = {
    cin: '',
    templateType: 'work_certificate',
    fileName: ''
  };
  templateTypes: string[] = [
    'work_certificate',
    'leave_certificate',
    'training_approval',
    'performance_review',
    'promotion_letter',
    'termination_notice'
  ];
  errorMessage: string = '';
  isLoading: boolean = false;
  currentView: 'upload' | 'generate' | 'list' | 'requests' = 'generate';
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
    this.fetchEmployees();
    this.fetchPendingRequests();
    this.subscribeToNotifications();
  }

  fetchEmployees(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.manageEmployeesService.getEmployees().subscribe({
      next: (employees) => {
        this.employees = employees;
        this.filteredEmployees = employees;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  filterCins(event: Event): void {
    const input = (event.target as HTMLInputElement).value.toLowerCase();
    this.filteredEmployees = this.employees.filter(emp =>
      emp.cin.toLowerCase().includes(input) ||
      emp.firstname.toLowerCase().includes(input) ||
      emp.lastname.toLowerCase().includes(input)
    );
    this.showAutocomplete = input.length > 0;
  }

  toggleAutocomplete(show: boolean): void {
    this.showAutocomplete = show && this.filteredEmployees.length > 0;
  }

  selectCin(cin: string, isUploadView: boolean = true): void {
    if (isUploadView) {
      const currentCins = this.Cin.split(';').map(c => c.trim()).filter(c => c);
      if (!currentCins.includes(cin)) {
        this.Cin = currentCins.length ? `${this.Cin};${cin}` : cin;
      }
      this.updateAccess();
    } else {
      this.newDocument.cin = cin;
    }
    this.filteredEmployees = [];
    this.showAutocomplete = false;
  }

  getEmployeeNamesForCins(): string {
    const cins = this.Cin.split(';').map(cin => cin.trim()).filter(cin => cin);
    const names = cins
      .map(cin => {
        const emp = this.employees.find(e => e.cin === cin);
        return emp ? `${emp.firstname} ${emp.lastname}` : null;
      })
      .filter(name => name);
    return names.length ? names.join(', ') : 'No valid employees selected';
  }

  onDestinationTypeChange(): void {
    this.Cin = '';
    this.selectedDepartment = '';
    this.selectedJob = '';
    this.access = { emp: [], dep: [], job: [], all: [] };
  }

  updateAccess(): void {
    this.access = { emp: [], dep: [], job: [], all: [] };
    switch (this.destinationType) {
      case 'employee':
        this.access['emp'] = this.Cin
          .split(';')
          .map(cin => cin.trim())
          .filter(cin => cin && /^[0-9]{8}$/.test(cin));
        break;
      case 'department':
        if (this.selectedDepartment) {
          this.access['dep'] = [this.selectedDepartment];
        }
        break;
      case 'job':
        if (this.selectedJob) {
          this.access['job'] = [this.selectedJob];
        }
        break;
      case 'all':
        this.access['all'] = ['all'];
        break;
    }
  }

  isUploadFormValid(): boolean {
    if (!this.fileToUpload) return false;
    switch (this.destinationType) {
      case 'employee':
        return this.isValidCins(this.Cin);
      case 'department':
        return !!this.selectedDepartment;
      case 'job':
        return !!this.selectedJob;
      case 'all':
        return true;
      default:
        return false;
    }
  }

  upload(): void {
    if (!this.fileToUpload) {
      this.errorMessage = 'Please select a file';
      this.toastService.show('Please select a file', 'error');
      return;
    }

    if (!this.isUploadFormValid()) {
      this.errorMessage = 'Please provide valid destination details';
      this.toastService.show('Please provide valid destination details', 'error');
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.manageDocumentService.upload(this.fileToUpload, this.access).subscribe({
      next: (doc) => {
        this.toastService.show(`Uploaded "${doc.name}" successfully`, 'success');
        this.fileToUpload = null;
        this.Cin = '';
        this.selectedDepartment = '';
        this.selectedJob = '';
        this.destinationType = 'employee';
        this.access = { emp: [], dep: [], job: [], all: [] };
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

  isValidCins(input: string): boolean {
    if (!input) return false;
    const cins = input.split(';').map(cin => cin.trim()).filter(cin => cin);
    return cins.length > 0 && cins.every(cin => /^[0-9]{8}$/.test(cin));
  }

  isValidSingleCin(cin: string): boolean {
    return /^[0-9]{8}$/.test(cin);
  }

  isFormValid(): boolean {
    const { templateType, cin, fileName, trainingRequestId, reviewId } = this.newDocument;
    switch (templateType) {
      case 'work_certificate':
      case 'leave_certificate':
      case 'promotion_letter':
      case 'termination_notice':
        return this.isValidSingleCin(cin) && !!fileName;
      case 'training_approval':
        return !!trainingRequestId && !!fileName;
      case 'performance_review':
        return !!reviewId && !!fileName;
      default:
        return false;
    }
  }

  onTemplateTypeChange(): void {
    this.newDocument = {
      cin: '',
      templateType: this.newDocument.templateType,
      fileName: '',
      trainingRequestId: undefined,
      reviewId: undefined
    };
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

  generateDocument(): void {
    if (!this.isFormValid()) {
      this.errorMessage = 'Please fill all required fields correctly';
      this.toastService.show('Please fill all required fields correctly', 'error');
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    const documentName = this.newDocument.templateType.replace('_', ' ').replace(/\b\w/g, char => char.toUpperCase());
    const parameters: any = {
      cin: this.newDocument.cin,
      fileName: this.newDocument.fileName
    };
    if (this.newDocument.templateType === 'training_approval') {
      parameters.trainingRequestId = this.newDocument.trainingRequestId;
    } else if (this.newDocument.templateType === 'performance_review') {
      parameters.reviewId = this.newDocument.reviewId;
    } else if (this.newDocument.templateType === 'leave_certificate') {
      parameters.leaveType = 'Annual Leave';
      parameters.startDate = new Date().toISOString().split('T')[0];
      parameters.endDate = new Date().toISOString().split('T')[0];
    }
    const accessibleBy = { type: 'employees', value: [this.newDocument.cin].filter(Boolean) };
    this.manageDocumentService.generateDocument(documentName, parameters, accessibleBy, this.newDocument.templateType).subscribe({
      next: (doc) => {
        this.toastService.show(`Generated "${doc.name}" successfully`, 'success');
        this.newDocument = { cin: '', templateType: 'work_certificate', fileName: '' };
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
      let matchesSearch = !this.searchTerm ||
        req.type.toLowerCase().includes(this.searchTerm.toLowerCase());
      if (req.description != undefined) {
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
    if (id == undefined) {
      this.toastService.show('Request id not found', 'error');
    } else {
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
