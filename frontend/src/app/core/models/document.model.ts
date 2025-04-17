export interface Document {
  id: number;
  name: string;
  filePath: string;
  uploadedBy: string;
  uploadDate: string;
  generated: boolean;
  accessibleBy: string;
}
