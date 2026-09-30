import {
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
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import Iconify from '../../components/iconify';
import Label from '../../components/label';
import Scrollbar from '../../components/scrollbar';
import { EmptyState } from '../../components/wallet-ui';
import HttpService from '../../services/HttpService';

const emptyForm = {
  name: '',
  phone: '',
  email: '',
  taxId: '',
  notes: '',
};

const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ARCHIVED', label: 'Archived' },
  { value: 'ALL', label: 'All statuses' },
];

const LINKED_OPTIONS = [
  { value: '', label: 'All links' },
  { value: 'linked', label: 'Linked' },
  { value: 'unlinked', label: 'Unlinked' },
];

const ROWS_PER_PAGE_OPTIONS = [5, 10, 25];
const PAGINATION_SX = {
  borderTop: (theme) => `1px solid ${theme.palette.divider}`,
  cursor: 'pointer',
  '& .MuiTablePagination-actions button': { cursor: 'pointer' },
  '& .MuiInputBase-root': { cursor: 'pointer' },
};

function matchesLinkedFilter(row, linkedFilter) {
  if (!linkedFilter) return true;
  const hasLink = Boolean(row.linkedWalletIban);
  if (linkedFilter === 'linked') return hasLink;
  if (linkedFilter === 'unlinked') return !hasLink;
  return true;
}

export default function Customers() {
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

  const filtersActive = Boolean(
    appliedQuery.trim() || statusFilter !== 'ACTIVE' || linkedFilter
  );

  const filteredRows = useMemo(
    () => rows.filter((row) => matchesLinkedFilter(row, linkedFilter)),
    [rows, linkedFilter]
  );

  const load = (q = appliedQuery, status = statusFilter) => {
    setLoading(true);
    const params = new URLSearchParams();
    if (q?.trim()) params.set('q', q.trim());
    params.set('status', status || 'ACTIVE');
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
      load('', 'ACTIVE');
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
    load(next, statusFilter);
  };

  const clearFilters = () => {
    setQuery('');
    setAppliedQuery('');
    setStatusFilter('ACTIVE');
    setLinkedFilter('');
    setPage(0);
    load('', 'ACTIVE');
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
      enqueueSnackbar('Name must be at least 2 characters', { variant: 'warning' });
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
        enqueueSnackbar(editingId ? 'Customer updated' : 'Customer created', { variant: 'success' });
        setDialogOpen(false);
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Save failed', { variant: 'error' });
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
      enqueueSnackbar('Enter an account number', { variant: 'warning' });
      return;
    }
    setSubmitting(true);
    HttpService.postWithAuth(`/customers/${linkCustomerId}/link-wallet`, { iban })
      .then(() => {
        enqueueSnackbar('Wallet linked', { variant: 'success' });
        setLinkDialogOpen(false);
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Link failed', { variant: 'error' });
      })
      .finally(() => setSubmitting(false));
  };

  const unlink = (customer) => {
    closeMenu();
    HttpService.postWithAuth(`/customers/${customer.id}/unlink-wallet`, null)
      .then(() => {
        enqueueSnackbar('Wallet unlinked', { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Unlink failed', { variant: 'error' });
      });
  };

  const archive = (customer) => {
    closeMenu();
    HttpService.postWithAuth(`/customers/${customer.id}/archive`, null)
      .then(() => {
        enqueueSnackbar('Customer archived', { variant: 'success' });
        load();
      })
      .catch((error) => {
        enqueueSnackbar(error.response?.data?.message || 'Archive failed', { variant: 'error' });
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
        <title> Customers | Digital Purse </title>
      </Helmet>
      <Container sx={{ minWidth: '100%' }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between" mb={3} spacing={2}>
          <Stack spacing={0.5}>
            <Typography variant="h4">Customers</Typography>
            <Typography variant="body2" color="text.secondary">
              Payee contacts for the active organization. Link a Digital Purse account to speed up transfers.
            </Typography>
          </Stack>
          <Button
            variant="contained"
            startIcon={<Iconify icon="eva:plus-fill" />}
            onClick={openCreate}
            sx={{ cursor: 'pointer', flexShrink: 0 }}
          >
            New customer
          </Button>
        </Stack>

        <Card aria-busy={loading || undefined}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            alignItems={{ xs: 'stretch', md: 'center' }}
            flexWrap="wrap"
            sx={{ px: 2.5, py: 2, borderBottom: (theme) => `1px solid ${theme.palette.divider}` }}
          >
            <TextField
              size="small"
              label="Search"
              placeholder="Name"
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
              Search
            </Button>
            <TextField
              select
              size="small"
              label="Status"
              value={statusFilter}
              onChange={(event) => {
                const next = event.target.value;
                setStatusFilter(next);
                setPage(0);
                load(appliedQuery, next);
              }}
              sx={{ minWidth: 160 }}
            >
              {STATUS_OPTIONS.map((opt) => (
                <MenuItem key={opt.value} value={opt.value}>
                  {opt.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              size="small"
              label="Linked"
              value={linkedFilter}
              onChange={(event) => {
                setLinkedFilter(event.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 160 }}
            >
              {LINKED_OPTIONS.map((opt) => (
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
              title={statusFilter === 'ARCHIVED' ? 'No archived customers' : 'No customers yet'}
              description={
                statusFilter === 'ARCHIVED'
                  ? 'Archived payees will appear here. Switch status to Active or All to see current contacts.'
                  : 'Add a payee contact, then optionally link their Digital Purse account number.'
              }
              actionLabel={statusFilter === 'ACTIVE' ? 'New customer' : undefined}
              onAction={statusFilter === 'ACTIVE' ? openCreate : undefined}
            />
          ) : filteredRows.length === 0 ? (
            <EmptyState
              icon="eva:search-outline"
              title="No customers match your filters"
              description="Try a different status or linked filter, or clear the filters above."
            />
          ) : (
            <>
              <Scrollbar>
                <TableContainer sx={{ minWidth: 720 }}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Name</TableCell>
                        <TableCell>Phone</TableCell>
                        <TableCell>Email</TableCell>
                        <TableCell>Linked account</TableCell>
                        <TableCell>Status</TableCell>
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
                                Tax ID: {row.taxId}
                              </Typography>
                            )}
                          </TableCell>
                          <TableCell>{row.phone || '—'}</TableCell>
                          <TableCell>{row.email || '—'}</TableCell>
                          <TableCell>
                            {row.linkedWalletIban ? (
                              <Stack spacing={0.25}>
                                <Typography variant="body2">{row.linkedWalletName || 'Wallet'}</Typography>
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
                labelRowsPerPage="Customers per page"
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
          Edit
        </MenuItem>
        <MenuItem onClick={() => openLink(menuCustomer)} sx={{ cursor: 'pointer' }}>
          <Iconify icon="eva:link-2-fill" sx={{ mr: 2 }} />
          Link wallet
        </MenuItem>
        {menuCustomer?.linkedWalletIban && (
          <MenuItem onClick={() => unlink(menuCustomer)} sx={{ cursor: 'pointer' }}>
            <Iconify icon="eva:link-break-fill" sx={{ mr: 2 }} />
            Unlink wallet
          </MenuItem>
        )}
        {menuCustomer?.status === 'ACTIVE' && (
          <MenuItem onClick={() => archive(menuCustomer)} sx={{ color: 'error.main', cursor: 'pointer' }}>
            <Iconify icon="eva:trash-2-outline" sx={{ mr: 2 }} />
            Archive
          </MenuItem>
        )}
      </Popover>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editingId ? 'Edit customer' : 'New customer'}</DialogTitle>
        <DialogContent>
          <Stack component="form" id="customer-form" onSubmit={saveCustomer} spacing={2} sx={{ pt: 1 }} noValidate>
            <TextField
              name="name"
              label="Name"
              required
              value={formValues.name}
              onChange={handleFormChange}
              inputProps={{ maxLength: 100 }}
            />
            <TextField
              name="phone"
              label="Phone"
              value={formValues.phone}
              onChange={handleFormChange}
              inputProps={{ maxLength: 20 }}
            />
            <TextField
              name="email"
              label="Email"
              type="email"
              value={formValues.email}
              onChange={handleFormChange}
              inputProps={{ maxLength: 100 }}
            />
            <TextField
              name="taxId"
              label="Tax ID"
              value={formValues.taxId}
              onChange={handleFormChange}
              inputProps={{ maxLength: 20 }}
            />
            <TextField
              name="notes"
              label="Notes"
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
            Cancel
          </Button>
          <LoadingButton type="submit" form="customer-form" variant="contained" loading={submitting} sx={{ cursor: 'pointer' }}>
            Save
          </LoadingButton>
        </DialogActions>
      </Dialog>

      <Dialog open={linkDialogOpen} onClose={() => setLinkDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Link Digital Purse</DialogTitle>
        <DialogContent>
          <Stack component="form" id="link-form" onSubmit={submitLink} spacing={2} sx={{ pt: 1 }} noValidate>
            <TextField
              label="Account number"
              value={linkIban}
              onChange={(e) => setLinkIban(e.target.value)}
              placeholder="VN…"
              required
              helperText="Existing Digital Purse account number in the system"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setLinkDialogOpen(false)} sx={{ cursor: 'pointer' }}>
            Cancel
          </Button>
          <LoadingButton type="submit" form="link-form" variant="contained" loading={submitting} sx={{ cursor: 'pointer' }}>
            Link
          </LoadingButton>
        </DialogActions>
      </Dialog>
    </>
  );
}
