import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NavbarComponent } from '../../layout/navbar/navbar.component';
import { SidebarComponent } from '../../layout/sidebar/sidebar.component';
import { FooterComponent } from '../../layout/footer/footer.component';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from './profile.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent, SidebarComponent, FooterComponent],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  profile: any = null;
  profileForm: FormGroup;
  error: string | null = null;
  isLoading: boolean = false;
  isEditing: boolean = false;

  constructor(
    private authService: AuthService,
    private profileService: ProfileService,
    private fb: FormBuilder,
    private toastService: ToastService
  ) {
    this.profileForm = this.fb.group({
      cin: [{ value: '', disabled: true }],
      firstname: ['', [Validators.required, Validators.minLength(2)]],
      lastname: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phonenumber: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
      address: ['', [Validators.required, Validators.minLength(5)]],
      certifications: [''],
      department: [{ value: '', disabled: true }],
      jobtitle: [{ value: '', disabled: true }],
      dateOfBirth: [{ value: '', disabled: true }],
      gender: [{ value: '', disabled: true }],
      maritalStatus: [{ value: '', disabled: true }],
      hiredate: [{ value: '', disabled: true }],
      employeeStatus: [{ value: '', disabled: true }],
      employmentType: [{ value: '', disabled: true }],
      salary: [{ value: '', disabled: true }],
      personalLeaveBalance: [{ value: '', disabled: true }],
      sickLeaveBalance: [{ value: '', disabled: true }],
      unpaidLeaveBalance: [{ value: '', disabled: true }]
    });
  }

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.loadProfile();
    } else {
      this.error = 'User not authenticated';
      this.toastService.show(this.error, 'error');
    }
  }

  loadProfile(): void {
    this.isLoading = true;
    this.error = null;
    this.profileService.getEmployeeProfile().subscribe({
      next: (data) => {
        this.profile = data;
        this.profileForm.patchValue({
          cin: data.cin || 'N/A',
          firstname: data.firstname || 'N/A',
          lastname: data.lastname || 'N/A',
          email: data.email || 'N/A',
          phonenumber: data.phonenumber || 'N/A',
          address: data.address || 'N/A',
          certifications: data.certifications || '',
          department: data.department?.name || 'N/A',
          jobtitle: data.jobtitle || 'N/A',
          dateOfBirth: data.dateOfBirth || 'N/A',
          gender: data.gender || 'N/A',
          maritalStatus: data.maritalStatus || 'N/A',
          hiredate: data.hiredate || 'N/A',
          employeeStatus: data.employeeStatus || 'N/A',
          employmentType: data.employmentType || 'N/A',
          salary: data.salary ? data.salary.toString() : 'N/A',
          personalLeaveBalance: data.personalLeaveBalance != null ? data.personalLeaveBalance : 'N/A',
          sickLeaveBalance: data.sickLeaveBalance != null ? data.sickLeaveBalance : 'N/A',
          unpaidLeaveBalance: data.unpaidLeaveBalance != null ? data.unpaidLeaveBalance : 'N/A'
        });
        this.isLoading = false;
      },
      error: (err) => {
        this.error = 'Failed to load profile: ' + err.message;
        this.toastService.show(this.error, 'error');
        this.isLoading = false;
      }
    });
  }

  get role(): string | null {
    return this.authService.getRole();
  }

  enableEdit(): void {
    this.isEditing = true;
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.loadProfile();
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      this.toastService.show('Please fill all required fields correctly', 'error');
      return;
    }

    this.isLoading = true;
    this.error = null;

    const updatedProfile = {
      ...this.profile,
      firstname: this.profileForm.get('firstname')?.value,
      lastname: this.profileForm.get('lastname')?.value,
      email: this.profileForm.get('email')?.value,
      phonenumber: this.profileForm.get('phonenumber')?.value,
      address: this.profileForm.get('address')?.value,
      certifications: this.profileForm.get('certifications')?.value || ''
    };

    console.log('Sending POST /api/profile with payload:', JSON.stringify(updatedProfile, null, 2));

    this.profileService.updateEmployeeProfile(updatedProfile).subscribe({
      next: (data) => {
        this.profile = data;
        this.isEditing = false;
        this.isLoading = false;
        this.toastService.show('Profile updated successfully', 'success');
      },
      error: (err) => {
        console.error('Update profile error:', err);
        this.error = `Failed to update profile: ${err.message} (Status: ${err.status})`;
        this.toastService.show(this.error, 'error');
        this.isLoading = false;
      }
    });
  }
}
