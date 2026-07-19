import { apiClient } from './client';

export interface InvoiceLineResponse {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface InvoiceResponse {
  id: string;
  customerId: string;
  customerNo: string | null;
  subscriptionId: string;
  invoiceNumber: string;
  periodStart: string;
  periodEnd: string;
  subTotal: number;
  taxRate: number;
  taxAmount: number;
  grandTotal: number;
  status: 'DRAFT' | 'ISSUED' | 'PAID' | 'OVERDUE' | 'CANCELLED';
  dueDate: string;
  issuedAt: string | null;
  paidAt: string | null;
  lines: InvoiceLineResponse[];
  createdAt: string;
}

export interface BillCycleResponse {
  id: string;
  customerId: string;
  subscriptionId: string;
  tariffCode: string;
  dayOfMonth: number;
  nextRunDate: string;
  active: boolean;
}

export interface BillRunResponse {
  generated: number;
  asOf: string;
  status: string;
}

export interface InvoiceStats {
  overdueCount: number;
  paidCount: number;
  issuedCount: number;
  totalCount: number;
  totalRevenue: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const billingApi = {
  getInvoices: async (customerId?: string, page = 0, size = 20): Promise<PageResponse<InvoiceResponse>> => {
    const params: Record<string, string | number> = { page, size };
    if (customerId) params.customerId = customerId;
    const { data } = await apiClient.get(`/api/v1/invoices`, { params });
    return data;
  },

  getInvoice: async (id: string): Promise<InvoiceResponse> => {
    const { data } = await apiClient.get(`/api/v1/invoices/${id}`);
    return data;
  },

  getBillCycles: async (customerId: string): Promise<BillCycleResponse[]> => {
    const { data } = await apiClient.get(`/api/v1/billing/cycles`, {
      params: { customerId },
    });
    return data;
  },

  triggerBillRun: async (asOf?: string): Promise<BillRunResponse> => {
    const params: Record<string, string> = {};
    if (asOf) params.asOf = asOf;
    const { data } = await apiClient.post(`/api/v1/billing/runs`, null, { params });
    return data;
  },

  getInvoiceStats: async (): Promise<InvoiceStats> => {
    const { data } = await apiClient.get<InvoiceStats>('/api/v1/invoices/stats');
    return data;
  },

  downloadInvoicePdf: async (id: string, invoiceNumber: string): Promise<void> => {
    const response = await apiClient.get(`/api/v1/invoices/${id}/pdf`, {
      responseType: 'blob',
    });
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${invoiceNumber}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};
