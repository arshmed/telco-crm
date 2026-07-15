const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:9011';

export const redirectToKeycloakLogin = () => {
  window.location.href = `${API_URL}/oauth2/authorization/keycloak`;
};

export const logoutFromBff = async () => {
  try {
    const response = await fetch(`${API_URL}/logout`, {
      method: 'POST',
      credentials: 'include',
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
