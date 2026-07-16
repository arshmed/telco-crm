import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:9011';

export const apiClient = axios.create({
  baseURL: API_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// CSRF token XSRF-TOKEN cookie'den okunup header'a eklenir
apiClient.interceptors.request.use((config) => {
  const xsrfToken = document.cookie
    .split('; ')
    .find(row => row.startsWith('XSRF-TOKEN='))
    ?.split('=')[1];
  if (xsrfToken) {
    config.headers['X-XSRF-TOKEN'] = xsrfToken;
  }
  return config;
});

// 403 bilerek disarida: oturum gecerli, sadece yetki yok — login'e atmak sonsuz dongu olur.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && window.location.pathname !== '/login') {
      window.location.replace('/login');
    }
    return Promise.reject(error);
  }
);
