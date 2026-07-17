import { apiClient } from './client';

export interface TicketSummary {
  id: string;
  customerId: string;
  customerName: string;
  category: 'COMPLAINT' | 'REQUEST' | 'FAULT';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  status: 'ASSIGNED' | 'RESOLVED';
  description: string;
  assignedTeam: string;
  slaDueAt: string;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const getTickets = async (status?: string, page = 0, size = 20) => {
  const statusParam = status ? `&status=${status}` : '';
  const response = await apiClient.get<Page<TicketSummary>>(
    `/api/v1/tickets?page=${page}&size=${size}&sort=createdAt,desc${statusParam}`
  );
  return response.data;
};
