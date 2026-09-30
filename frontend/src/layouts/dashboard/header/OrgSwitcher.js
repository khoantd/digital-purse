import { useEffect, useState } from 'react';
import { FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { useTranslation } from 'react-i18next';
import OrganizationContext from '../../../services/OrganizationContext';
import { ensureActiveOrganization } from '../../../services/ensureOrganization';

/**
 * Selects the active SME organization (sends X-Organization-Id on subsequent API calls).
 */
export default function OrgSwitcher() {
  const [orgs, setOrgs] = useState([]);
  const { t } = useTranslation('header');
  const [value, setValue] = useState(() => {
    const id = OrganizationContext.getActiveOrganizationId();
    return id != null ? String(id) : '';
  });

  useEffect(() => {
    ensureActiveOrganization()
      .then((list) => {
        setOrgs(list);
        const current = OrganizationContext.getActiveOrganizationId();
        setValue(current != null ? String(current) : '');
      })
      .catch(() => undefined);
  }, []);

  const handleChange = (event) => {
    const id = Number(event.target.value);
    OrganizationContext.setActiveOrganizationId(id);
    setValue(String(id));
    window.dispatchEvent(new Event('organization-changed'));
  };

  if (orgs.length === 0) {
    return null;
  }

  const orgLabel = t('organization');

  return (
    <FormControl size="small" sx={{ minWidth: 180 }}>
      <InputLabel id="org-switcher-label">{orgLabel}</InputLabel>
      <Select labelId="org-switcher-label" label={orgLabel} value={value} onChange={handleChange}>
        {orgs.map((org) => (
          <MenuItem key={org.id} value={String(org.id)}>
            {org.name}
            {org.myRole ? ` · ${org.myRole}` : ''}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
