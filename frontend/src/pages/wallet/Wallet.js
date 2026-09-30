import { LoadingButton } from '@mui/lab';
import {
  Box,
  Button,
  Card,
  Chip,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  IconButton,
  MenuItem,
  Popover,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { enqueueSnackbar } from 'notistack';
import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import Iconify from '../../components/iconify';
import Scrollbar from '../../components/scrollbar';
import { EmptyState, OpsStatCard, WalletCard } from '../../components/wallet-ui';
import AuthService from '../../services/AuthService';
import HttpService from '../../services/HttpService';
import { fCurrency } from '../../utils/formatNumber';
import WalletListHead from './WalletListHead';

const ROWS_PER_PAGE_OPTIONS = [5, 10, 25];
const PAGINATION_SX = {
  borderTop: (theme) => `1px solid ${theme.palette.divider}`,
  mt: 2,
  cursor: 'pointer',
  '& .MuiTablePagination-actions button': { cursor: 'pointer' },
  '& .MuiInputBase-root': { cursor: 'pointer' },
};

const THREE_COL_GRID_SX = {
  display: 'grid',
  gap: 2,
  gridTemplateColumns: {
    xs: '1fr',
    sm: 'repeat(2, 1fr)',
    md: 'repeat(3, 1fr)',
  },
};

const ACTIVE_STAT_SX = {
  boxShadow: (theme) => theme.customShadows.z8,
  outline: (theme) => `2px solid ${theme.palette.primary.main}`,
  outlineOffset: 1,
};

const SR_ONLY_SX = {
  position: 'absolute',
  width: 1,
  height: 1,
  padding: 0,
  margin: -1,
  overflow: 'hidden',
  clip: 'rect(0,0,0,0)',
  whiteSpace: 'nowrap',
  border: 0,
};

function slicePage(items, page, rowsPerPage) {
  return items.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);
}

function matchesTypeFilter(wallet, typeFilter) {
  if (!typeFilter) return true;
  if (typeFilter === 'CUSTOMER') return wallet.ownerType === 'CUSTOMER';
  return wallet.ownerType !== 'CUSTOMER';
}

function matchesSearch(wallet, appliedQuery) {
  const q = appliedQuery.trim().toLowerCase();
  if (!q) return true;
  const haystack = [wallet.name, wallet.iban, wallet.customerName]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
  return haystack.includes(q);
}

function transactionsPath(wallet) {
  return `/transactions?walletId=${wallet.id}&walletName=${encodeURIComponent(wallet.name || '')}`;
}

function DetailRow({ label, children }) {
  return (
    <Stack spacing={0.5}>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      {children}
    </Stack>
  );
}

function WalletCardGrid({ wallets, navigate, onDetails, onEdit }) {
  return (
    <Grid container spacing={2}>
      {wallets.map((wallet) => (
        <Grid item xs={12} sm={6} md={4} key={wallet.id}>
          <WalletCard
            name={wallet.name}
            balance={wallet.balance}
            iban={wallet.iban}
            currency={wallet.currency}
            ownerType={wallet.ownerType}
            customerName={wallet.customerName}
            onClick={() => navigate(transactionsPath(wallet))}
            onDetails={() => onDetails(wallet)}
            onEdit={() => onEdit(wallet)}
          />
        </Grid>
      ))}
    </Grid>
  );
}

function WalletTable({ wallets, selected, onOpenMenu, headLabel, typeLabel, actionsForLabel }) {
  return (
    <Scrollbar>
      <TableContainer sx={{ minWidth: 800 }}>
        <Table>
          <WalletListHead headLabel={headLabel} />
          <TableBody>
            {wallets.map((row) => {
              const { id, name, balance, currency, user, iban, ownerType, customerName } = row;
              const selectedRecord = selected.indexOf(name) !== -1;
              return (
                <TableRow hover key={id} tabIndex={-1} role="checkbox" selected={selectedRecord}>
                  <TableCell align="left" sx={{ paddingLeft: 5 }}>
                    {id}
                  </TableCell>
                  <TableCell align="left">
                    <Typography variant="subtitle2">{name}</Typography>
                  </TableCell>
                  <TableCell align="left">
                    <Typography variant="body2" noWrap>
                      {typeLabel(ownerType, customerName)}
                    </Typography>
                  </TableCell>
                  <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                    {fCurrency(balance)}
                  </TableCell>
                  <TableCell align="left">{currency || 'VND'}</TableCell>
                  <TableCell align="left">{user?.fullName}</TableCell>
                  <TableCell align="left">
                    <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: 13 }}>
                      {iban}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <IconButton
                      size="large"
                      color="inherit"
                      aria-label={actionsForLabel(name)}
                      onClick={(event) => onOpenMenu(event, row)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <Iconify icon="eva:more-vertical-fill" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </Scrollbar>
  );
}

export default function Wallet() {
  const { t } = useTranslation(['wallets', 'common']);
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuWallet, setMenuWallet] = useState(null);
  const [selected] = useState([]);
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [detailWallet, setDetailWallet] = useState(null);
  const [editWallet, setEditWallet] = useState(null);
  const [editName, setEditName] = useState('');
  const [saving, setSaving] = useState(false);
  const [query, setQuery] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const navigate = useNavigate();
  const isAdmin = AuthService.isAdmin();

  const tableHead = useMemo(
    () => [
      { id: 'id', label: t('wallets:table.id'), alignRight: false, firstColumn: true },
      { id: 'name', label: t('wallets:table.name'), alignRight: false },
      { id: 'ownerType', label: t('wallets:table.type'), alignRight: false },
      { id: 'balance', label: t('wallets:table.balance'), alignRight: true },
      { id: 'currency', label: t('wallets:table.currency'), alignRight: false },
      { id: 'userId', label: t('wallets:table.user'), alignRight: false },
      { id: 'iban', label: t('wallets:table.account'), alignRight: false },
      { id: '' },
    ],
    [t]
  );

  const typeOptions = useMemo(
    () => [
      { value: '', label: t('wallets:filters.typeAll') },
      { value: 'ORGANIZATION', label: t('wallets:filters.organization') },
      { value: 'CUSTOMER', label: t('wallets:filters.customer') },
    ],
    [t]
  );

  const resolveTypeLabel = (ownerType, customerName) => {
    if (ownerType === 'CUSTOMER') {
      return customerName
        ? t('common:ownerType.customerNamed', { name: customerName })
        : t('common:ownerType.customer');
    }
    return t('common:ownerType.organization');
  };

  const actionsForLabel = (name) => t('wallets:menu.actionsFor', { name });

  const walletStats = useMemo(() => {
    let organizationCount = 0;
    let customerCount = 0;
    data.forEach((wallet) => {
      if (wallet.ownerType === 'CUSTOMER') customerCount += 1;
      else organizationCount += 1;
    });
    return {
      totalCount: data.length,
      organizationCount,
      customerCount,
    };
  }, [data]);

  const filteredWallets = useMemo(
    () =>
      data.filter(
        (wallet) => matchesTypeFilter(wallet, typeFilter) && matchesSearch(wallet, appliedQuery)
      ),
    [data, typeFilter, appliedQuery]
  );

  const pagedWallets = slicePage(filteredWallets, page, rowsPerPage);

  const filtersActive = Boolean(appliedQuery.trim() || typeFilter);

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(filteredWallets.length / rowsPerPage) - 1);
    if (page > maxPage) {
      setPage(maxPage);
    }
  }, [filteredWallets.length, rowsPerPage, page]);

  const handleOpenMenu = (event, wallet) => {
    setMenuAnchor(event.currentTarget);
    setMenuWallet(wallet);
  };

  const handleCloseMenu = () => {
    setMenuAnchor(null);
    setMenuWallet(null);
  };

  const openDetails = (wallet) => {
    handleCloseMenu();
    setDetailWallet(wallet);
  };

  const openEdit = (wallet) => {
    handleCloseMenu();
    setEditWallet(wallet);
    setEditName(wallet?.name || '');
  };

  const closeDetails = () => setDetailWallet(null);

  const closeEdit = () => {
    if (saving) return;
    setEditWallet(null);
    setEditName('');
  };

  const copyIban = async (iban) => {
    if (!iban) return;
    try {
      await navigator.clipboard.writeText(iban);
      enqueueSnackbar(t('wallets:messages.accountCopied'), { variant: 'success' });
    } catch {
      enqueueSnackbar(t('wallets:messages.copyFailed'), { variant: 'error' });
    }
  };

  const saveRename = (event) => {
    event.preventDefault();
    if (!editWallet) return;

    const name = editName.trim();
    if (name.length < 3 || name.length > 50) {
      enqueueSnackbar(t('wallets:messages.nameLength'), { variant: 'warning' });
      return;
    }

    // Server update only renames; DTO still requires @Positive balance + ownerType.
    const balanceNum = Number(editWallet.balance);
    const payload = {
      name,
      balance: Number.isFinite(balanceNum) && balanceNum > 0 ? balanceNum : 1,
      ownerType: editWallet.ownerType || 'ORGANIZATION',
    };
    if (editWallet.ownerType === 'CUSTOMER' && editWallet.customerId) {
      payload.customerId = editWallet.customerId;
    }

    setSaving(true);
    HttpService.putWithAuth(`/wallets/${editWallet.id}`, payload)
      .then(() => {
        enqueueSnackbar(t('wallets:messages.walletUpdated'), { variant: 'success' });
        setEditWallet(null);
        setEditName('');
        fetchData();
      })
      .catch((error) => {
        if (error.response?.data?.errors) {
          error.response.data.errors.map((e) => enqueueSnackbar(e.message, { variant: 'error' }));
        } else if (error.response?.data?.message) {
          enqueueSnackbar(error.response.data.message, { variant: 'error' });
        } else {
          enqueueSnackbar(error.message, { variant: 'error' });
        }
      })
      .finally(() => setSaving(false));
  };

  useEffect(() => {
    fetchData();
    const onOrg = () => {
      setQuery('');
      setAppliedQuery('');
      setTypeFilter('');
      fetchData();
    };
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchData = () => {
    const url = '/wallets?page=0&size=1000';
    setLoading(true);
    HttpService.getListWithAuth(url)
      .then((list) => {
        setData(list);
        setPage(0);
      })
      .catch((error) => {
        if (error?.response?.status === 401) {
          navigate('/login');
        } else if (error.response?.data?.errors) {
          error.response?.data?.errors.map((e) => enqueueSnackbar(e.message, { variant: 'error' }));
        } else if (error.response?.data?.message) {
          enqueueSnackbar(error.response?.data?.message, { variant: 'error' });
        } else {
          enqueueSnackbar(error.message, { variant: 'error' });
        }
      })
      .finally(() => setLoading(false));
  };

  const runSearch = () => {
    setAppliedQuery(query.trim());
    setPage(0);
  };

  const clearFilters = () => {
    setQuery('');
    setAppliedQuery('');
    setTypeFilter('');
    setPage(0);
  };

  const toggleTypeFilter = (type) => {
    setTypeFilter((current) => (current === type ? '' : type));
    setPage(0);
  };

  const listTitle = isAdmin ? t('wallets:titleAdmin') : t('wallets:title');
  const listSubtitle = isAdmin ? t('wallets:subtitleAdmin') : t('wallets:subtitle');

  const pagination =
    filteredWallets.length > 0 ? (
      <TablePagination
        rowsPerPageOptions={ROWS_PER_PAGE_OPTIONS}
        component="div"
        count={filteredWallets.length}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={(_, newPage) => setPage(newPage)}
        onRowsPerPageChange={(event) => {
          setRowsPerPage(parseInt(event.target.value, 10));
          setPage(0);
        }}
        labelRowsPerPage={t('wallets:pagination.rowsPerPage')}
        sx={PAGINATION_SX}
      />
    ) : null;

  const renderListBody = () => {
    if (loading) {
      return (
        <Typography variant="body2" color="text.secondary" sx={{ py: 6, textAlign: 'center' }}>
          {t('common:status.loading')}
        </Typography>
      );
    }

    if (data.length === 0) {
      return (
        <EmptyState
          icon="ant-design:wallet-outlined"
          title={t('wallets:empty.none')}
          description={
            isAdmin ? t('wallets:empty.adminNoneDescription') : t('wallets:empty.noneDescriptionOrg')
          }
          actionLabel={isAdmin ? undefined : t('wallets:actions.newWallet')}
          onAction={isAdmin ? undefined : () => navigate('/wallets/new')}
        />
      );
    }

    if (filteredWallets.length === 0) {
      return (
        <EmptyState
          icon="eva:search-outline"
          title={t('wallets:empty.noMatch')}
          description={t('wallets:empty.filterHint')}
        />
      );
    }

    if (isAdmin) {
      return (
        <>
          <WalletTable
            wallets={pagedWallets}
            selected={selected}
            onOpenMenu={handleOpenMenu}
            headLabel={tableHead}
            typeLabel={resolveTypeLabel}
            actionsForLabel={actionsForLabel}
          />
          {pagination}
        </>
      );
    }

    return (
      <Box sx={{ px: 2.5, pb: 2.5, pt: 2 }}>
        <WalletCardGrid
          wallets={pagedWallets}
          navigate={navigate}
          onDetails={openDetails}
          onEdit={openEdit}
        />
        {pagination}
      </Box>
    );
  };

  return (
    <>
      <Helmet>
        <title>{t('wallets:helmet')}</title>
      </Helmet>
      <Container sx={{ minWidth: '100%' }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between" mb={3} spacing={2}>
          <Stack spacing={0.5}>
            <Typography variant="h4">{listTitle}</Typography>
            <Typography variant="body2" color="text.secondary">
              {listSubtitle}
            </Typography>
          </Stack>
          <Button
            variant="contained"
            startIcon={<Iconify icon="eva:plus-fill" />}
            onClick={() => navigate('/wallets/new')}
            sx={{ cursor: 'pointer', flexShrink: 0 }}
          >
            {t('wallets:actions.newWallet')}
          </Button>
        </Stack>

        <Typography component="span" role="status" aria-atomic="true" sx={SR_ONLY_SX}>
          {t('wallets:pagination.showing', {
            filtered: filteredWallets.length,
            total: walletStats.totalCount,
            unit:
              walletStats.totalCount === 1
                ? t('wallets:pagination.wallet')
                : t('wallets:pagination.wallets'),
          })}
        </Typography>

        <Box sx={{ ...THREE_COL_GRID_SX, mb: 3 }}>
          <OpsStatCard
            title={t('wallets:stats.total')}
            value={walletStats.totalCount}
            format="count"
            icon="ant-design:wallet-outlined"
            color="primary"
            onClick={() => {
              setTypeFilter('');
              setPage(0);
            }}
            sx={!typeFilter ? ACTIVE_STAT_SX : undefined}
          />
          <OpsStatCard
            title={t('wallets:stats.organization')}
            value={walletStats.organizationCount}
            format="count"
            icon="ant-design:bank-outlined"
            color="info"
            onClick={() => toggleTypeFilter('ORGANIZATION')}
            sx={typeFilter === 'ORGANIZATION' ? ACTIVE_STAT_SX : undefined}
          />
          <OpsStatCard
            title={t('wallets:stats.customer')}
            value={walletStats.customerCount}
            format="count"
            icon="eva:people-outline"
            color="secondary"
            onClick={() => toggleTypeFilter('CUSTOMER')}
            sx={typeFilter === 'CUSTOMER' ? ACTIVE_STAT_SX : undefined}
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
              placeholder={t('wallets:filters.searchPlaceholderExtended')}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  runSearch();
                }
              }}
              sx={{ minWidth: 200, maxWidth: 320 }}
            />
            <Button variant="outlined" onClick={runSearch} sx={{ cursor: 'pointer' }}>
              {t('common:actions.search')}
            </Button>
            <TextField
              select
              size="small"
              label={t('common:filters.type')}
              value={typeFilter}
              onChange={(event) => {
                setTypeFilter(event.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 160 }}
              inputProps={{ 'aria-label': t('wallets:filters.typeFilterAria') }}
            >
              {typeOptions.map((opt) => (
                <MenuItem key={opt.value || 'all-types'} value={opt.value}>
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

          {renderListBody()}
        </Card>
      </Container>

      <Popover
        open={Boolean(menuAnchor)}
        anchorEl={menuAnchor}
        onClose={handleCloseMenu}
        anchorOrigin={{ vertical: 'top', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{
          sx: {
            p: 1,
            width: 160,
            '& .MuiMenuItem-root': {
              px: 1,
              typography: 'body2',
              borderRadius: 0.75,
            },
          },
        }}
      >
        <MenuItem
          onClick={() => menuWallet && openDetails(menuWallet)}
          sx={{ cursor: 'pointer' }}
          disabled={!menuWallet}
        >
          <Iconify icon="eva:eye-outline" sx={{ mr: 2 }} />
          {t('common:actions.details')}
        </MenuItem>
        <MenuItem
          onClick={() => menuWallet && openEdit(menuWallet)}
          sx={{ cursor: 'pointer' }}
          disabled={!menuWallet}
        >
          <Iconify icon="eva:edit-fill" sx={{ mr: 2 }} />
          {t('common:actions.edit')}
        </MenuItem>
        <MenuItem disabled sx={{ color: 'error.main' }}>
          <Iconify icon="eva:trash-2-outline" sx={{ mr: 2 }} />
          {t('common:actions.delete')}
        </MenuItem>
      </Popover>

      <Dialog open={Boolean(detailWallet)} onClose={closeDetails} fullWidth maxWidth="sm">
        <DialogTitle>{t('wallets:dialog.detailsTitle')}</DialogTitle>
        <DialogContent>
          {detailWallet && (
            <Stack spacing={2.5} sx={{ pt: 1 }}>
              <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
                <Typography variant="h6">{detailWallet.name}</Typography>
                <Chip
                  size="small"
                  label={resolveTypeLabel(detailWallet.ownerType, detailWallet.customerName)}
                />
              </Stack>

              <DetailRow label={t('common:fields.balance')}>
                <Typography variant="h5" sx={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
                  {fCurrency(detailWallet.balance)}
                  <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                    {detailWallet.currency || 'VND'}
                  </Typography>
                </Typography>
              </DetailRow>

              <Divider />

              <DetailRow label={t('wallets:dialog.accountNumber')}>
                <Stack direction="row" alignItems="center" spacing={1}>
                  <Typography
                    variant="body2"
                    sx={{ fontFamily: 'monospace', fontSize: 14, wordBreak: 'break-all', flex: 1 }}
                  >
                    {detailWallet.iban || '—'}
                  </Typography>
                  {detailWallet.iban && (
                    <IconButton
                      size="small"
                      aria-label={t('wallets:dialog.copyAccount')}
                      onClick={() => copyIban(detailWallet.iban)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <Iconify icon="eva:copy-outline" width={18} height={18} />
                    </IconButton>
                  )}
                </Stack>
              </DetailRow>

              {detailWallet.user?.fullName && (
                <DetailRow label={t('wallets:dialog.ownerUser')}>
                  <Typography variant="body2">{detailWallet.user.fullName}</Typography>
                </DetailRow>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={closeDetails} sx={{ cursor: 'pointer' }}>
            {t('common:actions.close')}
          </Button>
          {detailWallet && (
            <Button
              variant="contained"
              onClick={() => {
                closeDetails();
                navigate(transactionsPath(detailWallet));
              }}
              sx={{ cursor: 'pointer' }}
            >
              {t('wallets:actions.viewTransactions')}
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(editWallet)} onClose={closeEdit} fullWidth maxWidth="xs">
        <DialogTitle>{t('wallets:dialog.editTitle')}</DialogTitle>
        <DialogContent>
          <Stack component="form" id="wallet-edit-form" onSubmit={saveRename} spacing={2} sx={{ pt: 1 }} noValidate>
            <TextField
              label={t('wallets:edit.walletName')}
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              required
              autoFocus
              inputProps={{ minLength: 3, maxLength: 50 }}
              helperText={t('wallets:edit.nameHelper')}
            />
            <TextField
              label={t('wallets:dialog.accountNumber')}
              value={editWallet?.iban || ''}
              disabled
              helperText={t('wallets:edit.accountHelper')}
            />
            <TextField
              label={t('common:fields.balance')}
              value={editWallet ? `${fCurrency(editWallet.balance)} ${editWallet.currency || 'VND'}` : ''}
              disabled
              helperText={t('wallets:edit.balanceHelper')}
            />
            <TextField
              label={t('wallets:edit.ownerType')}
              value={editWallet ? resolveTypeLabel(editWallet.ownerType, editWallet.customerName) : ''}
              disabled
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={closeEdit} disabled={saving} sx={{ cursor: 'pointer' }}>
            {t('common:actions.cancel')}
          </Button>
          <LoadingButton
            type="submit"
            form="wallet-edit-form"
            variant="contained"
            loading={saving}
            sx={{ cursor: 'pointer' }}
          >
            {t('common:actions.save')}
          </LoadingButton>
        </DialogActions>
      </Dialog>
    </>
  );
}
