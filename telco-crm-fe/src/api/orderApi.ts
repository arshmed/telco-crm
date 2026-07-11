import { apiClient } from './client';

export interface OrderItemRequest {
  productCode: string;
  productType: 'TARIFF' | 'ADDON' | 'VAS';
  quantity: number;
}

export interface CreateOrderRequest {
  customerId?: string;
  customerNo?: string;
  items: OrderItemRequest[];
}

export interface OrderItemResponse {
  id: string;
  productCode: string;
  productName: string;
  productType: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface SagaStateResponse {
  currentStep: string;
  retryCount: number;
  errorMessage: string | null;
  lastUpdated: string;
}

export interface OrderResponse {
  id: string;
  customerId: string;
  customerNo: string | null;
  status: string;
  totalAmount: number;
  currency: string;
  paymentId: string | null;
  subscriptionId: string | null;
  cancellationReason: string | null;
  items: OrderItemResponse[];
  sagaState: SagaStateResponse | null;
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  pageable: { pageNumber: number; pageSize: number };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const createOrder = async (data: CreateOrderRequest) => {
  const response = await apiClient.post<OrderResponse>('/orders', data);
  return response.data;
};

export const listOrders = async (page = 0, size = 20, customerId?: string) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (customerId) params.append('customerId', customerId);
  const response = await apiClient.get<Page<OrderResponse>>(`/orders?${params}`);
  return response.data;
};

export const getOrderById = async (orderId: string) => {
  const response = await apiClient.get<OrderResponse>(`/orders/${orderId}`);
  return response.data;
};

export const cancelOrder = async (orderId: string, reason: string) => {
  const response = await apiClient.post<OrderResponse>(`/orders/${orderId}/cancel`, { reason });
  return response.data;
};
