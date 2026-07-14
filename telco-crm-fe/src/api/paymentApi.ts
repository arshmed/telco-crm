import { apiClient } from './client';

export interface Page<T> {
  content: T[];
  pageable: any;
  last: boolean;
  totalPages: number;
  totalElements: number;
  first: boolean;
  size: number;
  number: number;
  sort: any;
  numberOfElements: number;
  empty: boolean;
}

export interface PaymentResponse {
  id: string;
  orderId: string;
  customerId: string;
  amount: number;
  currency: string;
  method: string;
  status: string;
  externalRef: string;
  createdAt: string;
  updatedAt: string;
}

export interface RefundRequest {
  reason: string;
}

export const getPayments = async (page: number = 0, size: number = 20): Promise<Page<PaymentResponse>> => {
  const { data } = await apiClient.get<Page<PaymentResponse>>(`/payments?page=${page}&size=${size}`);
  return data;
};

export const getPaymentById = async (id: string): Promise<PaymentResponse> => {
  const { data } = await apiClient.get<PaymentResponse>(`/payments/${id}`);
  return data;
};

export const refundPayment = async (id: string, request: RefundRequest): Promise<PaymentResponse> => {
  const { data } = await apiClient.post<PaymentResponse>(`/payments/${id}/refund`, request);
  return data;
};
