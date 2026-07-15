import { apiClient } from './client';

export interface NotificationResponse {
  id: string;
  userId: string;
  templateCode: string;
  channel: 'SMS' | 'EMAIL' | 'PUSH';
  subject: string;
  body: string;
  status: 'PENDING' | 'SENT' | 'FAILED';
  sentAt: string;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  pageable: any;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const getUserNotificationHistory = async (userId: string, page = 0, size = 10) => {
  const response = await apiClient.get<Page<NotificationResponse>>(`/api/v1/notifications/users/${userId}/history?page=${page}&size=${size}`);
  return response.data;
};

export interface NotificationRequest {
  userId: string;
  templateCode: string;
  channel: 'SMS' | 'EMAIL' | 'PUSH';
  payloadJson?: string;
}

export const sendNotification = async (data: NotificationRequest) => {
  const response = await apiClient.post<NotificationResponse>('/api/v1/notifications', data);
  return response.data;
};

export const getRecentNotifications = async (): Promise<NotificationResponse[]> => {
  const response = await apiClient.get<NotificationResponse[]>('/api/v1/notifications/recent');
  return response.data;
};
