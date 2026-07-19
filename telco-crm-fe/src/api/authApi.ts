import { apiClient } from './client';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:9011';

export interface CurrentUser {
  username: string;
  roles: string[];
}

export const redirectToKeycloakLogin = () => {
  window.location.href = `${API_URL}/oauth2/authorization/keycloak`;
};

export const fetchCurrentUser = async (): Promise<CurrentUser> => {
  const { data } = await apiClient.get<CurrentUser>('/bff/me');
  return data;
};

export const logoutFromBff = async () => {
  try {
    const xsrfToken = document.cookie
      .split('; ')
      .find((row) => row.startsWith('XSRF-TOKEN='))
      ?.split('=')[1];

    const response = await fetch(`${API_URL}/logout`, {
      method: 'POST',
      credentials: 'include',
      headers: xsrfToken ? { 'X-XSRF-TOKEN': xsrfToken } : undefined,
    });
    const location = response.headers.get('Location');
    if (location) {
      window.location.href = location;
    } else {
      window.location.href = '/login';
    }
  } catch {
    window.location.href = '/login';
  }
};
