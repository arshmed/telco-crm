import axios from 'axios';

// Keycloak ayarları
const KEYCLOAK_URL = 'http://localhost:8085';
const REALM = 'telcocrm-gygy5';
const CLIENT_ID = 'telco-frontend'; // Keycloak'ta oluşturacağın/oluşturduğun Client ID

export const loginWithKeycloak = async (username: string, password: string) => {
  const tokenEndpoint = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;

  const params = new URLSearchParams();
  params.append('client_id', CLIENT_ID);
  params.append('grant_type', 'password');
  params.append('username', username);
  params.append('password', password);

  const response = await axios.post(tokenEndpoint, params, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  });

  return response.data; // { access_token, refresh_token, expires_in, vs... }
};

export const refreshTokenWithKeycloak = async (refreshToken: string) => {
  const tokenEndpoint = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;

  const params = new URLSearchParams();
  params.append('client_id', CLIENT_ID);
  params.append('grant_type', 'refresh_token');
  params.append('refresh_token', refreshToken);

  const response = await axios.post(tokenEndpoint, params, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  });

  return response.data;
};
