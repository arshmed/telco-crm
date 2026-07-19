import { apiClient } from './client';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface UserResponse {
  id: string;
  customerId: string | null;
  username: string;
  email: string;
  fullName: string;
  phoneNumber: string | null;
  status: UserStatus;
  keycloakUserId: string | null;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  customerId?: string;
}

export interface UpdateUserRequest {
  email?: string;
  fullName?: string;
  phoneNumber?: string;
}

export interface RoleResponse {
  id: string;
  name: string;
  description: string | null;
  permissions: string[];
}

export interface PermissionResponse {
  id: string;
  name: string;
  description: string | null;
}

export interface AuditLogResponse {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  detail: string | null;
  performedBy: string;
  createdAt: string;
}

export const getUsers = async (page = 0, size = 20) => {
  const response = await apiClient.get<Page<UserResponse>>('/api/v1/users', { params: { page, size } });
  return response.data;
};

export const getUserById = async (id: string) => {
  const response = await apiClient.get<UserResponse>(`/api/v1/users/${id}`);
  return response.data;
};

export const createUser = async (data: CreateUserRequest) => {
  const response = await apiClient.post<UserResponse>('/api/v1/users', data);
  return response.data;
};

export const updateUser = async (id: string, data: UpdateUserRequest) => {
  const response = await apiClient.put<UserResponse>(`/api/v1/users/${id}`, data);
  return response.data;
};

export const assignRole = async (userId: string, roleName: string) => {
  const response = await apiClient.post<UserResponse>(`/api/v1/users/${userId}/roles`, { roleName });
  return response.data;
};

export const getRoles = async () => {
  const response = await apiClient.get<RoleResponse[]>('/api/v1/roles');
  return response.data;
};

export const createRole = async (data: { name: string; description?: string }) => {
  const response = await apiClient.post<RoleResponse>('/api/v1/roles', data);
  return response.data;
};

export const assignPermission = async (roleName: string, permissionName: string) => {
  const response = await apiClient.post<RoleResponse>(`/api/v1/roles/${roleName}/permissions`, { permissionName });
  return response.data;
};

export const getPermissions = async () => {
  const response = await apiClient.get<PermissionResponse[]>('/api/v1/permissions');
  return response.data;
};

export const createPermission = async (data: { name: string; description?: string }) => {
  const response = await apiClient.post<PermissionResponse>('/api/v1/permissions', data);
  return response.data;
};

export const getAuditLogs = async (page = 0, size = 20, entityType?: string) => {
  const response = await apiClient.get<Page<AuditLogResponse>>('/api/v1/audit-logs', {
    params: { page, size, entityType },
  });
  return response.data;
};
