export interface Training {
  id: number;
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  capacity: number;
  enrolledCount: number;
  category : string ;
  rating : number ;
}

export interface TrainingEnrollment {
  id: number;
  training: Training;
  employeeEmail: string;
  status: string;
}
