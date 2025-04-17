import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { Document } from '../../core/models/document.model';
import { saveAs } from 'file-saver';
import { ToastService } from '../../core/services/toast.service';
import { DocumentService } from '../document/document.service';
import { ManageDocumentService } from './manage-document.service';

@Component({
  selector: 'app-manage-document',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './manage-document.component.html',
  styleUrls: ['./manage-document.component.scss'],
  providers: [ToastService]
})
export class ManageDocumentComponent implements OnInit {
  documents: Document[] = [];
  fileToUpload: File | null = null;
  accessibleByType: 'department' | 'employees' | 'all_departments' = 'all_departments';
  departmentValue: string = '';
  employeeCins: string[] = [];
  departments: string[] = [];
  employees: { cin: string, name: string }[] = [];
  newDocument: { name: string; content: string; templateType: string } = { name: '', content: '', templateType: 'work_certificate' };
  templateTypes: string[] = ['work_certificate', 'leave_certificate'];
  errorMessage: string = '';
  isLoading: boolean = false;
  currentView: 'upload' | 'generate' | 'list' = 'upload';

  constructor(
    private documentService: DocumentService,
    private manageDocumentService: ManageDocumentService,
    protected toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.fetchDocuments();
    this.fetchDepartments();
    this.fetchEmployees('');
  }

  fetchDocuments(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.documentService.getAllDocuments().subscribe({
      next: (docs) => {
        this.documents = docs;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  fetchDepartments(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.manageDocumentService.getDepartments().subscribe({
      next: (depts) => {
        this.departments = depts;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show('Failed to fetch departments', 'error');
        this.isLoading = false;
      }
    });
  }

  fetchEmployees(department: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.manageDocumentService.getEmployees(department).subscribe({
      next: (emps) => {
        this.employees = emps;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show('Failed to fetch employees', 'error');
        this.isLoading = false;
      }
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fileToUpload = input.files?.[0] || null;
  }

  uploadDocument(): void {
    if (!this.fileToUpload) {
      this.errorMessage = 'Please select a file to upload';
      this.toastService.show('Please select a file to upload', 'error');
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    const accessibleBy = this.buildAccessibleBy();
    this.manageDocumentService.uploadDocument(this.fileToUpload, accessibleBy).subscribe({
      next: (doc) => {
        this.toastService.show(`Uploaded "${doc.name}" successfully`, 'success');
        this.fileToUpload = null;
        this.accessibleByType = 'all_departments';
        this.departmentValue = '';
        this.employeeCins = [];
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
    if (!this.newDocument.name.trim() || !this.newDocument.content.trim()) {
      this.errorMessage = 'Document name and content are required';
      this.toastService.show('Document name and content are required', 'error');
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    const accessibleBy = this.buildAccessibleBy();
    this.manageDocumentService.generateDocument(
      this.newDocument.name,
      this.newDocument.content,
      accessibleBy,
      this.newDocument.templateType
    ).subscribe({
      next: (doc) => {
        this.toastService.show(`Generated "${doc.name}" successfully`, 'success');
        this.newDocument = { name: '', content: '', templateType: 'work_certificate' };
        this.accessibleByType = 'all_departments';
        this.departmentValue = '';
        this.employeeCins = [];
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
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  onAccessibleByTypeChange(): void {
    this.departmentValue = '';
    this.employeeCins = [];
    if (this.accessibleByType === 'department') {
      this.fetchEmployees(this.departmentValue);
    } else if (this.accessibleByType === 'employees') {
      this.fetchEmployees('');
    }
  }

  private buildAccessibleBy(): any {
    if (this.accessibleByType === 'all_departments') {
      return { type: 'all_departments', value: null };
    } else if (this.accessibleByType === 'department') {
      return { type: 'department', value: this.departmentValue };
    } else {
      return { type: 'employees', value: this.employeeCins };
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
