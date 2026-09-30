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
import { useTranslation } from 'react-i18next';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Iconify from '../../components/iconify';
import Label from '../../components/label';
import Scrollbar from '../../components/scrollbar';
import { EmptyState } from '../../components/wallet-ui';
import HttpService from '../../services/HttpService';
import OrganizationContext from '../../services/OrganizationContext';
import { fCurrency, fNumber } from '../../utils/formatNumber';
import ProfilePanel from './ProfilePanel';

const ASSIGNABLE_ROLE_VALUES = ['ADMIN', 'ACCOUNTANT', 'APPROVER'];

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
  const { t } = useTranslation(['settings', 'common']);
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const tabFromQuery = searchParams.get('tab');
  const initialTab =
    tabFromQuery === 'subscription'
      ? 4
      : tabFromQuery === 'controls'
        ? 3
        : tabFromQuery === 'members'
          ? 2
          : tabFromQuery === 'general'
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

  const assignableRoles = useMemo(
    () =>
      ASSIGNABLE_ROLE_VALUES.map((value) => ({
        value,
        label: t(`members.roles.${value.toLowerCase()}`),
        hint: t(`members.roles.${value.toLowerCase()}Hint`),
      })),
    [t]
  );

  const limitFields = useMemo(
    () => [
      {
        key: 'perTransactionMax',
        title: t('controls.perTransactionTitle'),
        description: t('controls.perTransactionDescription'),
      },
      {
        key: 'dailyOutboundMax',
        title: t('controls.dailyOutboundTitle'),
        description: t('controls.dailyOutboundDescription'),
      },
      {
        key: 'dailyTopUpMax',
        title: t('controls.dailyTopUpTitle'),
        description: t('controls.dailyTopUpDescription'),
      },
      {
        key: 'dualControlThreshold',
        title: t('controls.dualControlTitle'),
        description: t('controls.dualControlDescription'),
      },
    ],
    [t]
  );

  useEffect(() => {
    if (tabFromQuery === 'subscription') setTab(4);
    else if (tabFromQuery === 'controls') setTab(3);
    else if (tabFromQuery === 'members') setTab(2);
    else if (tabFromQuery === 'general') setTab(1);
    else if (tabFromQuery === 'profile' || !tabFromQuery) setTab(0);
  }, [tabFromQuery]);

  const load = useCallback(() => {
    const id = OrganizationContext.getActiveOrganizationId();
    if (id == null) {
      setLoading(false);
      enqueueSnackbar(t('messages.selectOrg'), { variant: 'warning' });
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
          enqueueSnackbar(error.response?.data?.message || t('messages.loadFailed'), { variant: 'error' });
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
      enqueueSnackbar(t('snackbar.orgNameMinLength'), { variant: 'warning' });
      return;
    }
    setSavingOrg(true);
    HttpService.putWithAuth(`/organizations/${orgId}`, {
      name,
      taxId: orgForm.taxId.trim() || null,
    })
      .then(() => {
        enqueueSnackbar(t('snackbar.organizationUpdated'), { variant: 'success' });
        load();
        window.dispatchEvent(new Event('organization-changed'));
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.orgUpdateFailed'), { variant: 'error' });
      })
      .finally(() => setSavingOrg(false));
  };

  const parseVndAmount = (raw, label) => {
    const trimmed = String(raw ?? '').trim().replace(/,/g, '');
    if (!/^\d+$/.test(trimmed) || trimmed === '0') {
      enqueueSnackbar(t('snackbar.vndAmountInvalid', { label }), { variant: 'warning' });
      return null;
    }
    return trimmed;
  };

  const saveLimits = (event) => {
    event.preventDefault();
    const perTransactionMax = parseVndAmount(limitsForm.perTransactionMax, t('controls.perTransactionTitle'));
    if (perTransactionMax == null) return;
    const dailyOutboundMax = parseVndAmount(limitsForm.dailyOutboundMax, t('controls.dailyOutboundTitle'));
    if (dailyOutboundMax == null) return;
    const dailyTopUpMax = parseVndAmount(limitsForm.dailyTopUpMax, t('controls.dailyTopUpTitle'));
    if (dailyTopUpMax == null) return;
    const dualControlThreshold = parseVndAmount(limitsForm.dualControlThreshold, t('controls.dualControlTitle'));
    if (dualControlThreshold == null) return;

    if (Number(dualControlThreshold) > Number(perTransactionMax)) {
      enqueueSnackbar(t('snackbar.dualControlExceedsMax'), { variant: 'warning' });
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
        enqueueSnackbar(t('snackbar.spendingControlsUpdated'), { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.controlsUpdateFailed'), { variant: 'error' });
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
      enqueueSnackbar(t('snackbar.usernameMinLength'), { variant: 'warning' });
      return;
    }
    setSubmitting(true);
    HttpService.postWithAuth(`/organizations/${orgId}/members`, {
      username,
      role: addForm.role,
    })
      .then(() => {
        enqueueSnackbar(t('snackbar.memberAdded'), { variant: 'success' });
        setAddOpen(false);
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.memberAddFailed'), { variant: 'error' });
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
        enqueueSnackbar(t('snackbar.roleUpdated'), { variant: 'success' });
        setRoleDialogOpen(false);
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.roleUpdateFailed'), { variant: 'error' });
      })
      .finally(() => setSubmitting(false));
  };

  const removeMember = (member) => {
    closeMenu();
    if (!window.confirm(t('members.confirmRemove', { username: member.username }))) {
      return;
    }
    HttpService.deleteWithAuth(`/organizations/${orgId}/members/${member.id}`)
      .then(() => {
        enqueueSnackbar(t('snackbar.memberRemoved'), { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.removeFailed'), { variant: 'error' });
      });
  };

  const displayName = (m) => {
    const full = `${m.firstName || ''} ${m.lastName || ''}`.trim();
    return full || m.username;
  };

  return (
    <>
      <Helmet>
        <title>{t('helmet')}</title>
      </Helmet>
      <Container sx={{ minWidth: '100%' }}>
        <Stack spacing={0.5} mb={3}>
          <Typography variant="h4">{t('title')}</Typography>
          <Typography variant="body2" color="text.secondary">
            {t('subtitle')}
          </Typography>
        </Stack>

        <Tabs
          value={tab}
          onChange={(_, next) => {
            setTab(next);
            const names = ['profile', 'general', 'members', 'controls', 'subscription'];
            setSearchParams(next === 0 ? { tab: 'profile' } : { tab: names[next] }, { replace: true });
          }}
          sx={{ borderBottom: 1, borderColor: 'divider', mb: 0 }}
        >
          <Tab label={t('tabs.profile')} sx={{ cursor: 'pointer' }} />
          <Tab label={t('tabs.general')} sx={{ cursor: 'pointer' }} />
          <Tab label={t('tabs.members')} sx={{ cursor: 'pointer' }} />
          <Tab label={t('tabs.controls')} sx={{ cursor: 'pointer' }} />
          <Tab label={t('tabs.subscription')} sx={{ cursor: 'pointer' }} />
        </Tabs>

        <TabPanel value={tab} index={0}>
          <ProfilePanel />
        </TabPanel>

        <TabPanel value={tab} index={1}>
          <Card sx={{ maxWidth: 560 }}>
            <CardContent>
              <Stack component="form" onSubmit={saveOrganization} spacing={2.5} noValidate>
                <Stack spacing={0.5}>
                  <Typography variant="h6">{t('general.sectionTitle')}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t('general.sectionDescription')}
                  </Typography>
                </Stack>
                <TextField
                  label={t('general.orgName')}
                  value={orgForm.name}
                  onChange={(e) => setOrgForm((prev) => ({ ...prev, name: e.target.value }))}
                  required
                  disabled={!manage || loading}
                  inputProps={{ maxLength: 100 }}
                />
                <TextField
                  label={t('general.taxId')}
                  value={orgForm.taxId}
                  onChange={(e) => setOrgForm((prev) => ({ ...prev, taxId: e.target.value }))}
                  disabled={!manage || loading}
                  inputProps={{ maxLength: 20 }}
                  helperText={t('general.taxIdHelper')}
                />
                <TextField
                  label={t('general.orgId')}
                  value={org?.id ?? ''}
                  InputProps={{ readOnly: true }}
                  disabled
                />
                <TextField
                  label={t('general.status')}
                  value={org?.status ?? ''}
                  InputProps={{ readOnly: true }}
                  disabled
                />
                <TextField
                  label={t('general.yourRole')}
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
                    {t('general.save')}
                  </LoadingButton>
                ) : (
                  <Typography variant="caption" color="text.secondary">
                    {t('general.readOnly')}
                  </Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </TabPanel>

        <TabPanel value={tab} index={2}>
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
                {t('members.summary', {
                  orgName: org?.name || t('general.sectionTitle'),
                  count: members.length,
                })}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {t('members.roleBreakdown', {
                  owners: roleCounts.OWNER,
                  admins: roleCounts.ADMIN,
                  accountants: roleCounts.ACCOUNTANT,
                  approvers: roleCounts.APPROVER,
                })}
              </Typography>
            </Stack>
            {manage && (
              <Button
                variant="contained"
                startIcon={<Iconify icon="eva:person-add-fill" />}
                onClick={openAdd}
                sx={{ cursor: 'pointer', flexShrink: 0 }}
              >
                {t('members.addMember')}
              </Button>
            )}
          </Stack>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} mb={2}>
            <TextField
              size="small"
              placeholder={t('members.searchPlaceholder')}
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
                      <TableCell>{t('members.memberColumn')}</TableCell>
                      <TableCell>{t('members.usernameColumn')}</TableCell>
                      <TableCell>{t('members.roleColumn')}</TableCell>
                      {manage && <TableCell align="right" />}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {!loading && filteredMembers.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={manage ? 4 : 3} sx={{ py: 6 }}>
                          <EmptyState
                            title={t('members.empty')}
                            description={
                              manage ? t('members.emptyDescriptionManage') : t('members.emptyDescriptionReadOnly')
                            }
                            actionLabel={manage ? t('members.addMember') : undefined}
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

        <TabPanel value={tab} index={3}>
          <Card sx={{ maxWidth: 640 }}>
            <CardContent>
              <Stack component="form" onSubmit={saveLimits} spacing={2.5} noValidate>
                <Stack spacing={0.5}>
                  <Typography variant="h6">{t('controls.sectionTitle')}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t('controls.sectionDescription')}
                  </Typography>
                </Stack>
                {limitFields.map((item) => (
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
                        ? t('controls.helperWithAmount', {
                            description: item.description,
                            amount: fCurrency(limitsForm[item.key]),
                          })
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
                    {t('controls.save')}
                  </LoadingButton>
                ) : (
                  <Typography variant="caption" color="text.secondary">
                    {t('controls.readOnly')}
                  </Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </TabPanel>

        <TabPanel value={tab} index={4}>
          <Card sx={{ maxWidth: 560 }}>
            <CardContent>
              <Stack spacing={2.5}>
                <Stack spacing={0.5}>
                  <Typography variant="h6">{t('subscription.sectionTitle')}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t('subscription.sectionDescription')}
                  </Typography>
                </Stack>
                <Stack spacing={1}>
                  <Stack direction="row" justifyContent="space-between" alignItems="baseline">
                    <Typography variant="body2" color="text.secondary">
                      {t('subscription.used')}
                    </Typography>
                    <Typography variant="subtitle1">
                      {fNumber(subscription.transactionUsed)} / {fNumber(subscription.transactionQuota)}
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
                      ? t('subscription.quotaExhausted')
                      : t('subscription.remaining', { count: subscription.remaining })}
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
          {t('common:actions.changeRole')}
        </MenuItem>
        <MenuItem
          onClick={() => removeMember(menuMember)}
          sx={{ color: 'error.main', cursor: 'pointer' }}
        >
          <Iconify icon="eva:person-remove-fill" sx={{ mr: 2 }} />
          {t('common:actions.remove')}
        </MenuItem>
      </Popover>

      <Dialog open={addOpen} onClose={() => setAddOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>{t('members.addTitle')}</DialogTitle>
        <DialogContent>
          <Stack component="form" id="add-member-form" onSubmit={submitAdd} spacing={2} sx={{ pt: 1 }} noValidate>
            <TextField
              label={t('common:fields.username')}
              value={addForm.username}
              onChange={(e) => setAddForm((prev) => ({ ...prev, username: e.target.value }))}
              required
              helperText={t('members.usernameHelper')}
              inputProps={{ maxLength: 20 }}
            />
            <TextField
              select
              label={t('members.role')}
              value={addForm.role}
              onChange={(e) => setAddForm((prev) => ({ ...prev, role: e.target.value }))}
            >
              {assignableRoles.map((r) => (
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
            {t('common:actions.cancel')}
          </Button>
          <LoadingButton
            type="submit"
            form="add-member-form"
            variant="contained"
            loading={submitting}
            sx={{ cursor: 'pointer' }}
          >
            {t('common:actions.add')}
          </LoadingButton>
        </DialogActions>
      </Dialog>

      <Dialog open={roleDialogOpen} onClose={() => setRoleDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>{t('members.changeRoleTitle')}</DialogTitle>
        <DialogContent>
          <Stack component="form" id="role-form" onSubmit={submitRoleChange} spacing={2} sx={{ pt: 1 }} noValidate>
            <Typography variant="body2" color="text.secondary">
              {roleTarget ? displayName(roleTarget) : ''} ({roleTarget?.username})
            </Typography>
            <TextField
              select
              label={t('members.role')}
              value={roleValue}
              onChange={(e) => setRoleValue(e.target.value)}
            >
              {assignableRoles.map((r) => (
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
                {t('members.ownerDemoteHint')}
              </Typography>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRoleDialogOpen(false)} sx={{ cursor: 'pointer' }}>
            {t('common:actions.cancel')}
          </Button>
          <LoadingButton
            type="submit"
            form="role-form"
            variant="contained"
            loading={submitting}
            sx={{ cursor: 'pointer' }}
          >
            {t('common:actions.save')}
          </LoadingButton>
        </DialogActions>
      </Dialog>
    </>
  );
}
