import { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Button,
  Card,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
import { EmptyState } from '../../components/wallet-ui';
import AuthService from '../../services/AuthService';
import HttpService from '../../services/HttpService';
import OrganizationContext from '../../services/OrganizationContext';
import { ensureActiveOrganization } from '../../services/ensureOrganization';
import { fDateTime, parseDateInputEnd, parseDateInputStart, toDate } from '../../utils/formatTime';

const EVENT_OPTIONS = [
  { value: '', label: 'All events' },
  { value: 'AUTH_LOGIN', label: 'Login' },
  { value: 'AUTH_LOGOUT', label: 'Logout' },
  { value: 'AUTH_SIGNUP', label: 'Signup' },
  { value: 'ORG_CREATE', label: 'Org created' },
  { value: 'ORG_UPDATE', label: 'Org updated' },
  { value: 'ORG_MEMBER_ADD', label: 'Member added' },
  { value: 'ORG_MEMBER_ROLE_UPDATE', label: 'Member role' },
  { value: 'ORG_MEMBER_REMOVE', label: 'Member removed' },
  { value: 'ORG_LIMITS_UPDATE', label: 'Limits updated' },
  { value: 'CUSTOMER_CREATE', label: 'Customer created' },
  { value: 'CUSTOMER_UPDATE', label: 'Customer updated' },
  { value: 'CUSTOMER_ARCHIVE', label: 'Customer archived' },
  { value: 'CUSTOMER_LINK_WALLET', label: 'Customer linked' },
  { value: 'CUSTOMER_UNLINK_WALLET', label: 'Customer unlinked' },
  { value: 'WALLET_CREATE', label: 'Wallet created' },
  { value: 'WALLET_UPDATE', label: 'Wallet updated' },
  { value: 'WALLET_DELETE', label: 'Wallet deleted' },
  { value: 'SPEND_REQUEST_CREATE', label: 'Spend requested' },
  { value: 'SPEND_APPROVE', label: 'Spend approved' },
  { value: 'SPEND_REJECT', label: 'Spend rejected' },
  { value: 'TX_REVERSE', label: 'Transaction reverse' },
];

const EVENT_COLORS = {
  AUTH_LOGIN: 'info',
  AUTH_LOGOUT: 'default',
  AUTH_SIGNUP: 'info',
  ORG_CREATE: 'primary',
  ORG_UPDATE: 'primary',
  ORG_MEMBER_ADD: 'success',
  ORG_MEMBER_ROLE_UPDATE: 'warning',
  ORG_MEMBER_REMOVE: 'error',
  ORG_LIMITS_UPDATE: 'warning',
  CUSTOMER_CREATE: 'success',
  CUSTOMER_UPDATE: 'info',
  CUSTOMER_ARCHIVE: 'default',
  CUSTOMER_LINK_WALLET: 'info',
  CUSTOMER_UNLINK_WALLET: 'default',
  WALLET_CREATE: 'success',
  WALLET_UPDATE: 'info',
  WALLET_DELETE: 'error',
  SPEND_REQUEST_CREATE: 'warning',
  SPEND_APPROVE: 'success',
  SPEND_REJECT: 'error',
  TX_REVERSE: 'warning',
};

const ROWS_PER_PAGE_OPTIONS = [5, 10, 25];
const PAGINATION_SX = {
  borderTop: (theme) => `1px solid ${theme.palette.divider}`,
  cursor: 'pointer',
  '& .MuiTablePagination-actions button': { cursor: 'pointer' },
  '& .MuiInputBase-root': { cursor: 'pointer' },
};

function eventLabel(eventType) {
  return EVENT_OPTIONS.find((opt) => opt.value === eventType)?.label || eventType || '—';
}

function actorDisplay(row) {
  const name = [row.actorFirstName, row.actorLastName].filter(Boolean).join(' ').trim();
  if (name && row.actorUsername) return { primary: name, secondary: row.actorUsername };
  if (row.actorUsername) return { primary: row.actorUsername, secondary: null };
  if (name) return { primary: name, secondary: null };
  return { primary: '—', secondary: null };
}

function matchesFilters(row, { eventFilter, dateFrom, dateTo }) {
  if (eventFilter && (row.eventType || '') !== eventFilter) {
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

function canViewActivity(role) {
  if (AuthService.isAdmin()) return true;
  const r = (role || '').toUpperCase();
  return r === 'OWNER' || r === 'ADMIN';
}

export default function Activity() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [allowed, setAllowed] = useState(null);
  const [eventFilter, setEventFilter] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [detailRow, setDetailRow] = useState(null);
  const [loadError, setLoadError] = useState('');
  const navigate = useNavigate();

  const filtersActive = Boolean(eventFilter || dateFrom || dateTo);

  const filteredRows = useMemo(
    () => rows.filter((row) => matchesFilters(row, { eventFilter, dateFrom, dateTo })),
    [rows, eventFilter, dateFrom, dateTo]
  );

  const checkAccess = () =>
    ensureActiveOrganization().then((list) => {
      const activeId = OrganizationContext.getActiveOrganizationId();
      const active = list.find((org) => org.id === activeId);
      const ok = canViewActivity(active?.myRole);
      setAllowed(ok);
      if (!ok) {
        navigate('/', { replace: true });
      }
      return ok;
    });

  const load = () => {
    setLoading(true);
    setLoadError('');
    HttpService.getListWithAuth('/activity-logs')
      .then((list) => {
        setRows(list);
        setPage(0);
      })
      .catch((error) => {
        if (error?.response?.status === 401) {
          navigate('/login');
        } else if (error?.response?.status === 403) {
          setAllowed(false);
          navigate('/', { replace: true });
        } else if (error.response?.data?.message) {
          setLoadError(error.response.data.message);
          enqueueSnackbar(error.response.data.message, { variant: 'error' });
        }
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    checkAccess()
      .then((ok) => {
        if (ok) load();
      })
      .catch(() => undefined);

    const onOrg = () => {
      setEventFilter('');
      setDateFrom('');
      setDateTo('');
      checkAccess()
        .then((ok) => {
          if (ok) load();
        })
        .catch(() => undefined);
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

  if (allowed === false) {
    return null;
  }

  const pagedRows = filteredRows.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);

  const clearFilters = () => {
    setEventFilter('');
    setDateFrom('');
    setDateTo('');
    setPage(0);
  };

  return (
    <>
      <Helmet>
        <title> Activity | Digital Purse </title>
      </Helmet>
      <Container>
        <Stack spacing={0.5} sx={{ mb: 3 }}>
          <Typography variant="h4" component="h1">
            Activity
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Security and admin actions for this organization. Visible to OWNER and ADMIN only.
          </Typography>
        </Stack>

        {loadError ? (
          <Typography role="alert" color="error" sx={{ mb: 2 }}>
            {loadError}
          </Typography>
        ) : null}

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
              label="Event type"
              value={eventFilter}
              onChange={(event) => {
                setEventFilter(event.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 180 }}
            >
              {EVENT_OPTIONS.map((opt) => (
                <MenuItem key={opt.value || 'all-events'} value={opt.value}>
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

          {loading || allowed == null ? (
            <Typography variant="body2" color="text.secondary" sx={{ py: 6, textAlign: 'center' }}>
              Loading…
            </Typography>
          ) : rows.length === 0 ? (
            <EmptyState
              icon="eva:clock-outline"
              title="No activity yet"
              description="Security and admin actions will appear here as your team works."
            />
          ) : filteredRows.length === 0 ? (
            <EmptyState
              icon="eva:search-outline"
              title="No matching activity"
              description="Try a different event type or date range, or clear filters."
              actionLabel="Clear filters"
              onAction={clearFilters}
            />
          ) : (
            <>
              <Scrollbar>
                <TableContainer sx={{ overflowX: 'auto' }}>
                  <Table size="small" sx={{ minWidth: 800 }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>When</TableCell>
                        <TableCell>Actor</TableCell>
                        <TableCell>Event</TableCell>
                        <TableCell>Summary</TableCell>
                        <TableCell>IP</TableCell>
                        <TableCell align="right">Details</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {pagedRows.map((row) => {
                        const actor = actorDisplay(row);
                        return (
                          <TableRow key={row.id} hover>
                            <TableCell sx={{ whiteSpace: 'nowrap' }}>
                              {fDateTime(row.createdAt)}
                            </TableCell>
                            <TableCell>
                              <Typography variant="subtitle2">{actor.primary}</Typography>
                              {actor.secondary ? (
                                <Typography variant="caption" color="text.secondary">
                                  {actor.secondary}
                                </Typography>
                              ) : null}
                            </TableCell>
                            <TableCell>
                              <Label color={EVENT_COLORS[row.eventType] || 'default'}>
                                {eventLabel(row.eventType)}
                              </Label>
                            </TableCell>
                            <TableCell>{row.summary || '—'}</TableCell>
                            <TableCell sx={{ fontFamily: 'monospace', fontSize: 12 }}>
                              {row.ipAddress || '—'}
                            </TableCell>
                            <TableCell align="right">
                              <Button
                                size="small"
                                onClick={() => setDetailRow(row)}
                                sx={{ cursor: 'pointer' }}
                              >
                                View
                              </Button>
                            </TableCell>
                          </TableRow>
                        );
                      })}
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
                onPageChange={(_, next) => setPage(next)}
                onRowsPerPageChange={(event) => {
                  setRowsPerPage(parseInt(event.target.value, 10));
                  setPage(0);
                }}
                labelRowsPerPage="Activity per page"
                sx={PAGINATION_SX}
              />
            </>
          )}
        </Card>

        <Dialog
          open={Boolean(detailRow)}
          onClose={() => setDetailRow(null)}
          fullWidth
          maxWidth="sm"
        >
          <DialogTitle>Activity details</DialogTitle>
          <DialogContent dividers>
            {detailRow ? (
              <Stack spacing={1.5}>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Event
                  </Typography>
                  <Typography variant="body2">{eventLabel(detailRow.eventType)}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Summary
                  </Typography>
                  <Typography variant="body2">{detailRow.summary}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Metadata
                  </Typography>
                  <Box
                    component="pre"
                    sx={{
                      m: 0,
                      mt: 0.5,
                      p: 1.5,
                      borderRadius: 1,
                      bgcolor: 'grey.100',
                      fontFamily: 'monospace',
                      fontSize: 12,
                      overflow: 'auto',
                    }}
                  >
                    {JSON.stringify(detailRow.metadata || {}, null, 2)}
                  </Box>
                </Box>
              </Stack>
            ) : null}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDetailRow(null)} sx={{ cursor: 'pointer' }}>
              Close
            </Button>
          </DialogActions>
        </Dialog>
      </Container>
    </>
  );
}
