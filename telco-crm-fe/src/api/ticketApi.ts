import { apiClient } from './client';

export type TicketCategory = 'COMPLAINT' | 'REQUEST' | 'FAULT';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TicketStatus = 'ASSIGNED' | 'RESOLVED';

export interface TicketSummary {
  id: string;
  customerId: string;
  customerName: string;
  category: TicketCategory;
  priority: TicketPriority;
  status: TicketStatus;
  description: string;
  assignedTeam: string;
  slaDueAt: string;
  createdAt: string;
}

export interface TicketComment {
  id: string;
  authorId: string;
  body: string;
  createdAt: string;
}

export interface TicketDetailResponse {
  id: string;
  customerId: string;
  category: TicketCategory;
  priority: TicketPriority;
  status: TicketStatus;
  description: string;
  assignedTeam: string;
  slaDueAt: string;
  resolution: string | null;
  resolvedAt: string | null;
  comments: TicketComment[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateTicketPayload {
  customerId: string;
  category: TicketCategory;
  priority: TicketPriority;
  description: string;
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

export const getTicketById = async (ticketId: string) => {
  const response = await apiClient.get<TicketDetailResponse>(`/api/v1/tickets/${ticketId}`);
  return response.data;
};

export const createTicket = async (payload: CreateTicketPayload) => {
  const response = await apiClient.post<TicketDetailResponse>('/api/v1/tickets', payload);
  return response.data;
};

export const addComment = async (ticketId: string, body: string) => {
  const response = await apiClient.post<TicketComment>(`/api/v1/tickets/${ticketId}/comments`, { body });
  return response.data;
};

export const assignTicket = async (ticketId: string, assignedTeam: string) => {
  const response = await apiClient.post<TicketDetailResponse>(`/api/v1/tickets/${ticketId}/assign`, { assignedTeam });
  return response.data;
};

export const resolveTicket = async (ticketId: string, resolution: string) => {
  const response = await apiClient.post<TicketDetailResponse>(`/api/v1/tickets/${ticketId}/resolve`, { resolution });
  return response.data;
};
