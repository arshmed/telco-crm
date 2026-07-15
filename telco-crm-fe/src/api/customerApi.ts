import { apiClient } from './client';

export interface AddressResponse {
  id: string;
  line1: string;
  city: string;
  district: string;
  postalCode: string;
  isDefault: boolean;
}

export interface DocumentResponse {
  id: string;
  type: string;
  fileRef: string;
  verifiedAt: string;
}

export interface CustomerResponse {
  id: string;
  customerNo: string;
  type: 'INDIVIDUAL' | 'CORPORATE';
  firstName: string;
  lastName: string;
  identityNumber: string;
  dateOfBirth: string;
  email: string;
  phone: string;
  status: 'PENDING' | 'ACTIVE' | 'REJECTED' | 'CANCELLED';
  companyName?: string;
  taxOffice?: string;
  createdAt: string;
  updatedAt: string;
  addresses: AddressResponse[];
  documents: DocumentResponse[];
}

export interface CustomerRequest {
  type: 'INDIVIDUAL' | 'CORPORATE';
  firstName: string;
  lastName: string;
  identityNumber: string;
  dateOfBirth?: string;
  email: string;
  phone: string;
  companyName?: string;
  taxOffice?: string;
  addresses?: { line1: string; city: string; district: string; postalCode: string; isDefault: boolean }[];
}

export interface Page<T> {
  content: T[];
  pageable: { pageNumber: number; pageSize: number };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const getCustomers = async (page = 0, size = 20) => {
  const response = await apiClient.get<Page<CustomerResponse>>(`/api/v1/customers?page=${page}&size=${size}`);
  return response.data;
};

export const getCustomerById = async (id: string) => {
  const response = await apiClient.get<CustomerResponse>(`/api/v1/customers/${id}`);
  return response.data;
};

export const getCustomerByNo = async (customerNo: string) => {
  const response = await apiClient.get<CustomerResponse>(`/api/v1/customers/byNo/${customerNo}`);
  return response.data;
};

export const createCustomer = async (data: CustomerRequest) => {
  const response = await apiClient.post<CustomerResponse>('/api/v1/customers', data);
  return response.data;
};

export const updateCustomer = async (id: string, data: CustomerRequest) => {
  const response = await apiClient.put<CustomerResponse>(`/api/v1/customers/${id}`, data);
  return response.data;
};

export const deleteCustomer = async (id: string) => {
  await apiClient.delete(`/api/v1/customers/${id}`);
};

export const approveKyc = async (id: string) => {
  const response = await apiClient.post<CustomerResponse>(`/api/v1/customers/${id}/kyc/approve`);
  return response.data;
};

export const rejectKyc = async (id: string) => {
  const response = await apiClient.post<CustomerResponse>(`/api/v1/customers/${id}/kyc/reject`);
  return response.data;
};

export interface DocumentTypeOption {
  code: string;
  label: string;
}

export const getDocumentTypes = async (): Promise<DocumentTypeOption[]> => {
  const response = await apiClient.get<DocumentTypeOption[]>('/api/v1/document-types');
  return response.data;
};

export interface DocumentRequest {
  type: string;
  fileRef: string;
}

export const uploadDocument = async (customerId: string, data: DocumentRequest): Promise<DocumentResponse> => {
  const response = await apiClient.post<DocumentResponse>(`/api/v1/customers/${customerId}/documents`, data);
  return response.data;
};
