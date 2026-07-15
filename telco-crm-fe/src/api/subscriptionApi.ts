import { apiClient } from './client';

export type SubscriptionStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'TERMINATED';

export interface SubscriptionResponse {
  id: string;
  customerId: string;
  customerNo: string;
  msisdn: string;
  tariffCode: string;
  status: SubscriptionStatus;
  activatedAt: string | null;
  suspendedAt: string | null;
  terminatedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSubscriptionRequest {
  customerId: string;
  tariffCode: string;
  msisdn?: string;
}

export interface Page<T> {
  content: T[];
  pageable: { pageNumber: number; pageSize: number };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const getSubscriptions = async (page = 0, size = 20): Promise<Page<SubscriptionResponse>> => {
  const response = await apiClient.get<Page<SubscriptionResponse>>(`/api/v1/subscriptions?page=${page}&size=${size}`);
  return response.data;
};

export const getSubscriptionById = async (id: string): Promise<SubscriptionResponse> => {
  const response = await apiClient.get<SubscriptionResponse>(`/api/v1/subscriptions/${id}`);
  return response.data;
};

export const getSubscriptionsByCustomer = async (customerId: string, page = 0, size = 20): Promise<Page<SubscriptionResponse>> => {
  const response = await apiClient.get<Page<SubscriptionResponse>>(`/api/v1/subscriptions/customer/${customerId}?page=${page}&size=${size}`);
  return response.data;
};

export const getSubscriptionsByStatus = async (status: SubscriptionStatus, page = 0, size = 20): Promise<Page<SubscriptionResponse>> => {
  const response = await apiClient.get<Page<SubscriptionResponse>>(`/api/v1/subscriptions/status/${status}?page=${page}&size=${size}`);
  return response.data;
};

export const createSubscription = async (data: CreateSubscriptionRequest): Promise<SubscriptionResponse> => {
  const response = await apiClient.post<SubscriptionResponse>('/api/v1/subscriptions', data);
  return response.data;
};

export const activateSubscription = async (id: string): Promise<SubscriptionResponse> => {
  const response = await apiClient.post<SubscriptionResponse>(`/api/v1/subscriptions/${id}/activate`);
  return response.data;
};

export const suspendSubscription = async (id: string): Promise<SubscriptionResponse> => {
  const response = await apiClient.post<SubscriptionResponse>(`/api/v1/subscriptions/${id}/suspend`);
  return response.data;
};

export const reactivateSubscription = async (id: string): Promise<SubscriptionResponse> => {
  const response = await apiClient.post<SubscriptionResponse>(`/api/v1/subscriptions/${id}/reactivate`);
  return response.data;
};

export const terminateSubscription = async (id: string): Promise<SubscriptionResponse> => {
  const response = await apiClient.post<SubscriptionResponse>(`/api/v1/subscriptions/${id}/terminate`);
  return response.data;
};

export interface SubscriptionAddonResponse {
  id: string;
  addonCode: string;
  addedAt: string;
}

export const changeTariff = async (id: string, tariffCode: string): Promise<SubscriptionResponse> => {
  const response = await apiClient.patch<SubscriptionResponse>(`/api/v1/subscriptions/${id}/tariff`, { tariffCode });
  return response.data;
};

export const addAddonToSubscription = async (id: string, addonCode: string): Promise<SubscriptionAddonResponse> => {
  const response = await apiClient.post<SubscriptionAddonResponse>(`/api/v1/subscriptions/${id}/addons`, { addonCode });
  return response.data;
};

export const getSubscriptionAddons = async (id: string): Promise<SubscriptionAddonResponse[]> => {
  const response = await apiClient.get<SubscriptionAddonResponse[]>(`/api/v1/subscriptions/${id}/addons`);
  return response.data;
};

export interface SubscriptionStats {
  active: number;
  suspended: number;
  pending: number;
  terminated: number;
  total: number;
}

export interface MonthlyActivation {
  month: string;
  count: number;
}

export interface TariffDistribution {
  tariffCode: string;
  count: number;
}

export const getSubscriptionStats = async (): Promise<SubscriptionStats> => {
  const response = await apiClient.get<SubscriptionStats>('/api/v1/subscriptions/stats');
  return response.data;
};

export const getMonthlyActivations = async (months: number = 6): Promise<MonthlyActivation[]> => {
  const response = await apiClient.get<MonthlyActivation[]>(`/api/v1/subscriptions/stats/monthly-activations?months=${months}`);
  return response.data;
};

export const getTariffDistribution = async (): Promise<TariffDistribution[]> => {
  const response = await apiClient.get<TariffDistribution[]>('/api/v1/subscriptions/stats/by-tariff');
  return response.data;
};
