import AuthService from './AuthService';

export default function AuthHeader() {
  const token = AuthService.getAccessToken();

  return token ? { Accept: 'application/json', Authorization: `Bearer ${token}` } : {};
}
