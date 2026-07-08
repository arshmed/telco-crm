import { apiClient } from './client';

export interface OrderItemRequest {
  productCode: string;
  productType: 'TARIFF' | 'ADDON' | 'VAS';
  quantity: number;
}

export interface CreateOrderRequest {
  customerId: string;
  items: OrderItemRequest[];
}

export interface OrderResponse {
  id: string;
  customerId: string;
  status: string;
  totalAmount: number;
  currency: string;
  createdAt: string;
  // Diğer alanlar backend'e bağlı olarak eklenebilir
}

export const createOrder = async (data: CreateOrderRequest) => {
  const response = await apiClient.post<OrderResponse>('/orders', data);
  return response.data;
};
