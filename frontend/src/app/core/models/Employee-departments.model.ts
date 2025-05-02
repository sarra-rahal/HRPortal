export interface Employee {
  cin: string;
  firstname: string;
  lastname: string;
  email: string;
  phonenumber?: string;
  hiredate: string;
  jobtitle: string;
  department: { id: number; name: string };
  salary: number;
  managernum?: string;
}

export interface Department {
  id: number;
  name: string;
}
