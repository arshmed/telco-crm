import { apiClient } from './client';

export interface QuotaResponse {
  subscriptionId: string;
  msisdn: string;
  tariffCode: string;
  periodStart: string;
  periodEnd: string;
  minutesIncluded: number;
  minutesUsed: number;
  minutesRemaining: number;
  smsIncluded: number;
  smsUsed: number;
  smsRemaining: number;
  dataMbIncluded: number;
  dataMbUsed: number;
  dataMbRemaining: number;
}

export interface UsageRecordResponse {
  id: string;
  type: string;
  quantity: number;
  recordedAt: string;
  cdrRef: string;
}

export interface Page<T> {
  content: T[];
  pageable: any;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const getQuota = async (subscriptionId: string) => {
  const response = await apiClient.get<QuotaResponse>(`/usage/subscriptions/${subscriptionId}/quota`);
  return response.data;
};

export const getUsageHistory = async (subscriptionId: string, page = 0, size = 20) => {
  const response = await apiClient.get<Page<UsageRecordResponse>>(`/usage/subscriptions/${subscriptionId}/history?page=${page}&size=${size}`);
  return response.data;
};
