import AuthService from './AuthService';
import OrganizationContext from './OrganizationContext';

export default function AuthHeader() {
  const token = AuthService.getAccessToken();
  if (!token) {
    return {};
  }
  const headers = { Accept: 'application/json', Authorization: `Bearer ${token}` };
  const orgId = OrganizationContext.getActiveOrganizationId();
  if (orgId != null) {
    headers['X-Organization-Id'] = String(orgId);
  }
  return headers;
}
