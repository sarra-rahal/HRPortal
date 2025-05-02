import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { ToastService } from '../../core/services/toast.service';
import { ManageEmployeesService } from './manage-employees.service';
import { AuthService } from '../../core/services/auth.service';
import {Department, Employee} from '../../core/models/Employee-departments.model';


@Component({
  selector: 'app-manage-employees',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './manage-employees.component.html',
  styleUrls: ['./manage-employees.component.scss']
})
export class ManageEmployeesComponent implements OnInit {
  employees: Employee[] = [];
  filteredEmployees: Employee[] = [];
  departments: Department[] = [];
  newEmployee: Employee = {
    cin: '',
    firstname: '',
    lastname: '',
    email: '',
    hiredate: '',
    jobtitle: '',
    department: { id: 0, name: '' },
    salary: 0
  };
  newDepartment: Department = { id: 0, name: '' };
  editingEmployee: Employee | null = null;
  editingDepartment: Department | null = null;
  errorMessage: string = '';
  isLoading: boolean = false;
  currentView: 'add-employee' | 'list-employees' | 'manage-departments' = 'add-employee';
  searchTerm: string = '';
  departmentFilter: string = '';

  constructor(
    private manageEmployeesService: ManageEmployeesService,
    private toastService: ToastService,
    private authService: AuthService
  ) {}

  get firstname(): string | null {
    return this.authService.getFirstname();
  }

  get lastname(): string | null {
    return this.authService.getLastname();
  }

  ngOnInit(): void {
    this.fetchEmployees();
    this.fetchDepartments();
  }

  fetchEmployees(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.manageEmployeesService.getEmployees().subscribe({
      next: (employees) => {
        this.employees = employees;
        this.filteredEmployees = employees;
        this.isLoading = false;
        this.filterEmployees();
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

  isValidEmployee(): boolean {
    const cinRegex = /^[0-9]{8}$/;
    return cinRegex.test(this.newEmployee.cin) &&
      this.newEmployee.firstname.trim().length > 0 &&
      this.newEmployee.lastname.trim().length > 0 &&
      this.newEmployee.email.includes('@') &&
      !!this.newEmployee.hiredate.match(/^\d{4}-\d{2}-\d{2}$/) &&
      this.newEmployee.jobtitle.trim().length > 0 &&
      this.newEmployee.department.id > 0 &&
      this.newEmployee.salary > 0;
  }

  saveEmployee(): void {
    if (!this.isValidEmployee()) {
      this.errorMessage = 'Please enter valid employee details';
      this.toastService.show(this.errorMessage, 'error');
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';

    const operation = this.editingEmployee
      ? this.manageEmployeesService.updateEmployee(this.newEmployee)
      : this.manageEmployeesService.addEmployee(this.newEmployee);

    operation.subscribe({
      next: () => {
        this.toastService.show(`Employee ${this.editingEmployee ? 'updated' : 'added'} successfully`, 'success');
        this.newEmployee = {
          cin: '',
          firstname: '',
          lastname: '',
          email: '',
          hiredate: '',
          jobtitle: '',
          department: { id: 0, name: '' },
          salary: 0
        };
        this.editingEmployee = null;
        this.fetchEmployees();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  editEmployee(employee: Employee): void {
    this.newEmployee = { ...employee };
    this.editingEmployee = employee;
    this.currentView = 'add-employee';
  }

  cancelEdit(): void {
    this.newEmployee = {
      cin: '',
      firstname: '',
      lastname: '',
      email: '',
      hiredate: '',
      jobtitle: '',
      department: { id: 0, name: '' },
      salary: 0
    };
    this.editingEmployee = null;
  }

  deleteEmployee(cin: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.manageEmployeesService.deleteEmployee(cin).subscribe({
      next: () => {
        this.toastService.show('Employee deleted successfully', 'success');
        this.fetchEmployees();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  saveDepartment(): void {
    if (!this.newDepartment.name.trim()) {
      this.errorMessage = 'Please enter a department name';
      this.toastService.show(this.errorMessage, 'error');
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';

    const operation = this.editingDepartment
      ? this.manageEmployeesService.updateDepartment(this.newDepartment)
      : this.manageEmployeesService.addDepartment(this.newDepartment);

    operation.subscribe({
      next: () => {
        this.toastService.show(`Department ${this.editingDepartment ? 'updated' : 'added'} successfully`, 'success');
        this.newDepartment = { id: 0, name: '' };
        this.editingDepartment = null;
        this.fetchDepartments();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  editDepartment(department: Department): void {
    this.newDepartment = { ...department };
    this.editingDepartment = department;
  }

  cancelDepartmentEdit(): void {
    this.newDepartment = { id: 0, name: '' };
    this.editingDepartment = null;
  }

  deleteDepartment(id: number): void {
    if (!id) return;
    this.isLoading = true;
    this.errorMessage = '';
    this.manageEmployeesService.deleteDepartment(id).subscribe({
      next: () => {
        this.toastService.show('Department Deleted Successfully', 'success');
        this.fetchDepartments();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message;
        this.toastService.show(err.message, 'error');
        this.isLoading = false;
      }
    });
  }

  filterEmployees(): void {
    this.filteredEmployees = this.employees.filter(emp => {
      const matchesSearch = !this.searchTerm ||
        `${emp.firstname} ${emp.lastname}`.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        emp.cin.includes(this.searchTerm);
      const matchesDepartment = !this.departmentFilter || emp.department.name === this.departmentFilter;
      return matchesSearch && matchesDepartment;
    });
  }

  getDepartmentName(department: { id: number; name: string }): string {
    return department?.name || 'N/A';
  }

  getEmployeeCount(departmentName: string): number {
    return this.employees.filter(emp => emp.department.name === departmentName).length;
  }
}
