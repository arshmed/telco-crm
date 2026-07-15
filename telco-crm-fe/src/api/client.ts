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

function redirectToLogin() {
  const redirectCount = parseInt(sessionStorage.getItem(REDIRECT_KEY) || '0', 10);
  if (redirectCount >= MAX_REDIRECTS) {
    sessionStorage.removeItem(REDIRECT_KEY);
    return;
  }
  sessionStorage.setItem(REDIRECT_KEY, String(redirectCount + 1));
  alert('Oturumunuzun süresi doldu. Lütfen tekrar giriş yapın.');
  window.location.href = BFF_LOGIN_URL;
}

function isSessionExpired(error: any): boolean {
  // Durum 1: 401/403 — doğrudan session süresi dolmuş
  if (error.response?.status === 401 || error.response?.status === 403) return true;

  // Durum 2: 302 redirect Keycloak'a gitti → Browser CORS preflight → 405
  // responseURL Keycloak'a yönlenmişse session süresi dolmuştur
  const responseURL = error.request?.responseURL || '';
  if (responseURL.includes('localhost:8085') || responseURL.includes('/oauth2/authorization/')) return true;

  // Durum 3: Yanıt HTML (Keycloak login sayfası)
  const contentType = error.response?.headers?.['content-type'] || '';
  if (contentType.includes('text/html')) return true;

  return false;
}

// Session süresi dolduğunda login'e yönlendir
apiClient.interceptors.response.use(
  (response) => {
    // Başarılı yanıt ama content-type HTML ise → Keycloak login sayfasına redirect edilmiş
    const contentType = String(response.headers['content-type'] || '');
    if (contentType.includes('text/html')) {
      if (window.location.pathname !== '/login') redirectToLogin();
    }
    sessionStorage.removeItem(REDIRECT_KEY);
    return response;
  },
  (error) => {
    if (isSessionExpired(error) && window.location.pathname !== '/login') {
      redirectToLogin();
    }
    return Promise.reject(error);
  }
);
