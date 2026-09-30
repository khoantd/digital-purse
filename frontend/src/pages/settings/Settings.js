import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  LinearProgress,
  MenuItem,
  Popover,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { enqueueSnackbar } from 'notistack';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Iconify from '../../components/iconify';
import Label from '../../components/label';
import Scrollbar from '../../components/scrollbar';
import { EmptyState } from '../../components/wallet-ui';
import HttpService from '../../services/HttpService';
import OrganizationContext from '../../services/OrganizationContext';
import { fCurrency } from '../../utils/formatNumber';

const ASSIGNABLE_ROLES = [
  { value: 'ADMIN', label: 'Admin', hint: 'Manage members, wallets, and approvals' },
  { value: 'ACCOUNTANT', label: 'Accountant', hint: 'Day-to-day top-up, transfer, withdraw' },
  { value: 'APPROVER', label: 'Approver', hint: 'Approve or reject large spends only' },
];

const ROLE_COLOR = {
  OWNER: 'warning',
  ADMIN: 'info',
  ACCOUNTANT: 'success',
  APPROVER: 'secondary',
};

function canManageMembers(role) {
  return role === 'OWNER' || role === 'ADMIN';
}

function TabPanel({ children, value, index }) {
  if (value !== index) return null;
  return <Box sx={{ pt: 3 }}>{children}</Box>;
}

export default function Settings() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const tabFromQuery = searchParams.get('tab');
  const initialTab =
    tabFromQuery === 'subscription'
      ? 3
      : tabFromQuery === 'controls'
        ? 2
        : tabFromQuery === 'members'
          ? 1
          : 0;
  const [tab, setTab] = useState(initialTab);
  const [org, setOrg] = useState(null);
  const [members, setMembers] = useState([]);
  const [limitsForm, setLimitsForm] = useState({
    perTransactionMax: '',
    dailyOutboundMax: '',
    dailyTopUpMax: '',
    dualControlThreshold: '',
  });
  const [subscription, setSubscription] = useState({
    transactionQuota: 0,
    transactionUsed: 0,
    remaining: 0,
  });
  const [savingLimits, setSavingLimits] = useState(false);
  const [loading, setLoading] = useState(true);
  const [savingOrg, setSavingOrg] = useState(false);
  const [orgForm, setOrgForm] = useState({ name: '', taxId: '' });
  const [memberQuery, setMemberQuery] = useState('');
  const [addOpen, setAddOpen] = useState(false);
  const [addForm, setAddForm] = useState({ username: '', role: 'ACCOUNTANT' });
  const [submitting, setSubmitting] = useState(false);
  const [roleDialogOpen, setRoleDialogOpen] = useState(false);
  const [roleTarget, setRoleTarget] = useState(null);
  const [roleValue, setRoleValue] = useState('ACCOUNTANT');
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuMember, setMenuMember] = useState(null);

  const orgId = OrganizationContext.getActiveOrganizationId();
  const manage = canManageMembers(org?.myRole);

  useEffect(() => {
    if (tabFromQuery === 'subscription') setTab(3);
    else if (tabFromQuery === 'controls') setTab(2);
    else if (tabFromQuery === 'members') setTab(1);
    else if (tabFromQuery === 'general') setTab(0);
  }, [tabFromQuery]);

  const load = useCallback(() => {
    const id = OrganizationContext.getActiveOrganizationId();
    if (id == null) {
      setLoading(false);
      enqueueSnackbar('Select an organization first', { variant: 'warning' });
      return;
    }
    setLoading(true);
    Promise.all([
      HttpService.getWithAuth(`/organizations/${id}`),
      HttpService.getListWithAuth(`/organizations/${id}/members`),
      HttpService.getWithAuth(`/organizations/${id}/limits`),
      HttpService.getWithAuth(`/organizations/${id}/subscription`),
    ])
      .then(([orgData, memberList, limitsData, subscriptionData]) => {
        setOrg(orgData);
        setOrgForm({ name: orgData.name || '', taxId: orgData.taxId || '' });
        setMembers(memberList);
        setLimitsForm({
          perTransactionMax: String(limitsData?.perTransactionMax ?? ''),
          dailyOutboundMax: String(limitsData?.dailyOutboundMax ?? ''),
          dailyTopUpMax: String(limitsData?.dailyTopUpMax ?? ''),
          dualControlThreshold: String(limitsData?.dualControlThreshold ?? ''),
        });
        setSubscription({
          transactionQuota: Number(subscriptionData?.transactionQuota) || 0,
          transactionUsed: Number(subscriptionData?.transactionUsed) || 0,
          remaining: Number(subscriptionData?.remaining) || 0,
        });
      })
      .catch((error) => {
        if (error?.response?.status === 401) {
          navigate('/login');
        } else {
          enqueueSnackbar(error.response?.data?.message || 'Failed to load settings', { variant: 'error' });
        }
      })
      .finally(() => setLoading(false));
  }, [navigate]);

  useEffect(() => {
    load();
    const onOrg = () => load();
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
  }, [load]);

  const roleCounts = useMemo(() => {
    const counts = { OWNER: 0, ADMIN: 0, ACCOUNTANT: 0, APPROVER: 0 };
    members.forEach((m) => {
      if (counts[m.role] != null) counts[m.role] += 1;
    });
    return counts;
  }, [members]);

  const filteredMembers = useMemo(() => {
    const q = memberQuery.trim().toLowerCase();
    if (!q) return members;
    return members.filter((m) => {
      const name = `${m.firstName || ''} ${m.lastName || ''}`.toLowerCase();
      return m.username?.toLowerCase().includes(q) || name.includes(q) || m.role?.toLowerCase().includes(q);
    });
  }, [members, memberQuery]);

  const saveOrganization = (event) => {
    event.preventDefault();
    const name = orgForm.name.trim();
    if (name.length < 2) {
      enqueueSnackbar('Name must be at least 2 characters', { variant: 'warning' });
      return;
    }
    setSavingOrg(true);
    HttpService.putWithAuth(`/organizations/${orgId}`, {
      name,
      taxId: orgForm.taxId.trim() || null,
    })
      .then(() => {
        enqueueSnackbar('Organization updated', { variant: 'success' });
        load();
        window.dispatchEvent(new Event('organization-changed'));
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Update failed', { variant: 'error' });
      })
      .finally(() => setSavingOrg(false));
  };

  const parseVndAmount = (raw, label) => {
    const trimmed = String(raw ?? '').trim().replace(/,/g, '');
    if (!/^\d+$/.test(trimmed) || trimmed === '0') {
      enqueueSnackbar(`${label} must be a whole VND amount of at least 1`, { variant: 'warning' });
      return null;
    }
    return trimmed;
  };

  const saveLimits = (event) => {
    event.preventDefault();
    const perTransactionMax = parseVndAmount(limitsForm.perTransactionMax, 'Per transaction');
    if (perTransactionMax == null) return;
    const dailyOutboundMax = parseVndAmount(limitsForm.dailyOutboundMax, 'Daily outbound');
    if (dailyOutboundMax == null) return;
    const dailyTopUpMax = parseVndAmount(limitsForm.dailyTopUpMax, 'Daily top-up');
    if (dailyTopUpMax == null) return;
    const dualControlThreshold = parseVndAmount(limitsForm.dualControlThreshold, 'Dual-control threshold');
    if (dualControlThreshold == null) return;

    if (Number(dualControlThreshold) > Number(perTransactionMax)) {
      enqueueSnackbar('Dual-control threshold cannot exceed per-transaction maximum', { variant: 'warning' });
      return;
    }

    setSavingLimits(true);
    HttpService.putWithAuth(`/organizations/${orgId}/limits`, {
      perTransactionMax,
      dailyOutboundMax,
      dailyTopUpMax,
      dualControlThreshold,
    })
      .then(() => {
        enqueueSnackbar('Spending controls updated', { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Could not update controls', { variant: 'error' });
      })
      .finally(() => setSavingLimits(false));
  };

  const openAdd = () => {
    setAddForm({ username: '', role: 'ACCOUNTANT' });
    setAddOpen(true);
  };

  const submitAdd = (event) => {
    event.preventDefault();
    const username = addForm.username.trim();
    if (username.length < 3) {
      enqueueSnackbar('Username must be at least 3 characters', { variant: 'warning' });
      return;
    }
    setSubmitting(true);
    HttpService.postWithAuth(`/organizations/${orgId}/members`, {
      username,
      role: addForm.role,
    })
      .then(() => {
        enqueueSnackbar('Member added', { variant: 'success' });
        setAddOpen(false);
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Could not add member', { variant: 'error' });
      })
      .finally(() => setSubmitting(false));
  };

  const openMenu = (event, member) => {
    setMenuAnchor(event.currentTarget);
    setMenuMember(member);
  };

  const closeMenu = () => {
    setMenuAnchor(null);
    setMenuMember(null);
  };

  const openChangeRole = (member) => {
    closeMenu();
    setRoleTarget(member);
    setRoleValue(member.role === 'OWNER' ? 'ADMIN' : member.role);
    setRoleDialogOpen(true);
  };

  const submitRoleChange = (event) => {
    event.preventDefault();
    if (!roleTarget) return;
    setSubmitting(true);
    HttpService.putWithAuth(`/organizations/${orgId}/members/${roleTarget.id}`, { role: roleValue })
      .then(() => {
        enqueueSnackbar('Role updated', { variant: 'success' });
        setRoleDialogOpen(false);
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Role update failed', { variant: 'error' });
      })
      .finally(() => setSubmitting(false));
  };

  const removeMember = (member) => {
    closeMenu();
    if (!window.confirm(`Remove ${member.username} from this organization?`)) {
      return;
    }
    HttpService.deleteWithAuth(`/organizations/${orgId}/members/${member.id}`)
      .then(() => {
        enqueueSnackbar('Member removed', { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Remove failed', { variant: 'error' });
      });
  };

  const displayName = (m) => {
    const full = `${m.firstName || ''} ${m.lastName || ''}`.trim();
    return full || m.username;
  };

  return (
    <>
      <Helmet>
        <title> Settings | Digital Purse </title>
      </Helmet>
      <Container sx={{ minWidth: '100%' }}>
        <Stack spacing={0.5} mb={3}>
          <Typography variant="h4">Settings</Typography>
          <Typography variant="body2" color="text.secondary">
            Manage your organization, team roles, and spending controls for the active business.
          </Typography>
        </Stack>

        <Tabs
          value={tab}
          onChange={(_, next) => {
            setTab(next);
            const names = ['general', 'members', 'controls', 'subscription'];
            setSearchParams(next === 0 ? {} : { tab: names[next] }, { replace: true });
          }}
          sx={{ borderBottom: 1, borderColor: 'divider', mb: 0 }}
        >
          <Tab label="General" sx={{ cursor: 'pointer' }} />
          <Tab label="Members" sx={{ cursor: 'pointer' }} />
          <Tab label="Controls" sx={{ cursor: 'pointer' }} />
          <Tab label="Subscription" sx={{ cursor: 'pointer' }} />
        </Tabs>

        <TabPanel value={tab} index={0}>
          <Card sx={{ maxWidth: 560 }}>
            <CardContent>
              <Stack component="form" onSubmit={saveOrganization} spacing={2.5} noValidate>
                <Stack spacing={0.5}>
                  <Typography variant="h6">Organization</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Name and tax ID shown across wallets and reports for this business.
                  </Typography>
                </Stack>
                <TextField
                  label="Organization name"
                  value={orgForm.name}
                  onChange={(e) => setOrgForm((prev) => ({ ...prev, name: e.target.value }))}
                  required
                  disabled={!manage || loading}
                  inputProps={{ maxLength: 100 }}
                />
                <TextField
                  label="Tax ID"
                  value={orgForm.taxId}
                  onChange={(e) => setOrgForm((prev) => ({ ...prev, taxId: e.target.value }))}
                  disabled={!manage || loading}
                  inputProps={{ maxLength: 20 }}
                  helperText="Optional business tax / MST number"
                />
                <TextField
                  label="Organization ID"
                  value={org?.id ?? ''}
                  InputProps={{ readOnly: true }}
                  disabled
                />
                <TextField
                  label="Status"
                  value={org?.status ?? ''}
                  InputProps={{ readOnly: true }}
                  disabled
                />
                <TextField
                  label="Your role"
                  value={org?.myRole ?? ''}
                  InputProps={{ readOnly: true }}
                  disabled
                />
                {manage ? (
                  <LoadingButton
                    type="submit"
                    variant="contained"
                    loading={savingOrg}
                    sx={{ alignSelf: 'flex-start', cursor: 'pointer' }}
                  >
                    Save changes
                  </LoadingButton>
                ) : (
                  <Typography variant="caption" color="text.secondary">
                    Only owners and admins can edit organization details.
                  </Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </TabPanel>

        <TabPanel value={tab} index={1}>
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            alignItems={{ sm: 'center' }}
            justifyContent="space-between"
            spacing={2}
            mb={2}
            sx={{
              p: 2,
              borderRadius: 1,
              bgcolor: 'background.neutral',
            }}
          >
            <Stack spacing={0.5}>
              <Typography variant="subtitle1">
                {org?.name || 'Organization'} · {members.length} member{members.length === 1 ? '' : 's'}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {roleCounts.OWNER} owner · {roleCounts.ADMIN} admin · {roleCounts.ACCOUNTANT} accountant ·{' '}
                {roleCounts.APPROVER} approver
              </Typography>
            </Stack>
            {manage && (
              <Button
                variant="contained"
                startIcon={<Iconify icon="eva:person-add-fill" />}
                onClick={openAdd}
                sx={{ cursor: 'pointer', flexShrink: 0 }}
              >
                Add member
              </Button>
            )}
          </Stack>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} mb={2}>
            <TextField
              size="small"
              placeholder="Search members…"
              value={memberQuery}
              onChange={(e) => setMemberQuery(e.target.value)}
              sx={{ maxWidth: 320 }}
            />
          </Stack>

          <Card>
            <Scrollbar>
              <TableContainer sx={{ minWidth: 640 }}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Member</TableCell>
                      <TableCell>Username</TableCell>
                      <TableCell>Role</TableCell>
                      {manage && <TableCell align="right" />}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {!loading && filteredMembers.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={manage ? 4 : 3} sx={{ py: 6 }}>
                          <EmptyState
                            title="No members found"
                            description={
                              manage
                                ? 'Add a teammate by their Digital Purse username.'
                                : 'Ask an owner or admin to invite teammates.'
                            }
                            actionLabel={manage ? 'Add member' : undefined}
                            onAction={manage ? openAdd : undefined}
                          />
                        </TableCell>
                      </TableRow>
                    )}
                    {filteredMembers.map((row) => (
                      <TableRow key={row.id} hover>
                        <TableCell>
                          <Typography variant="subtitle2">{displayName(row)}</Typography>
                        </TableCell>
                        <TableCell>{row.username}</TableCell>
                        <TableCell>
                          <Label color={ROLE_COLOR[row.role] || 'default'}>{row.role}</Label>
                        </TableCell>
                        {manage && (
                          <TableCell align="right">
                            <IconButton onClick={(e) => openMenu(e, row)} sx={{ cursor: 'pointer' }}>
                              <Iconify icon="eva:more-vertical-fill" />
                            </IconButton>
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Scrollbar>
          </Card>
        </TabPanel>

        <TabPanel value={tab} index={2}>
          <Card sx={{ maxWidth: 640 }}>
            <CardContent>
              <Stack component="form" onSubmit={saveLimits} spacing={2.5} noValidate>
                <Stack spacing={0.5}>
                  <Typography variant="h6">Spending controls</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Limits for this organization (VND). Dual-control applies when transfer or withdraw is at or above
                    the threshold.
                  </Typography>
                </Stack>
                {[
                  {
                    key: 'perTransactionMax',
                    title: 'Per transaction',
                    description: 'Max amount for a single top-up, transfer, or withdraw.',
                  },
                  {
                    key: 'dailyOutboundMax',
                    title: 'Daily outbound',
                    description: 'Max sum of transfers and withdraws per day.',
                  },
                  {
                    key: 'dailyTopUpMax',
                    title: 'Daily top-up',
                    description: 'Max sum of top-ups into org wallets per day.',
                  },
                  {
                    key: 'dualControlThreshold',
                    title: 'Dual-control threshold',
                    description: 'At or above this amount, a second approver must approve.',
                  },
                ].map((item) => (
                  <TextField
                    key={item.key}
                    label={item.title}
                    value={limitsForm[item.key]}
                    onChange={(e) =>
                      setLimitsForm((prev) => ({ ...prev, [item.key]: e.target.value.replace(/[^\d]/g, '') }))
                    }
                    disabled={!manage || loading}
                    inputProps={{ inputMode: 'numeric', maxLength: 19 }}
                    helperText={
                      limitsForm[item.key]
                        ? `${item.description} (${fCurrency(limitsForm[item.key])})`
                        : item.description
                    }
                  />
                ))}
                {manage ? (
                  <LoadingButton
                    type="submit"
                    variant="contained"
                    loading={savingLimits}
                    sx={{ alignSelf: 'flex-start', cursor: 'pointer' }}
                  >
                    Save controls
                  </LoadingButton>
                ) : (
                  <Typography variant="caption" color="text.secondary">
                    Only owners and admins can change spending controls.
                  </Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </TabPanel>

        <TabPanel value={tab} index={3}>
          <Card sx={{ maxWidth: 560 }}>
            <CardContent>
              <Stack spacing={2.5}>
                <Stack spacing={0.5}>
                  <Typography variant="h6">Transaction subscription</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Lifetime allotment of money movements (transfer, withdraw, top-up) for this organization. New
                    organizations start with 1,000 transactions.
                  </Typography>
                </Stack>
                <Stack spacing={1}>
                  <Stack direction="row" justifyContent="space-between" alignItems="baseline">
                    <Typography variant="body2" color="text.secondary">
                      Used
                    </Typography>
                    <Typography variant="subtitle1">
                      {subscription.transactionUsed.toLocaleString()} /{' '}
                      {subscription.transactionQuota.toLocaleString()}
                    </Typography>
                  </Stack>
                  <LinearProgress
                    variant="determinate"
                    value={
                      subscription.transactionQuota > 0
                        ? Math.min(
                            100,
                            (subscription.transactionUsed / subscription.transactionQuota) * 100
                          )
                        : 0
                    }
                    color={subscription.remaining <= 0 ? 'error' : subscription.remaining < 100 ? 'warning' : 'primary'}
                    sx={{ height: 8, borderRadius: 1 }}
                  />
                  <Typography variant="body2" color="text.secondary">
                    {subscription.remaining <= 0
                      ? 'Quota used up — transfer, withdraw, and top-up are blocked until a platform admin raises the allotment.'
                      : `${subscription.remaining.toLocaleString()} transactions remaining`}
                  </Typography>
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        </TabPanel>
      </Container>

      <Popover
        open={Boolean(menuAnchor)}
        anchorEl={menuAnchor}
        onClose={closeMenu}
        anchorOrigin={{ vertical: 'top', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <MenuItem onClick={() => openChangeRole(menuMember)} sx={{ cursor: 'pointer' }}>
          <Iconify icon="eva:shield-fill" sx={{ mr: 2 }} />
          Change role
        </MenuItem>
        <MenuItem
          onClick={() => removeMember(menuMember)}
          sx={{ color: 'error.main', cursor: 'pointer' }}
        >
          <Iconify icon="eva:person-remove-fill" sx={{ mr: 2 }} />
          Remove
        </MenuItem>
      </Popover>

      <Dialog open={addOpen} onClose={() => setAddOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Add member</DialogTitle>
        <DialogContent>
          <Stack component="form" id="add-member-form" onSubmit={submitAdd} spacing={2} sx={{ pt: 1 }} noValidate>
            <TextField
              label="Username"
              value={addForm.username}
              onChange={(e) => setAddForm((prev) => ({ ...prev, username: e.target.value }))}
              required
              helperText="Existing Digital Purse username"
              inputProps={{ maxLength: 20 }}
            />
            <TextField
              select
              label="Role"
              value={addForm.role}
              onChange={(e) => setAddForm((prev) => ({ ...prev, role: e.target.value }))}
            >
              {ASSIGNABLE_ROLES.map((r) => (
                <MenuItem key={r.value} value={r.value}>
                  <Stack>
                    <Typography variant="body2">{r.label}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {r.hint}
                    </Typography>
                  </Stack>
                </MenuItem>
              ))}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddOpen(false)} sx={{ cursor: 'pointer' }}>
            Cancel
          </Button>
          <LoadingButton
            type="submit"
            form="add-member-form"
            variant="contained"
            loading={submitting}
            sx={{ cursor: 'pointer' }}
          >
            Add
          </LoadingButton>
        </DialogActions>
      </Dialog>

      <Dialog open={roleDialogOpen} onClose={() => setRoleDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Change role</DialogTitle>
        <DialogContent>
          <Stack component="form" id="role-form" onSubmit={submitRoleChange} spacing={2} sx={{ pt: 1 }} noValidate>
            <Typography variant="body2" color="text.secondary">
              {roleTarget ? displayName(roleTarget) : ''} ({roleTarget?.username})
            </Typography>
            <TextField
              select
              label="Role"
              value={roleValue}
              onChange={(e) => setRoleValue(e.target.value)}
            >
              {ASSIGNABLE_ROLES.map((r) => (
                <MenuItem key={r.value} value={r.value}>
                  <Stack>
                    <Typography variant="body2">{r.label}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {r.hint}
                    </Typography>
                  </Stack>
                </MenuItem>
              ))}
            </TextField>
            {roleTarget?.role === 'OWNER' && (
              <Typography variant="caption" color="warning.main">
                Demoting an owner is only allowed when another owner remains in the organization.
              </Typography>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRoleDialogOpen(false)} sx={{ cursor: 'pointer' }}>
            Cancel
          </Button>
          <LoadingButton
            type="submit"
            form="role-form"
            variant="contained"
            loading={submitting}
            sx={{ cursor: 'pointer' }}
          >
            Save
          </LoadingButton>
        </DialogActions>
      </Dialog>
    </>
  );
}
