/**
 * Active organization context for SME multi-tenant API calls.
 */
const STORAGE_KEY = 'ewallet.activeOrganizationId';

const getActiveOrganizationId = () => {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  const id = Number(raw);
  return Number.isFinite(id) ? id : null;
};

const setActiveOrganizationId = (id) => {
  if (id == null) {
    localStorage.removeItem(STORAGE_KEY);
    return;
  }
  localStorage.setItem(STORAGE_KEY, String(id));
};

const clearActiveOrganizationId = () => {
  localStorage.removeItem(STORAGE_KEY);
};

const OrganizationContext = {
  getActiveOrganizationId,
  setActiveOrganizationId,
  clearActiveOrganizationId,
  STORAGE_KEY,
};

export default OrganizationContext;
