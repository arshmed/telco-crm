export const ROLES = {
  CUSTOMER: 'CUSTOMER',
  CALL_CENTER_AGENT: 'CALL_CENTER_AGENT',
  FIELD_DEALER: 'FIELD_DEALER',
  MARKETING_MANAGER: 'MARKETING_MANAGER',
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  BILLING_OPERATOR: 'BILLING_OPERATOR',
  SYSTEM_SERVICE: 'SYSTEM_SERVICE',
} as const;

export type Role = (typeof ROLES)[keyof typeof ROLES];

export const ROLE_LABELS: Record<string, string> = {
  CUSTOMER: 'Müşteri',
  CALL_CENTER_AGENT: 'Çağrı Merkezi Temsilcisi',
  FIELD_DEALER: 'Saha Bayii',
  MARKETING_MANAGER: 'Pazarlama Yöneticisi',
  SYSTEM_ADMIN: 'Sistem Yöneticisi',
  BILLING_OPERATOR: 'Fatura Operatörü',
  SYSTEM_SERVICE: 'Sistem Servisi',
};

export function roleLabel(role: string): string {
  return ROLE_LABELS[role] ?? role;
}
