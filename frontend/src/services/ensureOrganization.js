import HttpService from './HttpService';
import OrganizationContext from './OrganizationContext';

/**
 * Ensures localStorage has a valid active organization before org-scoped API calls
 * (X-Organization-Id). Call after login/refresh and before dashboard data loads.
 *
 * @returns {Promise<Array>} memberships (possibly empty)
 */
export async function ensureActiveOrganization() {
  const list = await HttpService.getListWithAuth('/organizations');
  if (!list.length) {
    OrganizationContext.clearActiveOrganizationId();
    return [];
  }
  const current = OrganizationContext.getActiveOrganizationId();
  const stillValid = list.some((o) => o.id === current);
  if (!stillValid) {
    OrganizationContext.setActiveOrganizationId(list[0].id);
    window.dispatchEvent(new Event('organization-changed'));
  }
  return list;
}
