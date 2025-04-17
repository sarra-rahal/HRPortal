// src/app/document/document.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { Document } from '../../core/models/document.model';
import { saveAs } from 'file-saver';
import { ToastService } from '../../core/services/toast.service';
import { DocumentService } from './document.service';

@Component({
  selector: 'app-document',
  standalone: true,
  imports: [CommonModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './document.component.html',
  styleUrls: ['./document.component.scss'],
  providers: [ToastService]
})
export class DocumentComponent implements OnInit {
  documents: Document[] = [];
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private documentService: DocumentService,
    protected toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.fetchDocuments();
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
