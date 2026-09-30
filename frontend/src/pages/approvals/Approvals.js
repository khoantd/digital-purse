import { useEffect, useMemo, useState } from 'react';
import {
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
import { EmptyState, MoneyText } from '../../components/wallet-ui';
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

function operationLabel(operation) {
  const op = (operation || '').toUpperCase();
  if (op === 'TRANSFER') return 'Transfer';
  if (op === 'WITHDRAW') return 'Withdraw';
  if (op === 'REVERSE') return 'Reverse';
  return operation || '—';
}

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
];

const OPERATION_OPTIONS = [
  { value: '', label: 'All operations' },
  { value: 'TRANSFER', label: 'Transfer' },
  { value: 'WITHDRAW', label: 'Withdraw' },
  { value: 'REVERSE', label: 'Reverse' },
];

const ROWS_PER_PAGE_OPTIONS = [5, 10, 25];
const PAGINATION_SX = {
  borderTop: (theme) => `1px solid ${theme.palette.divider}`,
  cursor: 'pointer',
  '& .MuiTablePagination-actions button': { cursor: 'pointer' },
  '& .MuiInputBase-root': { cursor: 'pointer' },
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
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [operationFilter, setOperationFilter] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const navigate = useNavigate();

  const filtersActive = Boolean(statusFilter || operationFilter || dateFrom || dateTo);

  const filteredRows = useMemo(
    () =>
      rows.filter((row) =>
        matchesFilters(row, { statusFilter, operationFilter, dateFrom, dateTo })
      ),
    [rows, statusFilter, operationFilter, dateFrom, dateTo]
  );

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

  const approve = (id) => {
    HttpService.postWithAuth(`/spend-requests/${id}/approve`, null, {
      'Idempotency-Key': createIdempotencyKey(),
    })
      .then(() => {
        enqueueSnackbar('Spend request approved', { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Approve failed', { variant: 'error' });
      });
  };

  const reject = (id) => {
    HttpService.postWithAuth(`/spend-requests/${id}/reject`, null)
      .then(() => {
        enqueueSnackbar('Spend request rejected', { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Reject failed', { variant: 'error' });
      });
  };

  return (
    <>
      <Helmet>
        <title> Approvals | Digital Purse </title>
      </Helmet>
      <Container>
        <Stack spacing={0.5} sx={{ mb: 3 }}>
          <Typography variant="h4">Approvals</Typography>
          <Typography variant="body2" color="text.secondary">
            Dual-control spend for the active organization. Large transfers, withdrawals, and reverses wait
            here until another OWNER, ADMIN, or APPROVER acts.
          </Typography>
        </Stack>
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
              select
              size="small"
              label="Operation"
              value={operationFilter}
              onChange={(event) => {
                setOperationFilter(event.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 160 }}
            >
              {OPERATION_OPTIONS.map((opt) => (
                <MenuItem key={opt.value || 'all-operations'} value={opt.value}>
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
            <Typography variant="body2" color="text.secondary" sx={{ py: 6, textAlign: 'center' }}>
              Loading…
            </Typography>
          ) : rows.length === 0 ? (
            <EmptyState
              icon="eva:checkmark-circle-2-outline"
              title="No spend requests"
              description="When a transfer, withdrawal, or reverse needs dual-control approval, it will appear here for this organization."
            />
          ) : filteredRows.length === 0 ? (
            <EmptyState
              icon="eva:search-outline"
              title="No approvals match your filters"
              description="Try a different status, operation, or date range, or clear the filters above."
            />
          ) : (
            <>
              <Scrollbar>
                <TableContainer sx={{ minWidth: 800 }}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Operation</TableCell>
                        <TableCell align="right">Amount</TableCell>
                        <TableCell>From → To</TableCell>
                        <TableCell>Requester</TableCell>
                        <TableCell>When</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {pagedRows.map((row) => (
                        <TableRow key={row.id} hover>
                          <TableCell>
                            <Typography variant="subtitle2">{operationLabel(row.operation)}</Typography>
                            {row.sourceTransactionId && (
                              <Typography variant="caption" color="text.secondary" display="block">
                                Source tx #{row.sourceTransactionId}
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
                                  onClick={() => approve(row.id)}
                                  sx={{ cursor: 'pointer' }}
                                >
                                  Approve
                                </Button>
                                <Button
                                  size="small"
                                  color="inherit"
                                  variant="outlined"
                                  onClick={() => reject(row.id)}
                                  sx={{ cursor: 'pointer' }}
                                >
                                  Reject
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
                labelRowsPerPage="Approvals per page"
                sx={PAGINATION_SX}
              />
            </>
          )}
        </Card>
      </Container>
    </>
  );
}
