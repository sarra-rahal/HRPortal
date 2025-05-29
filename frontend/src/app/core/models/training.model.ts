export interface Training {
  id: number;
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  capacity: number;
  enrolledCount: number;
  category: string;
  rating: number;
  hostCompany: string;
  type: 'INTERNAL' | 'EXTERNAL';
}

export interface TrainingEnrollment {
  id: number;
  training: Training;
  employeeEmail: string;
  status: string;
}

export interface TrainingProposal {
  id: number;
  title: string;
  description: string;
  category: string;
  proposedBy: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  hostCompany: string;
  type: 'INTERNAL' | 'EXTERNAL';
}
