import {
  Box,
  Button,
  Card,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  MenuItem,
  Popover,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { enqueueSnackbar } from 'notistack';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import Iconify from '../../components/iconify';
import Label from '../../components/label';
import Scrollbar from '../../components/scrollbar';
import { EmptyState, OpsStatCard } from '../../components/wallet-ui';
import HttpService from '../../services/HttpService';

const emptyForm = {
  name: '',
  phone: '',
  email: '',
  taxId: '',
  notes: '',
};

const ROWS_PER_PAGE_OPTIONS = [5, 10, 25];
const PAGINATION_SX = {
  borderTop: (theme) => `1px solid ${theme.palette.divider}`,
  cursor: 'pointer',
  '& .MuiTablePagination-actions button': { cursor: 'pointer' },
  '& .MuiInputBase-root': { cursor: 'pointer' },
};

const FOUR_COL_GRID_SX = {
  display: 'grid',
  gap: 2,
  gridTemplateColumns: {
    xs: '1fr',
    sm: 'repeat(2, 1fr)',
    md: 'repeat(4, 1fr)',
  },
};

const ACTIVE_STAT_SX = {
  boxShadow: (theme) => theme.customShadows.z8,
  outline: (theme) => `2px solid ${theme.palette.primary.main}`,
  outlineOffset: 1,
};

function matchesStatusFilter(row, statusFilter) {
  if (!statusFilter || statusFilter === 'ALL') return true;
  return (row.status || '').toUpperCase() === statusFilter;
}

function matchesLinkedFilter(row, linkedFilter) {
  if (!linkedFilter) return true;
  const hasLink = Boolean(row.linkedWalletIban);
  if (linkedFilter === 'linked') return hasLink;
  if (linkedFilter === 'unlinked') return !hasLink;
  return true;
}

export default function Customers() {
  const { t } = useTranslation(['customers', 'common']);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ACTIVE');
  const [linkedFilter, setLinkedFilter] = useState('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formValues, setFormValues] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [linkDialogOpen, setLinkDialogOpen] = useState(false);
  const [linkIban, setLinkIban] = useState('');
  const [linkCustomerId, setLinkCustomerId] = useState(null);
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuCustomer, setMenuCustomer] = useState(null);
  const navigate = useNavigate();

  const statusOptions = useMemo(
    () => [
      { value: 'ACTIVE', label: t('filters.statusActive') },
      { value: 'ARCHIVED', label: t('filters.statusArchived') },
      { value: 'ALL', label: t('filters.statusAll') },
    ],
    [t]
  );
  const linkedOptions = useMemo(
    () => [
      { value: '', label: t('filters.linkedAll') },
      { value: 'linked', label: t('filters.linkedYes') },
      { value: 'unlinked', label: t('filters.linkedNo') },
    ],
    [t]
  );

  const filtersActive = Boolean(
    appliedQuery.trim() || statusFilter !== 'ACTIVE' || linkedFilter
  );

  const filteredRows = useMemo(
    () =>
      rows.filter(
        (row) => matchesStatusFilter(row, statusFilter) && matchesLinkedFilter(row, linkedFilter)
      ),
    [rows, statusFilter, linkedFilter]
  );

  const directoryStats = useMemo(() => {
    let activeCount = 0;
    let archivedCount = 0;
    let linkedCount = 0;
    let unlinkedCount = 0;
    rows.forEach((row) => {
      const status = (row.status || '').toUpperCase();
      if (status === 'ACTIVE') activeCount += 1;
      else if (status === 'ARCHIVED') archivedCount += 1;
      if (row.linkedWalletIban) linkedCount += 1;
      else unlinkedCount += 1;
    });
    return { activeCount, archivedCount, linkedCount, unlinkedCount };
  }, [rows]);

  const load = (q = appliedQuery) => {
    setLoading(true);
    const params = new URLSearchParams();
    if (q?.trim()) params.set('q', q.trim());
    params.set('status', 'ALL');
    const url = `/customers?${params.toString()}`;
    HttpService.getListWithAuth(url)
      .then((list) => {
        setRows(list);
        setPage(0);
      })
      .catch((error) => {
        if (error?.response?.status === 401) {
          navigate('/login');
        } else if (error.response?.data?.message) {
          enqueueSnackbar(error.response.data.message, { variant: 'error' });
        }
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    const onOrg = () => {
      setQuery('');
      setAppliedQuery('');
      setStatusFilter('ACTIVE');
      setLinkedFilter('');
      load('');
    };
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(filteredRows.length / rowsPerPage) - 1);
    if (page > maxPage) {
      setPage(maxPage);
    }
  }, [filteredRows.length, rowsPerPage, page]);

  const pagedRows = filteredRows.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);

  const runSearch = () => {
    const next = query.trim();
    setAppliedQuery(next);
    load(next);
  };

  const clearFilters = () => {
    setQuery('');
    setAppliedQuery('');
    setStatusFilter('ACTIVE');
    setLinkedFilter('');
    setPage(0);
    load('');
  };

  const toggleStatusFilter = (status) => {
    setStatusFilter((current) => (current === status ? 'ALL' : status));
    setPage(0);
  };

  const toggleLinkedFilter = (linked) => {
    setLinkedFilter((current) => (current === linked ? '' : linked));
    setPage(0);
  };

  const openCreate = () => {
    setEditingId(null);
    setFormValues(emptyForm);
    setDialogOpen(true);
  };

  const openEdit = (customer) => {
    setEditingId(customer.id);
    setFormValues({
      name: customer.name || '',
      phone: customer.phone || '',
      email: customer.email || '',
      taxId: customer.taxId || '',
      notes: customer.notes || '',
    });
    setDialogOpen(true);
    closeMenu();
  };

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormValues((prev) => ({ ...prev, [name]: value }));
  };

  const saveCustomer = (event) => {
    event.preventDefault();
    const name = formValues.name.trim();
    if (name.length < 2) {
      enqueueSnackbar(t('snackbar.nameMinLength'), { variant: 'warning' });
      return;
    }
    setSubmitting(true);
    const body = {
      name,
      phone: formValues.phone.trim() || null,
      email: formValues.email.trim() || null,
      taxId: formValues.taxId.trim() || null,
      notes: formValues.notes.trim() || null,
    };
    const request = editingId
      ? HttpService.putWithAuth(`/customers/${editingId}`, body)
      : HttpService.postWithAuth('/customers', body);
    request
      .then(() => {
        enqueueSnackbar(editingId ? t('snackbar.customerUpdated') : t('snackbar.customerCreated'), { variant: 'success' });
        setDialogOpen(false);
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.saveFailed'), { variant: 'error' });
      })
      .finally(() => setSubmitting(false));
  };

  const openLink = (customer) => {
    setLinkCustomerId(customer.id);
    setLinkIban(customer.linkedWalletIban || '');
    setLinkDialogOpen(true);
    closeMenu();
  };

  const submitLink = (event) => {
    event.preventDefault();
    const iban = linkIban.trim();
    if (!iban) {
      enqueueSnackbar(t('snackbar.enterAccountNumber'), { variant: 'warning' });
      return;
    }
    setSubmitting(true);
    HttpService.postWithAuth(`/customers/${linkCustomerId}/link-wallet`, { iban })
      .then(() => {
        enqueueSnackbar(t('snackbar.walletLinked'), { variant: 'success' });
        setLinkDialogOpen(false);
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.linkFailed'), { variant: 'error' });
      })
      .finally(() => setSubmitting(false));
  };

  const unlink = (customer) => {
    closeMenu();
    HttpService.postWithAuth(`/customers/${customer.id}/unlink-wallet`, null)
      .then(() => {
        enqueueSnackbar(t('snackbar.walletUnlinked'), { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.unlinkFailed'), { variant: 'error' });
      });
  };

  const archive = (customer) => {
    closeMenu();
    HttpService.postWithAuth(`/customers/${customer.id}/archive`, null)
      .then(() => {
        enqueueSnackbar(t('snackbar.customerArchived'), { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.archiveFailed'), { variant: 'error' });
      });
  };

  const openMenu = (event, customer) => {
    setMenuAnchor(event.currentTarget);
    setMenuCustomer(customer);
  };

  const closeMenu = () => {
    setMenuAnchor(null);
    setMenuCustomer(null);
  };

  return (
    <>
      <Helmet>
        <title>{t('helmet')}</title>
      </Helmet>
      <Container sx={{ minWidth: '100%' }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between" mb={3} spacing={2}>
          <Stack spacing={0.5}>
            <Typography variant="h4">{t('title')}</Typography>
            <Typography variant="body2" color="text.secondary">
              {t('subtitle')}
            </Typography>
          </Stack>
          <Button
            variant="contained"
            startIcon={<Iconify icon="eva:plus-fill" />}
            onClick={openCreate}
            sx={{ cursor: 'pointer', flexShrink: 0 }}
          >
            {t('actions.newCustomer')}
          </Button>
        </Stack>

        <Typography
          component="span"
          role="status"
          aria-atomic="true"
          sx={{
            position: 'absolute',
            width: 1,
            height: 1,
            padding: 0,
            margin: -1,
            overflow: 'hidden',
            clip: 'rect(0,0,0,0)',
            whiteSpace: 'nowrap',
            border: 0,
          }}
        >
          {directoryStats.activeCount === 1
            ? t('stats.activeCountOne')
            : t('stats.activeCountMany', { count: directoryStats.activeCount })}
        </Typography>

        <Box sx={{ ...FOUR_COL_GRID_SX, mb: 3 }}>
          <OpsStatCard
            title={t('stats.active')}
            value={directoryStats.activeCount}
            format="count"
            icon="eva:people-outline"
            color="success"
            onClick={() => toggleStatusFilter('ACTIVE')}
            sx={statusFilter === 'ACTIVE' ? ACTIVE_STAT_SX : undefined}
          />
          <OpsStatCard
            title={t('stats.archived')}
            value={directoryStats.archivedCount}
            format="count"
            icon="eva:archive-outline"
            color="warning"
            onClick={() => toggleStatusFilter('ARCHIVED')}
            sx={statusFilter === 'ARCHIVED' ? ACTIVE_STAT_SX : undefined}
          />
          <OpsStatCard
            title={t('stats.linked')}
            value={directoryStats.linkedCount}
            format="count"
            icon="eva:link-2-outline"
            color="info"
            onClick={() => toggleLinkedFilter('linked')}
            sx={linkedFilter === 'linked' ? ACTIVE_STAT_SX : undefined}
          />
          <OpsStatCard
            title={t('stats.unlinked')}
            value={directoryStats.unlinkedCount}
            format="count"
            icon="eva:link-break-outline"
            color="secondary"
            onClick={() => toggleLinkedFilter('unlinked')}
            sx={linkedFilter === 'unlinked' ? ACTIVE_STAT_SX : undefined}
          />
        </Box>

        <Card sx={{ borderRadius: 2 }} aria-busy={loading || undefined}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            alignItems={{ xs: 'stretch', md: 'center' }}
            flexWrap="wrap"
            sx={{ px: 2.5, py: 2, borderBottom: (theme) => `1px solid ${theme.palette.divider}` }}
          >
            <TextField
              size="small"
              label={t('common:filters.search')}
              placeholder={t('filters.searchPlaceholder')}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  runSearch();
                }
              }}
              sx={{ minWidth: 200, maxWidth: 280 }}
            />
            <Button variant="outlined" onClick={runSearch} sx={{ cursor: 'pointer' }}>
              {t('common:actions.search')}
            </Button>
            <TextField
              select
              size="small"
              label={t('common:filters.status')}
              value={statusFilter}
              onChange={(event) => {
                setStatusFilter(event.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 160 }}
            >
              {statusOptions.map((opt) => (
                <MenuItem key={opt.value} value={opt.value}>
                  {opt.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              size="small"
              label={t('common:filters.linked')}
              value={linkedFilter}
              onChange={(event) => {
                setLinkedFilter(event.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 160 }}
            >
              {linkedOptions.map((opt) => (
                <MenuItem key={opt.value || 'all-links'} value={opt.value}>
                  {opt.label}
                </MenuItem>
              ))}
            </TextField>
            {filtersActive && (
              <Button
                color="inherit"
                onClick={clearFilters}
                startIcon={<Iconify icon="eva:close-fill" />}
                sx={{ alignSelf: { xs: 'flex-start', md: 'center' }, cursor: 'pointer' }}
              >
                {t('common:actions.clear')}
              </Button>
            )}
          </Stack>

          {loading ? (
            <Typography variant="body2" color="text.secondary" sx={{ py: 6, textAlign: 'center' }}>
              {t('common:status.loading')}
            </Typography>
          ) : rows.length === 0 ? (
            <EmptyState
              title={appliedQuery ? t('empty.noSearch') : t('empty.none')}
              description={
                appliedQuery ? t('empty.noSearchDescription') : t('empty.noneDescription')
              }
              actionLabel={!appliedQuery ? t('actions.newCustomer') : undefined}
              onAction={!appliedQuery ? openCreate : undefined}
            />
          ) : filteredRows.length === 0 ? (
            <EmptyState
              icon="eva:search-outline"
              title={t('empty.noFilter')}
              description={t('empty.noFilterDescription')}
            />
          ) : (
            <>
              <Scrollbar>
                <TableContainer sx={{ minWidth: 720 }}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>{t('common:fields.name')}</TableCell>
                        <TableCell>{t('table.phone')}</TableCell>
                        <TableCell>{t('common:fields.email')}</TableCell>
                        <TableCell>{t('table.linkedAccount')}</TableCell>
                        <TableCell>{t('common:filters.status')}</TableCell>
                        <TableCell align="right" />
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {pagedRows.map((row) => (
                        <TableRow key={row.id} hover>
                          <TableCell>
                            <Typography variant="subtitle2">{row.name}</Typography>
                            {row.taxId && (
                              <Typography variant="caption" color="text.secondary">
                                {t('table.taxIdPrefix', { value: row.taxId })}
                              </Typography>
                            )}
                          </TableCell>
                          <TableCell>{row.phone || '—'}</TableCell>
                          <TableCell>{row.email || '—'}</TableCell>
                          <TableCell>
                            {row.linkedWalletIban ? (
                              <Stack spacing={0.25}>
                                <Typography variant="body2">{row.linkedWalletName || t('walletNameFallback')}</Typography>
                                <Typography variant="caption" color="text.secondary">
                                  {row.linkedWalletIban}
                                </Typography>
                              </Stack>
                            ) : (
                              '—'
                            )}
                          </TableCell>
                          <TableCell>
                            <Label color={row.status === 'ACTIVE' ? 'success' : 'default'}>{row.status}</Label>
                          </TableCell>
                          <TableCell align="right">
                            <IconButton onClick={(e) => openMenu(e, row)} sx={{ cursor: 'pointer' }}>
                              <Iconify icon="eva:more-vertical-fill" />
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Scrollbar>
              <TablePagination
                rowsPerPageOptions={ROWS_PER_PAGE_OPTIONS}
                component="div"
                count={filteredRows.length}
                rowsPerPage={rowsPerPage}
                page={page}
                onPageChange={(_, newPage) => setPage(newPage)}
                onRowsPerPageChange={(event) => {
                  setRowsPerPage(parseInt(event.target.value, 10));
                  setPage(0);
                }}
                labelRowsPerPage={t('pagination.rowsPerPage')}
                sx={PAGINATION_SX}
              />
            </>
          )}
        </Card>
      </Container>

      <Popover
        open={Boolean(menuAnchor)}
        anchorEl={menuAnchor}
        onClose={closeMenu}
        anchorOrigin={{ vertical: 'top', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <MenuItem onClick={() => openEdit(menuCustomer)} sx={{ cursor: 'pointer' }}>
          <Iconify icon="eva:edit-fill" sx={{ mr: 2 }} />
          {t('common:actions.edit')}
        </MenuItem>
        <MenuItem onClick={() => openLink(menuCustomer)} sx={{ cursor: 'pointer' }}>
          <Iconify icon="eva:link-2-fill" sx={{ mr: 2 }} />
          {t('actions.linkWallet')}
        </MenuItem>
        {menuCustomer?.linkedWalletIban && (
          <MenuItem onClick={() => unlink(menuCustomer)} sx={{ cursor: 'pointer' }}>
            <Iconify icon="eva:link-break-fill" sx={{ mr: 2 }} />
            {t('actions.unlinkWallet')}
          </MenuItem>
        )}
        {menuCustomer?.status === 'ACTIVE' && (
          <MenuItem onClick={() => archive(menuCustomer)} sx={{ color: 'error.main', cursor: 'pointer' }}>
            <Iconify icon="eva:trash-2-outline" sx={{ mr: 2 }} />
            {t('actions.archive')}
          </MenuItem>
        )}
      </Popover>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editingId ? t('actions.edit') : t('actions.newCustomer')}</DialogTitle>
        <DialogContent>
          <Stack component="form" id="customer-form" onSubmit={saveCustomer} spacing={2} sx={{ pt: 1 }} noValidate>
            <TextField
              name="name"
              label={t('common:fields.name')}
              required
              value={formValues.name}
              onChange={handleFormChange}
              inputProps={{ maxLength: 100 }}
            />
            <TextField
              name="phone"
              label={t('table.phone')}
              value={formValues.phone}
              onChange={handleFormChange}
              inputProps={{ maxLength: 20 }}
            />
            <TextField
              name="email"
              label={t('common:fields.email')}
              type="email"
              value={formValues.email}
              onChange={handleFormChange}
              inputProps={{ maxLength: 100 }}
            />
            <TextField
              name="taxId"
              label={t('common:fields.taxId')}
              value={formValues.taxId}
              onChange={handleFormChange}
              inputProps={{ maxLength: 20 }}
            />
            <TextField
              name="notes"
              label={t('common:fields.notes')}
              multiline
              minRows={2}
              value={formValues.notes}
              onChange={handleFormChange}
              inputProps={{ maxLength: 500 }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)} sx={{ cursor: 'pointer' }}>
            {t('common:actions.cancel')}
          </Button>
          <LoadingButton type="submit" form="customer-form" variant="contained" loading={submitting} sx={{ cursor: 'pointer' }}>
            {t('common:actions.save')}
          </LoadingButton>
        </DialogActions>
      </Dialog>

      <Dialog open={linkDialogOpen} onClose={() => setLinkDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>{t('dialogs.linkTitle')}</DialogTitle>
        <DialogContent>
          <Stack component="form" id="link-form" onSubmit={submitLink} spacing={2} sx={{ pt: 1 }} noValidate>
            <TextField
              label={t('dialogs.accountNumber')}
              value={linkIban}
              onChange={(e) => setLinkIban(e.target.value)}
              placeholder={t('dialogs.accountPlaceholder')}
              required
              helperText={t('dialogs.accountHelper')}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setLinkDialogOpen(false)} sx={{ cursor: 'pointer' }}>
            {t('common:actions.cancel')}
          </Button>
          <LoadingButton type="submit" form="link-form" variant="contained" loading={submitting} sx={{ cursor: 'pointer' }}>
            {t('actions.link')}
          </LoadingButton>
        </DialogActions>
      </Dialog>
    </>
  );
}
