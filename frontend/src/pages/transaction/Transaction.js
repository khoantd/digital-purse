import {
  Box,
  Button,
  Card,
  Chip,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  MenuItem,
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
import { sentenceCase } from 'change-case';
import { enqueueSnackbar } from 'notistack';
import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Iconify from '../../components/iconify';
import Label from '../../components/label';
import Scrollbar from '../../components/scrollbar';
import { EmptyState, MoneyText, OpsStatCard } from '../../components/wallet-ui';
import AuthService from '../../services/AuthService';
import HttpService from '../../services/HttpService';
import OrganizationContext from '../../services/OrganizationContext';
import { ensureActiveOrganization } from '../../services/ensureOrganization';
import { fCurrency } from '../../utils/formatNumber';
import { fDateTime, parseDateInputEnd, parseDateInputStart, toDate } from '../../utils/formatTime';
import { createIdempotencyKey } from '../../utils/idempotency';
import TransactionListHead from './TransactionListHead';

const STATS_ZONE = 'Asia/Ho_Chi_Minh';

const FOUR_COL_GRID_SX = {
  display: 'grid',
  gap: 2,
  gridTemplateColumns: {
    xs: '1fr',
    sm: 'repeat(2, 1fr)',
    md: 'repeat(4, 1fr)',
  },
};

function calendarDateInZone(date, timeZone) {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);
}

function isTodayInStatsZone(value) {
  const created = toDate(value);
  if (!created) return false;
  const today = calendarDateInZone(new Date(), STATS_ZONE);
  return calendarDateInZone(created, STATS_ZONE) === today;
}

function sumAmounts(rows) {
  return rows.reduce((sum, row) => sum + (Number(row.amount) || 0), 0);
}

function rowsOfType(rows, typeName) {
  return rows.filter((row) => (row.type?.name || '') === typeName);
}

function matchesSpendFilters(row, { typeFilter, statusFilter, dateFrom, dateTo, walletIban }) {
  if (statusFilter === 'SUCCESS' || statusFilter === 'ERROR') {
    return false;
  }
  if (typeFilter === 'Top-up') {
    return false;
  }
  if (typeFilter === 'Reverse') {
    return (row.operation || '').toUpperCase() === 'REVERSE';
  }
  if (typeFilter === 'Transfer' && (row.operation || '').toUpperCase() !== 'TRANSFER') {
    return false;
  }
  if (typeFilter === 'Withdraw' && (row.operation || '').toUpperCase() !== 'WITHDRAW') {
    return false;
  }
  if (walletIban) {
    if (row.fromWalletIban !== walletIban && row.toWalletIban !== walletIban) {
      return false;
    }
  }
  const created = toDate(row.createdAt);
  if (!created) {
    return !(dateFrom || dateTo);
  }
  const from = parseDateInputStart(dateFrom);
  if (from && created < from) return false;
  const to = parseDateInputEnd(dateTo);
  if (to && created > to) return false;
  return true;
}

const TABLE_HEAD = [
  { id: 'type', label: 'Type', alignRight: false, firstColumn: true },
  { id: 'fromWallet', label: 'From', alignRight: false },
  { id: 'toWallet', label: 'To', alignRight: false },
  { id: 'amount', label: 'Amount', alignRight: true },
  { id: 'description', label: 'Description', alignRight: false },
  { id: 'createdAt', label: 'When', alignRight: false },
  { id: 'status', label: 'Status', alignRight: false },
  { id: 'actions', label: '', alignRight: true },
];

const TYPE_OPTIONS = [
  { value: '', label: 'All types' },
  { value: 'Transfer', label: 'Transfer' },
  { value: 'Withdraw', label: 'Withdraw' },
  { value: 'Top-up', label: 'Top-up' },
  { value: 'Reverse', label: 'Reverse' },
];

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'SUCCESS', label: 'Success' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'ERROR', label: 'Error' },
];

function typeTone(typeName) {
  const name = (typeName || '').toLowerCase();
  if (name.includes('withdraw')) return 'debit';
  if (name.includes('top-up') || name.includes('topup') || name.includes('add') || name.includes('deposit')) {
    return 'credit';
  }
  return 'neutral';
}

function typeColor(typeName) {
  const name = (typeName || '').toLowerCase();
  if (name.includes('withdraw')) return 'warning';
  if (name.includes('top-up') || name.includes('topup') || name.includes('add') || name.includes('deposit')) {
    return 'success';
  }
  if (name.includes('reverse')) return 'secondary';
  if (name.includes('transfer')) return 'info';
  return 'default';
}

function typeIcon(typeName) {
  const name = (typeName || '').toLowerCase();
  if (name.includes('withdraw')) return 'eva:arrow-upward-fill';
  if (name.includes('top-up') || name.includes('topup') || name.includes('add') || name.includes('deposit')) {
    return 'eva:arrow-downward-fill';
  }
  if (name.includes('reverse')) return 'eva:undo-outline';
  return 'eva:swap-outline';
}

function activityTone(row) {
  const typeName = row.type?.name || '';
  const fromType = typeTone(typeName);
  if (fromType !== 'neutral') return fromType;
  const desc = (row.description || '').toLowerCase();
  if (desc.includes('withdraw')) return 'debit';
  if (desc.includes('add') || desc.includes('deposit') || desc.includes('initial') || desc.includes('top-up') || desc.includes('topup')) {
    return 'credit';
  }
  return 'neutral';
}

function activityColor(row) {
  const typeName = row.type?.name || '';
  const fromType = typeColor(typeName);
  if (fromType !== 'default') return fromType;
  const tone = activityTone(row);
  if (tone === 'credit') return 'success';
  if (tone === 'debit') return 'warning';
  return fromType;
}

function statusColor(status) {
  if (status === 'SUCCESS') return 'success';
  if (status === 'ERROR') return 'error';
  return 'warning';
}

/** Prefer wallet name from WalletResponse; fall back to owner then account id. */
function walletCell(wallet) {
  if (!wallet) {
    return { title: '—', subtitle: '' };
  }
  const title = wallet.name || wallet.user?.fullName || 'Wallet';
  const account = wallet.iban
    ? wallet.iban.length > 12
      ? `${wallet.iban.slice(0, 4)}…${wallet.iban.slice(-4)}`
      : wallet.iban
    : '';
  const owner =
    wallet.user?.fullName ||
    [wallet.user?.firstName, wallet.user?.lastName].filter(Boolean).join(' ');
  const subtitle = [account, owner].filter(Boolean).join(' · ');
  return { title, subtitle };
}

function matchesFilters(row, { typeFilter, statusFilter, dateFrom, dateTo, walletId }) {
  if (walletId) {
    const fromId = row.fromWallet?.id;
    const toId = row.toWallet?.id;
    if (String(fromId) !== String(walletId) && String(toId) !== String(walletId)) {
      return false;
    }
  }
  if (typeFilter) {
    const typeName = row.type?.name || '';
    if (typeName !== typeFilter) return false;
  }
  if (statusFilter && row.status !== statusFilter) {
    return false;
  }
  const created = toDate(row.createdAt);
  if (!created) {
    return !(dateFrom || dateTo);
  }
  const from = parseDateInputStart(dateFrom);
  if (from && created < from) return false;
  const to = parseDateInputEnd(dateTo);
  if (to && created > to) return false;
  return true;
}

function walletMetaFromRows(rows, walletId) {
  if (!walletId) return { name: '', iban: '' };
  const fromMatch = rows.find((row) => String(row.fromWallet?.id) === String(walletId));
  if (fromMatch?.fromWallet) {
    return {
      name: fromMatch.fromWallet.name || `Wallet ${walletId}`,
      iban: fromMatch.fromWallet.iban || '',
    };
  }
  const toMatch = rows.find((row) => String(row.toWallet?.id) === String(walletId));
  if (toMatch?.toWallet) {
    return {
      name: toMatch.toWallet.name || `Wallet ${walletId}`,
      iban: toMatch.toWallet.iban || '',
    };
  }
  return { name: `Wallet ${walletId}`, iban: '' };
}

export default function Transaction() {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [data, setData] = useState([]);
  const [spendRequests, setSpendRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [canReverse, setCanReverse] = useState(false);
  const [reverseTarget, setReverseTarget] = useState(null);
  const [reversing, setReversing] = useState(false);
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const isAdmin = AuthService.isAdmin();
  const walletId = searchParams.get('walletId') || '';
  const walletNameParam = searchParams.get('walletName') || '';

  const filtersActive = Boolean(typeFilter || statusFilter || dateFrom || dateTo || walletId);

  const filteredData = useMemo(
    () =>
      data.filter((row) =>
        matchesFilters(row, { typeFilter, statusFilter, dateFrom, dateTo, walletId })
      ),
    [data, typeFilter, statusFilter, dateFrom, dateTo, walletId]
  );

  const walletMeta = useMemo(() => walletMetaFromRows(data, walletId), [data, walletId]);
  const walletChipLabel = walletId
    ? walletMeta.iban
      ? walletMeta.name
      : walletNameParam || walletMeta.name
    : '';

  const filterStats = useMemo(() => {
    const transfers = rowsOfType(filteredData, 'Transfer');
    const withdraws = rowsOfType(filteredData, 'Withdraw');
    const receives = rowsOfType(filteredData, 'Top-up');
    const todayReceiveTotal = sumAmounts(receives.filter((row) => isTodayInStatsZone(row.createdAt)));
    const walletIban = walletId ? walletMeta.iban : '';
    const pendingApprovals = spendRequests.filter(
      (row) =>
        (row.status || '').toUpperCase() === 'PENDING' &&
        matchesSpendFilters(row, {
          typeFilter,
          statusFilter,
          dateFrom,
          dateTo,
          walletIban: walletId ? walletIban || '__no_match__' : '',
        })
    ).length;

    return {
      transferTotal: sumAmounts(transfers),
      withdrawTotal: sumAmounts(withdraws),
      receiveTotal: sumAmounts(receives),
      todayReceiveTotal,
      pendingApprovals,
    };
  }, [filteredData, spendRequests, typeFilter, statusFilter, dateFrom, dateTo, walletId, walletMeta.iban]);

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event) => {
    setPage(0);
    setRowsPerPage(parseInt(event.target.value, 10));
  };

  const clearWalletFilter = () => {
    const next = new URLSearchParams(searchParams);
    next.delete('walletId');
    next.delete('walletName');
    setSearchParams(next, { replace: true });
    setPage(0);
  };

  const clearFilters = () => {
    setTypeFilter('');
    setStatusFilter('');
    setDateFrom('');
    setDateTo('');
    if (walletId) {
      clearWalletFilter();
    } else {
      setPage(0);
    }
  };

  useEffect(() => {
    fetchData();
    const refreshRole = () => {
      ensureActiveOrganization()
        .then((list) => {
          const activeId = OrganizationContext.getActiveOrganizationId();
          const active = list.find((o) => o.id === activeId);
          const role = (active?.myRole || '').toUpperCase();
          setCanReverse(role === 'OWNER' || role === 'ADMIN' || isAdmin);
        })
        .catch(() => setCanReverse(isAdmin));
    };
    refreshRole();
    const onOrg = () => {
      fetchData();
      refreshRole();
    };
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    setPage(0);
  }, [walletId]);

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(filteredData.length / rowsPerPage) - 1);
    if (page > maxPage) {
      setPage(maxPage);
    }
  }, [filteredData.length, rowsPerPage, page]);

  const fetchData = () => {
    const userId = AuthService.getCurrentUser()?.id;
    const url = isAdmin ? '/transactions?page=0&size=1000' : `/transactions/users/${userId}`;
    setLoading(true);
    const txPromise = HttpService.getListWithAuth(url);
    const spendPromise = HttpService.getListWithAuth('/spend-requests').catch(() => []);

    Promise.all([txPromise, spendPromise])
      .then(([list, spendList]) => {
        setData(list);
        setSpendRequests(Array.isArray(spendList) ? spendList : []);
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

  const confirmReverse = () => {
    if (!reverseTarget?.id) return;
    setReversing(true);
    HttpService.postWithAuth(`/transactions/${reverseTarget.id}/reverse`, null, {
      'Idempotency-Key': createIdempotencyKey(),
    })
      .then((response) => {
        const status = response?.status || response?.data?.status;
        if (status === 'PENDING_APPROVAL') {
          enqueueSnackbar('Reverse submitted for dual-control approval', { variant: 'info' });
          setReverseTarget(null);
          navigate('/approvals');
          return;
        }
        enqueueSnackbar('Transaction reversed', { variant: 'success' });
        setReverseTarget(null);
        fetchData();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Reverse failed', { variant: 'error' });
      })
      .finally(() => setReversing(false));
  };

  const listTitle = isAdmin ? 'All Transactions' : 'Transactions';

  return (
    <>
      <Helmet>
        <title> Transactions | Digital Purse </title>
      </Helmet>
      <Container sx={{ minWidth: '100%' }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between" mb={3}>
          <Stack spacing={0.5}>
            <Typography variant="h4">{listTitle}</Typography>
            <Typography variant="body2" color="text.secondary">
              {loading
                ? 'Loading…'
                : filteredData.length === 1
                  ? '1 transaction'
                  : `${filteredData.length} transactions`}
              {filtersActive && data.length > 0 ? ` (of ${data.length})` : ''}
            </Typography>
          </Stack>
        </Stack>

        <Box sx={{ ...FOUR_COL_GRID_SX, mb: 3 }}>
          <OpsStatCard
            title="Transferred"
            value={filterStats.transferTotal}
            icon="eva:swap-outline"
            color="info"
          />
          <OpsStatCard
            title="Withdrawn"
            value={filterStats.withdrawTotal}
            icon="eva:arrow-upward-fill"
            color="warning"
          />
          <OpsStatCard
            title="Received"
            value={filterStats.receiveTotal}
            icon="eva:arrow-downward-fill"
            color="success"
            subtitle={
              filterStats.todayReceiveTotal
                ? `Today ${fCurrency(filterStats.todayReceiveTotal)}`
                : undefined
            }
          />
          <OpsStatCard
            title="Pending approvals"
            value={filterStats.pendingApprovals}
            format="count"
            icon="eva:checkmark-circle-2-outline"
            color="error"
            onClick={() => navigate('/approvals')}
          />
        </Box>

        <Card sx={{ borderRadius: 2 }}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            alignItems={{ xs: 'stretch', md: 'center' }}
            flexWrap="wrap"
            sx={{ px: 2.5, py: 2, borderBottom: (theme) => `1px solid ${theme.palette.divider}` }}
          >
            {walletId && (
              <Chip
                label={walletChipLabel}
                onDelete={clearWalletFilter}
                color="primary"
                variant="outlined"
                icon={<Iconify icon="ant-design:wallet-outlined" width={16} height={16} />}
                sx={{ alignSelf: { xs: 'flex-start', md: 'center' } }}
              />
            )}
            <TextField
              select
              size="small"
              label="Type"
              value={typeFilter}
              onChange={(event) => {
                setTypeFilter(event.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 160 }}
            >
              {TYPE_OPTIONS.map((opt) => (
                <MenuItem key={opt.value || 'all-types'} value={opt.value}>
                  {opt.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              size="small"
              label="Status"
              value={statusFilter}
              onChange={(event) => {
                setStatusFilter(event.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 160 }}
            >
              {STATUS_OPTIONS.map((opt) => (
                <MenuItem key={opt.value || 'all-statuses'} value={opt.value}>
                  {opt.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              size="small"
              type="date"
              label="From"
              value={dateFrom}
              onChange={(event) => {
                setDateFrom(event.target.value);
                setPage(0);
              }}
              InputLabelProps={{ shrink: true }}
              sx={{ minWidth: 160 }}
            />
            <TextField
              size="small"
              type="date"
              label="To"
              value={dateTo}
              onChange={(event) => {
                setDateTo(event.target.value);
                setPage(0);
              }}
              InputLabelProps={{ shrink: true }}
              inputProps={{ min: dateFrom || undefined }}
              sx={{ minWidth: 160 }}
            />
            {filtersActive && (
              <Button
                color="inherit"
                onClick={clearFilters}
                startIcon={<Iconify icon="eva:close-fill" />}
                sx={{ alignSelf: { xs: 'flex-start', md: 'center' }, cursor: 'pointer' }}
              >
                Clear
              </Button>
            )}
          </Stack>

          {loading ? (
            <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
              Loading…
            </Typography>
          ) : data.length === 0 ? (
            <EmptyState
              icon="eva:list-outline"
              title="No activity yet"
              description="Transfers, deposits, and withdrawals will show up here."
              actionLabel="Go to transfers"
              onAction={() => navigate('/transfers')}
            />
          ) : filteredData.length === 0 ? (
            <EmptyState
              icon="eva:search-outline"
              title="No transactions match your filters"
              description="Try a different wallet, type, status, or date range, or clear the filters above."
            />
          ) : (
            <>
              <Scrollbar>
                <TableContainer sx={{ minWidth: 800 }}>
                  <Table>
                    <TransactionListHead headLabel={TABLE_HEAD} />
                    <TableBody>
                      {filteredData.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage).map((row) => {
                        const { id, amount, description, createdAt, fromWallet, toWallet, type, status } = row;
                        const typeName = type?.name || 'Transfer';
                        const color = activityColor(row);
                        const from = walletCell(fromWallet);
                        const to = walletCell(toWallet);
                        const showReverse =
                          canReverse &&
                          row.reversible === true &&
                          !row.reversedByTransactionId &&
                          (typeName === 'Transfer' || typeName === 'Withdraw' || typeName === 'Top-up');
                        return (
                          <TableRow hover key={id} tabIndex={-1}>
                            <TableCell align="left" sx={{ pl: 3 }}>
                              <Stack direction="row" spacing={1.5} alignItems="center">
                                <Box
                                  sx={{
                                    width: 36,
                                    height: 36,
                                    borderRadius: '50%',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    bgcolor: `${color}.lighter`,
                                    color: `${color}.dark`,
                                  }}
                                >
                                  <Iconify icon={typeIcon(typeName)} width={18} height={18} />
                                </Box>
                                <Stack spacing={0.25}>
                                  <Label color={color} variant="soft">
                                    {typeName}
                                  </Label>
                                  {row.reversesTransactionId && (
                                    <Typography variant="caption" color="text.secondary">
                                      of #{row.reversesTransactionId}
                                    </Typography>
                                  )}
                                  {row.reversedByTransactionId && (
                                    <Typography variant="caption" color="text.secondary">
                                      reversed by #{row.reversedByTransactionId}
                                    </Typography>
                                  )}
                                </Stack>
                              </Stack>
                            </TableCell>
                            <TableCell align="left">
                              <Typography variant="body2">{from.title}</Typography>
                              {from.subtitle && (
                                <Typography variant="caption" color="text.secondary" display="block">
                                  {from.subtitle}
                                </Typography>
                              )}
                            </TableCell>
                            <TableCell align="left">
                              <Typography variant="body2">{to.title}</Typography>
                              {to.subtitle && (
                                <Typography variant="caption" color="text.secondary" display="block">
                                  {to.subtitle}
                                </Typography>
                              )}
                            </TableCell>
                            <TableCell align="right">
                              <MoneyText amount={amount} tone={activityTone(row)} />
                            </TableCell>
                            <TableCell align="left">
                              <Typography variant="body2" color="text.secondary" noWrap sx={{ maxWidth: 220 }}>
                                {description || '—'}
                              </Typography>
                            </TableCell>
                            <TableCell align="left">
                              <Typography variant="body2">{fDateTime(createdAt)}</Typography>
                            </TableCell>
                            <TableCell align="left">
                              <Label color={statusColor(status)}>
                                {sentenceCase(status || 'unknown')}
                              </Label>
                            </TableCell>
                            <TableCell align="right">
                              {showReverse && (
                                <Button
                                  size="small"
                                  color="inherit"
                                  variant="outlined"
                                  startIcon={<Iconify icon="eva:undo-outline" />}
                                  onClick={() => setReverseTarget(row)}
                                  sx={{ cursor: 'pointer' }}
                                >
                                  Reverse
                                </Button>
                              )}
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Scrollbar>
              <TablePagination
                rowsPerPageOptions={[5, 10, 25]}
                component="div"
                count={filteredData.length}
                rowsPerPage={rowsPerPage}
                page={page}
                onPageChange={handleChangePage}
                onRowsPerPageChange={handleChangeRowsPerPage}
                labelRowsPerPage="Transactions per page"
                sx={{
                  borderTop: (theme) => `1px solid ${theme.palette.divider}`,
                  cursor: 'pointer',
                  '& .MuiTablePagination-actions button': { cursor: 'pointer' },
                  '& .MuiInputBase-root': { cursor: 'pointer' },
                }}
              />
            </>
          )}
        </Card>

        <Dialog open={Boolean(reverseTarget)} onClose={() => !reversing && setReverseTarget(null)}>
          <DialogTitle>Reverse transaction?</DialogTitle>
          <DialogContent>
            <DialogContentText>
              This creates a compensating reverse for #{reverseTarget?.id}
              {reverseTarget?.amount != null ? ` (${fCurrency(reverseTarget.amount)})` : ''}. The original
              record stays in history. Large amounts may need dual-control approval.
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setReverseTarget(null)} disabled={reversing} color="inherit">
              Cancel
            </Button>
            <Button onClick={confirmReverse} disabled={reversing} variant="contained" color="warning">
              {reversing ? 'Reversing…' : 'Reverse'}
            </Button>
          </DialogActions>
        </Dialog>
      </Container>
    </>
  );
}
