export interface Document {
  id: number;
  name: string;
  uploadedBy: string;
  accessibleBy: string;
  uploadDate: string;
}

export interface DocumentRequest {
  id?: number;
  type: 'work_certificate' | 'leave_certificate' | 'other';
  description?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  requestDate: string;
  urgency: boolean;
  employeeEmail: string;
  startLDate?: string;
  endLDater?: string;
  duration?:number;
}
