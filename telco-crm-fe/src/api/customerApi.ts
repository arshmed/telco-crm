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

export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const getCustomers = async (page = 0, size = 20) => {
  const response = await apiClient.get<Page<CustomerResponse>>(`/customers?page=${page}&size=${size}`);
  return response.data;
};

export const getCustomerById = async (id: string) => {
  const response = await apiClient.get<CustomerResponse>(`/customers/${id}`);
  return response.data;
};

export const createCustomer = async (data: any) => {
  const response = await apiClient.post<CustomerResponse>('/customers', data);
  return response.data;
};
