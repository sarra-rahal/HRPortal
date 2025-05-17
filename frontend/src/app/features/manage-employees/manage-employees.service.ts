import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Department, Employee } from '../../core/models/Employee-departments.model';

@Injectable({
  providedIn: 'root'
})
export class ManageEmployeesService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    if (!token) {
      console.error('No token found in localStorage');
      throw new Error('No authentication token');
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token.trim()}`
    });
  }

  getEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.apiUrl}/employees`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(employees => console.log('Fetched employees:', employees)),
        catchError(this.handleError)
      );
  }

  searchEmployeesByCin(cin: string): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.apiUrl}/employees/search?cin=${cin}`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(suggestions => console.log('Fetched manager suggestions:', suggestions)),
        catchError(this.handleError)
      );
  }

  addEmployee(employee: Employee): Observable<Employee> {
    return this.http.post<Employee>(`${this.apiUrl}/employees`, employee, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log('Employee added successfully')),
        catchError(this.handleError)
      );
  }

  updateEmployee(employee: Employee): Observable<Employee> {
    return this.http.put<Employee>(`${this.apiUrl}/employees/${employee.cin}`, employee, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log('Employee updated successfully')),
        catchError(this.handleError)
      );
  }

  deleteEmployee(cin: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/employees/${cin}`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log('Employee deleted successfully')),
        catchError(this.handleError)
      );
  }

  getDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>(`${this.apiUrl}/departments`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(departments => console.log('Fetched departments:', departments)),
        catchError(this.handleError)
      );
  }

  addDepartment(department: Department): Observable<Department> {
    return this.http.post<Department>(`${this.apiUrl}/departments`, department, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log('Department added successfully')),
        catchError(this.handleError)
      );
  }

  updateDepartment(department: Department): Observable<Department> {
    return this.http.put<Department>(`${this.apiUrl}/departments/${department.id}`, department, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log('Department updated successfully')),
        catchError(this.handleError)
      );
  }

  deleteDepartment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/departments/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => console.log('Department deleted successfully')),
        catchError(this.handleError)
      );
  }

  private handleError(error: any): Observable<never> {
    let errorMessage = 'An unexpected error occurred';
    if (error.error instanceof ErrorEvent) {
      errorMessage = error.error.message;
    } else {
      errorMessage = error.error?.message || error.message || `Error ${error.status}`;
      if (error.status === 401) {
        errorMessage = 'Authentication failed, please sign in again';
      }
      if (error.error?.text) {
        console.error('Raw response text:', error.error.text);
      }
    }
    console.error('ManageEmployees API Error:', error);
    return throwError(() => new Error(errorMessage));
  }
}
