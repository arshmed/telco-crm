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

export const getTariffs = async () => {
  const response = await apiClient.get<Page<TariffResponse>>(`/tariffs?size=50`);
  return response.data;
};

export const getAddonsByTariff = async (tariffCode: string) => {
  const response = await apiClient.get<AddonResponse[]>(`/addons?tariffCode=${tariffCode}`);
  return response.data;
};

export const getAllAddons = async () => {
  const response = await apiClient.get<AddonResponse[]>(`/addons`);
  return response.data;
};
