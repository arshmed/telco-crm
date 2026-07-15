import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:9011';
const BFF_LOGIN_URL = `${API_URL}/oauth2/authorization/keycloak`;
const REDIRECT_KEY = 'telco_auth_redirect_count';
const MAX_REDIRECTS = 2;

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
}, (error) => {
  return Promise.reject(error);
});

// 401 geldiğinde session süresinin dolduğu anlaşılır → login'e yönlendir
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Login sayfasındayken tekrar 401 gelirse loop'a düşme
      if (window.location.pathname === '/login') {
        return Promise.reject(error);
      }

      // Redirect sayacı — aşırı yönlengi engelle
      const redirectCount = parseInt(sessionStorage.getItem(REDIRECT_KEY) || '0', 10);
      if (redirectCount >= MAX_REDIRECTS) {
        sessionStorage.removeItem(REDIRECT_KEY);
        return Promise.reject(error);
      }
      sessionStorage.setItem(REDIRECT_KEY, String(redirectCount + 1));

      alert('Oturumunuzun süresi doldu. Lütfen tekrar giriş yapın.');
      window.location.href = BFF_LOGIN_URL;
    }
    return Promise.reject(error);
  }
);

// Başarılı login sonrası redirect sayacını sıfırla
apiClient.interceptors.response.use(
  (response) => {
    sessionStorage.removeItem(REDIRECT_KEY);
    return response;
  },
  (error) => Promise.reject(error)
);
