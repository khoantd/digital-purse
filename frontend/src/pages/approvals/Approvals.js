import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Button,
  Card,
  Container,
  MenuItem,
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
import { Helmet } from 'react-helmet-async';
import { enqueueSnackbar } from 'notistack';
import { useNavigate } from 'react-router-dom';
import Iconify from '../../components/iconify';
import Label from '../../components/label';
import Scrollbar from '../../components/scrollbar';
import { EmptyState, MoneyText, OpsStatCard } from '../../components/wallet-ui';
import HttpService from '../../services/HttpService';
import { createIdempotencyKey } from '../../utils/idempotency';
import { fDateTime, parseDateInputEnd, parseDateInputStart, toDate } from '../../utils/formatTime';

function statusColor(status) {
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'error';
  if (status === 'PENDING') return 'warning';
  return 'default';
}

function maskAccount(account) {
  if (!account) return '—';
  if (account.length <= 12) return account;
  return `${account.slice(0, 4)}…${account.slice(-4)}`;
}

function operationLabel(operation, t) {
  const op = (operation || '').toUpperCase();
  if (op === 'TRANSFER') return t('operations.transfer');
  if (op === 'WITHDRAW') return t('operations.withdraw');
  if (op === 'REVERSE') return t('operations.reverse');
  return operation || '—';
}

function sumAmounts(list) {
  return list.reduce((total, row) => total + (Number(row.amount) || 0), 0);
}

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

function matchesFilters(row, { statusFilter, operationFilter, dateFrom, dateTo }) {
  if (statusFilter && (row.status || '').toUpperCase() !== statusFilter) {
    return false;
  }
  if (operationFilter && (row.operation || '').toUpperCase() !== operationFilter) {
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

export default function Approvals() {
  const { t } = useTranslation(['approvals', 'common']);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [operationFilter, setOperationFilter] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [actingId, setActingId] = useState(null);
  const navigate = useNavigate();

  const statusOptions = useMemo(
    () => [
      { value: '', label: t('filters.statusAll') },
      { value: 'PENDING', label: t('filters.pending') },
      { value: 'APPROVED', label: t('filters.approved') },
      { value: 'REJECTED', label: t('filters.rejected') },
    ],
    [t]
  );
  const operationOptions = useMemo(
    () => [
      { value: '', label: t('filters.operationAll') },
      { value: 'TRANSFER', label: t('operations.transfer') },
      { value: 'WITHDRAW', label: t('operations.withdraw') },
      { value: 'REVERSE', label: t('operations.reverse') },
    ],
    [t]
  );

  const filtersActive = Boolean(statusFilter || operationFilter || dateFrom || dateTo);

  const scopeRows = useMemo(
    () =>
      rows.filter((row) =>
        matchesFilters(row, {
          statusFilter: '',
          operationFilter,
          dateFrom,
          dateTo,
        })
      ),
    [rows, operationFilter, dateFrom, dateTo]
  );

  const filteredRows = useMemo(
    () =>
      rows.filter((row) =>
        matchesFilters(row, { statusFilter, operationFilter, dateFrom, dateTo })
      ),
    [rows, statusFilter, operationFilter, dateFrom, dateTo]
  );

  const queueStats = useMemo(() => {
    const pending = scopeRows.filter((row) => (row.status || '').toUpperCase() === 'PENDING');
    const approved = scopeRows.filter((row) => (row.status || '').toUpperCase() === 'APPROVED');
    const rejected = scopeRows.filter((row) => (row.status || '').toUpperCase() === 'REJECTED');
    return {
      pendingCount: pending.length,
      approvedCount: approved.length,
      rejectedCount: rejected.length,
      awaitingAmount: sumAmounts(pending),
    };
  }, [scopeRows]);

  const load = () => {
    setLoading(true);
    HttpService.getListWithAuth('/spend-requests')
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
      setStatusFilter('');
      setOperationFilter('');
      setDateFrom('');
      setDateTo('');
      load();
    };
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
  }, []);

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(filteredRows.length / rowsPerPage) - 1);
    if (page > maxPage) {
      setPage(maxPage);
    }
  }, [filteredRows.length, rowsPerPage, page]);

  const pagedRows = filteredRows.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);

  const clearFilters = () => {
    setStatusFilter('');
    setOperationFilter('');
    setDateFrom('');
    setDateTo('');
    setPage(0);
  };

  const toggleStatusFilter = (status) => {
    setStatusFilter((current) => (current === status ? '' : status));
    setPage(0);
  };

  const approve = (id) => {
    setActingId(id);
    HttpService.postWithAuth(`/spend-requests/${id}/approve`, null, {
      'Idempotency-Key': createIdempotencyKey(),
    })
      .then(() => {
        enqueueSnackbar(t('snackbar.approved'), { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.approveFailed'), { variant: 'error' });
      })
      .finally(() => setActingId(null));
  };

  const reject = (id) => {
    setActingId(id);
    HttpService.postWithAuth(`/spend-requests/${id}/reject`, null)
      .then(() => {
        enqueueSnackbar(t('snackbar.rejected'), { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || t('snackbar.rejectFailed'), { variant: 'error' });
      })
      .finally(() => setActingId(null));
  };

  return (
    <>
      <Helmet>
        <title>{t('helmet')}</title>
      </Helmet>
      <Container>
        <Stack spacing={0.5} sx={{ mb: 3 }}>
          <Typography variant="h4">{t('title')}</Typography>
          <Typography variant="body2" color="text.secondary">
            {t('subtitle')}
          </Typography>
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
          {queueStats.pendingCount === 1
            ? t('stats.pendingCountOne')
            : t('stats.pendingCountMany', { count: queueStats.pendingCount })}
        </Typography>

        <Box sx={{ ...FOUR_COL_GRID_SX, mb: 3 }}>
          <OpsStatCard
            title={t('stats.pending')}
            value={queueStats.pendingCount}
            format="count"
            icon="eva:clock-outline"
            color="warning"
            subtitle={queueStats.pendingCount > 0 ? t('stats.needsDualControl') : undefined}
            onClick={() => toggleStatusFilter('PENDING')}
            sx={statusFilter === 'PENDING' ? ACTIVE_STAT_SX : undefined}
          />
          <OpsStatCard
            title={t('stats.approved')}
            value={queueStats.approvedCount}
            format="count"
            icon="eva:checkmark-circle-2-outline"
            color="success"
            onClick={() => toggleStatusFilter('APPROVED')}
            sx={statusFilter === 'APPROVED' ? ACTIVE_STAT_SX : undefined}
          />
          <OpsStatCard
            title={t('stats.rejected')}
            value={queueStats.rejectedCount}
            format="count"
            icon="eva:close-circle-outline"
            color="error"
            onClick={() => toggleStatusFilter('REJECTED')}
            sx={statusFilter === 'REJECTED' ? ACTIVE_STAT_SX : undefined}
          />
          <OpsStatCard
            title={t('stats.awaiting')}
            value={queueStats.awaitingAmount}
            format="money"
            icon="eva:diagonal-arrow-right-up-fill"
            color="info"
            onClick={() => toggleStatusFilter('PENDING')}
            sx={statusFilter === 'PENDING' ? ACTIVE_STAT_SX : undefined}
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
                <MenuItem key={opt.value || 'all-statuses'} value={opt.value}>
                  {opt.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              size="small"
              label={t('common:filters.operation')}
              value={operationFilter}
              onChange={(event) => {
                setOperationFilter(event.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 160 }}
            >
              {operationOptions.map((opt) => (
                <MenuItem key={opt.value || 'all-operations'} value={opt.value}>
                  {opt.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              size="small"
              type="date"
              label={t('common:filters.from')}
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
              label={t('common:filters.to')}
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
              icon="eva:checkmark-circle-2-outline"
              title={t('empty.none')}
              description={t('empty.noneDescription')}
            />
          ) : filteredRows.length === 0 ? (
            <EmptyState
              icon="eva:search-outline"
              title={t('empty.noMatch')}
              description={t('empty.noMatchDescription')}
            />
          ) : (
            <>
              <Scrollbar>
                <TableContainer sx={{ overflowX: 'auto' }}>
                  <Table sx={{ minWidth: 800 }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>{t('common:fields.operation')}</TableCell>
                        <TableCell align="right">{t('common:fields.amount')}</TableCell>
                        <TableCell>{t('table.fromTo')}</TableCell>
                        <TableCell>{t('table.requester')}</TableCell>
                        <TableCell>{t('table.when')}</TableCell>
                        <TableCell>{t('common:filters.status')}</TableCell>
                        <TableCell align="right">{t('table.actions')}</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {pagedRows.map((row) => (
                        <TableRow key={row.id} hover>
                          <TableCell>
                            <Typography variant="subtitle2">{operationLabel(row.operation, t)}</Typography>
                            {row.sourceTransactionId && (
                              <Typography variant="caption" color="text.secondary" display="block">
                                {t('table.sourceTx', { id: row.sourceTransactionId })}
                              </Typography>
                            )}
                            {row.description && (
                              <Typography variant="caption" color="text.secondary" display="block">
                                {row.description}
                              </Typography>
                            )}
                          </TableCell>
                          <TableCell align="right">
                            <MoneyText amount={row.amount} tone="debit" />
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: 12 }}>
                              {maskAccount(row.fromWalletIban)}
                            </Typography>
                            <Typography variant="caption" color="text.secondary" display="block">
                              → {maskAccount(row.toWalletIban)}
                            </Typography>
                          </TableCell>
                          <TableCell>{row.requesterUsername || '—'}</TableCell>
                          <TableCell>
                            <Typography variant="body2" noWrap>
                              {fDateTime(row.createdAt)}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Label color={statusColor(row.status)} variant="soft">
                              {row.status}
                            </Label>
                          </TableCell>
                          <TableCell align="right">
                            {row.status === 'PENDING' && (
                              <Stack direction="row" spacing={1} justifyContent="flex-end">
                                <Button
                                  size="small"
                                  variant="contained"
                                  disabled={actingId === row.id}
                                  onClick={() => approve(row.id)}
                                  sx={{ cursor: 'pointer' }}
                                >
                                  {actingId === row.id ? t('actions.working') : t('common:actions.approve')}
                                </Button>
                                <Button
                                  size="small"
                                  color="inherit"
                                  variant="outlined"
                                  disabled={actingId === row.id}
                                  onClick={() => reject(row.id)}
                                  sx={{ cursor: 'pointer' }}
                                >
                                  {t('common:actions.reject')}
                                </Button>
                              </Stack>
                            )}
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
    </>
  );
}
