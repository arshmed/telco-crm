import { apiClient } from './client';

export interface TariffResponse {
  id: string;
  code: string;
  version: number;
  current: boolean;
  name: string;
  type: 'POSTPAID' | 'PREPAID';
  segment: string;
  monthlyFee: number;
  currency: string;
  minutesIncluded: number;
  smsIncluded: number;
  dataMbIncluded: number;
  status: string;
  effectiveFrom: string;
  effectiveTo: string;
  addons: AddonResponse[];
}

export interface AddonResponse {
  id: string;
  code: string;
  name: string;
  type: 'DATA' | 'SMS' | 'MINUTES' | 'VAS';
  price: number;
  currency: string;
  validityDays: number;
}

export interface Page<T> {
  content: T[];
  pageable: any;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const getTariffs = async (status?: string) => {
  const params = status && status !== 'all' ? `?status=${status}&size=50` : '?size=50';
  const response = await apiClient.get<Page<TariffResponse>>(`/api/v1/tariffs${params}`);
  return response.data;
};

export const getTariffByCode = async (code: string) => {
  const response = await apiClient.get<TariffResponse>(`/api/v1/tariffs/${code}`);
  return response.data;
};

export const createTariff = async (data: any) => {
  const response = await apiClient.post<TariffResponse>('/api/v1/tariffs', data);
  return response.data;
};

export const updateTariff = async (code: string, data: any) => {
  const response = await apiClient.put<TariffResponse>(`/api/v1/tariffs/${code}`, data);
  return response.data;
};

export const changeTariffPrice = async (code: string, monthlyFee: number) => {
  const response = await apiClient.patch<TariffResponse>(`/api/v1/tariffs/${code}/price`, { monthlyFee });
  return response.data;
};

export const publishTariff = async (code: string) => {
  const response = await apiClient.post<TariffResponse>(`/api/v1/tariffs/${code}/publish`);
  return response.data;
};

export const deleteTariff = async (code: string) => {
  await apiClient.delete(`/api/v1/tariffs/${code}`);
};

export const getAddons = async (tariffCode?: string) => {
  const params = tariffCode ? `?tariffCode=${tariffCode}` : '';
  const response = await apiClient.get<AddonResponse[]>(`/api/v1/addons${params}`);
  return response.data;
};

export const getAddonByCode = async (code: string) => {
  const response = await apiClient.get<AddonResponse>(`/api/v1/addons/${code}`);
  return response.data;
};

export const createAddon = async (data: any) => {
  const response = await apiClient.post<AddonResponse>('/api/v1/addons', data);
  return response.data;
};

export const updateAddon = async (code: string, data: any) => {
  const response = await apiClient.put<AddonResponse>(`/api/v1/addons/${code}`, data);
  return response.data;
};

export const deleteAddon = async (code: string) => {
  await apiClient.delete(`/api/v1/addons/${code}`);
};
