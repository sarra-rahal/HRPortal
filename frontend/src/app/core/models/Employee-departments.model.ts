export interface Employee {
  cin: string;
  firstname: string;
  lastname: string;
  dateOfBirth?: string; // ISO date string (e.g., "1990-05-15")
  gender: string; // "Male" or "Female"
  nationality: string;
  maritalStatus: string;
  email: string;
  phonenumber: string;
  address: string;
  hiredate: string; // ISO date string (e.g., "2023-01-15")
  employeeStatus: string; // "Active" or "On Leave"
  employmentType: string; // "Full-Time", "Part-Time", "Contractor"
  department: Department;
  jobtitle: string;
  salary: number;
  managercin?: string; // Optional, maps to managernum
  certifications: string[]; // Array of strings
  personalLeaveBalance: number;
  sickLeaveBalance: number;
  unpaidLeaveBalance: number;
}

export interface Department {
  id: number;
  name: string;
}
