import { LoadingButton } from '@mui/lab';
import {
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
import { useNavigate } from 'react-router-dom';
import Iconify from '../../components/iconify';
import Scrollbar from '../../components/scrollbar';
import { EmptyState, WalletCard } from '../../components/wallet-ui';
import AuthService from '../../services/AuthService';
import HttpService from '../../services/HttpService';
import { fCurrency } from '../../utils/formatNumber';
import WalletListHead from './WalletListHead';

const TABLE_HEAD = [
  { id: 'id', label: 'Id', alignRight: false, firstColumn: true },
  { id: 'name', label: 'Name', alignRight: false },
  { id: 'ownerType', label: 'Type', alignRight: false },
  { id: 'balance', label: 'Balance', alignRight: true },
  { id: 'currency', label: 'Currency', alignRight: false },
  { id: 'userId', label: 'User', alignRight: false },
  { id: 'iban', label: 'Account', alignRight: false },
  { id: '' },
];

const ROWS_PER_PAGE_OPTIONS = [5, 10, 25];
const PAGINATION_SX = {
  borderTop: (theme) => `1px solid ${theme.palette.divider}`,
  mt: 2,
  cursor: 'pointer',
  '& .MuiTablePagination-actions button': { cursor: 'pointer' },
  '& .MuiInputBase-root': { cursor: 'pointer' },
};

function slicePage(items, page, rowsPerPage) {
  return items.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);
}

function typeLabel(ownerType, customerName) {
  if (ownerType === 'CUSTOMER') {
    return customerName ? `Customer · ${customerName}` : 'Customer';
  }
  return 'Organization';
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

function SectionHeading({ title, count }) {
  return (
    <Stack direction="row" alignItems="baseline" spacing={1} sx={{ mb: 1.5 }}>
      <Typography variant="h6">{title}</Typography>
      <Typography variant="caption" color="text.secondary">
        {count} {count === 1 ? 'wallet' : 'wallets'}
      </Typography>
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

function WalletTable({ wallets, selected, onOpenMenu }) {
  return (
    <Scrollbar>
      <TableContainer sx={{ minWidth: 800 }}>
        <Table>
          <WalletListHead headLabel={TABLE_HEAD} />
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
                      aria-label={`Actions for ${name}`}
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
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuWallet, setMenuWallet] = useState(null);
  const [selected] = useState([]);
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [detailWallet, setDetailWallet] = useState(null);
  const [editWallet, setEditWallet] = useState(null);
  const [editName, setEditName] = useState('');
  const [saving, setSaving] = useState(false);
  const [orgPage, setOrgPage] = useState(0);
  const [orgRowsPerPage, setOrgRowsPerPage] = useState(10);
  const [customerPage, setCustomerPage] = useState(0);
  const [customerRowsPerPage, setCustomerRowsPerPage] = useState(10);
  const navigate = useNavigate();
  const isAdmin = AuthService.isAdmin();

  const organizationWallets = useMemo(
    () => data.filter((w) => w.ownerType !== 'CUSTOMER'),
    [data]
  );
  const customerWallets = useMemo(
    () => data.filter((w) => w.ownerType === 'CUSTOMER'),
    [data]
  );
  const pagedOrganizationWallets = slicePage(organizationWallets, orgPage, orgRowsPerPage);
  const pagedCustomerWallets = slicePage(customerWallets, customerPage, customerRowsPerPage);

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(organizationWallets.length / orgRowsPerPage) - 1);
    if (orgPage > maxPage) {
      setOrgPage(maxPage);
    }
  }, [organizationWallets.length, orgRowsPerPage, orgPage]);

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(customerWallets.length / customerRowsPerPage) - 1);
    if (customerPage > maxPage) {
      setCustomerPage(maxPage);
    }
  }, [customerWallets.length, customerRowsPerPage, customerPage]);

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
      enqueueSnackbar('Account number copied', { variant: 'success' });
    } catch {
      enqueueSnackbar('Could not copy account number', { variant: 'error' });
    }
  };

  const saveRename = (event) => {
    event.preventDefault();
    if (!editWallet) return;

    const name = editName.trim();
    if (name.length < 3 || name.length > 50) {
      enqueueSnackbar('Wallet name must be 3–50 characters', { variant: 'warning' });
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
        enqueueSnackbar('Wallet updated', { variant: 'success' });
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
    const onOrg = () => fetchData();
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
        setOrgPage(0);
        setCustomerPage(0);
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

  const orgPagination = organizationWallets.length > 0 && (
    <TablePagination
      rowsPerPageOptions={ROWS_PER_PAGE_OPTIONS}
      component="div"
      count={organizationWallets.length}
      rowsPerPage={orgRowsPerPage}
      page={orgPage}
      onPageChange={(_, newPage) => setOrgPage(newPage)}
      onRowsPerPageChange={(event) => {
        setOrgRowsPerPage(parseInt(event.target.value, 10));
        setOrgPage(0);
      }}
      labelRowsPerPage="Wallets per page"
      sx={PAGINATION_SX}
    />
  );

  const customerPagination = customerWallets.length > 0 && (
    <TablePagination
      rowsPerPageOptions={ROWS_PER_PAGE_OPTIONS}
      component="div"
      count={customerWallets.length}
      rowsPerPage={customerRowsPerPage}
      page={customerPage}
      onPageChange={(_, newPage) => setCustomerPage(newPage)}
      onRowsPerPageChange={(event) => {
        setCustomerRowsPerPage(parseInt(event.target.value, 10));
        setCustomerPage(0);
      }}
      labelRowsPerPage="Wallets per page"
      sx={PAGINATION_SX}
    />
  );

  const listTitle = isAdmin ? 'All Wallets' : 'Wallets';
  const listSubtitle = isAdmin
    ? 'All wallets by owner type.'
    : 'Organization and customer wallets for the active organization (VND).';

  const renderSmeSections = () => {
    if (loading) {
      return (
        <Card sx={{ p: 2.5, borderRadius: 2 }} aria-busy="true">
          <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
            Loading…
          </Typography>
        </Card>
      );
    }

    if (data.length === 0) {
      return (
        <Card sx={{ p: 2.5, borderRadius: 2 }}>
          <EmptyState
            icon="ant-design:wallet-outlined"
            title="No wallets yet"
            description="Create the first wallet for this organization. A Vietnam account number is assigned automatically."
            actionLabel="New wallet"
            onAction={() => navigate('/wallets/new')}
          />
        </Card>
      );
    }

    return (
      <Stack spacing={3}>
        <Card sx={{ p: 2.5, borderRadius: 2 }}>
          <SectionHeading title="Organization Wallets" count={organizationWallets.length} />
          {organizationWallets.length === 0 ? (
            <EmptyState
              icon="ant-design:bank-outlined"
              title="No organization wallets yet"
              description="Create a wallet owned by the organization."
              actionLabel="New wallet"
              onAction={() => navigate('/wallets/new')}
            />
          ) : (
            <>
              <WalletCardGrid
                wallets={pagedOrganizationWallets}
                navigate={navigate}
                onDetails={openDetails}
                onEdit={openEdit}
              />
              {orgPagination}
            </>
          )}
        </Card>

        <Card sx={{ p: 2.5, borderRadius: 2 }}>
          <SectionHeading title="Customers' Wallets" count={customerWallets.length} />
          {customerWallets.length === 0 ? (
            <EmptyState
              icon="ant-design:user-outlined"
              title="No customer wallets yet"
              description="Create a wallet labeled for a customer contact."
              actionLabel="New wallet"
              onAction={() => navigate('/wallets/new')}
            />
          ) : (
            <>
              <WalletCardGrid
                wallets={pagedCustomerWallets}
                navigate={navigate}
                onDetails={openDetails}
                onEdit={openEdit}
              />
              {customerPagination}
            </>
          )}
        </Card>
      </Stack>
    );
  };

  const renderAdminSections = () => {
    if (loading) {
      return (
        <Card sx={{ borderRadius: 2 }} aria-busy="true">
          <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
            Loading…
          </Typography>
        </Card>
      );
    }

    if (data.length === 0) {
      return (
        <Card sx={{ borderRadius: 2 }}>
          <EmptyState
            icon="ant-design:wallet-outlined"
            title="No wallets"
            description="No wallets have been created yet."
          />
        </Card>
      );
    }

    return (
      <Stack spacing={3}>
        <Card sx={{ borderRadius: 2, p: 2.5 }}>
          <SectionHeading title="Organization Wallets" count={organizationWallets.length} />
          {organizationWallets.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
              No organization wallets.
            </Typography>
          ) : (
            <>
              <WalletTable wallets={pagedOrganizationWallets} selected={selected} onOpenMenu={handleOpenMenu} />
              {orgPagination}
            </>
          )}
        </Card>

        <Card sx={{ borderRadius: 2, p: 2.5 }}>
          <SectionHeading title="Customers' Wallets" count={customerWallets.length} />
          {customerWallets.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
              No customer wallets.
            </Typography>
          ) : (
            <>
              <WalletTable wallets={pagedCustomerWallets} selected={selected} onOpenMenu={handleOpenMenu} />
              {customerPagination}
            </>
          )}
        </Card>
      </Stack>
    );
  };

  return (
    <>
      <Helmet>
        <title> Wallets | Digital Purse </title>
      </Helmet>
      <Container sx={{ minWidth: '100%' }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between" mb={3}>
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
            sx={{ cursor: 'pointer' }}
          >
            New Wallet
          </Button>
        </Stack>

        {isAdmin ? renderAdminSections() : renderSmeSections()}
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
          Details
        </MenuItem>
        <MenuItem
          onClick={() => menuWallet && openEdit(menuWallet)}
          sx={{ cursor: 'pointer' }}
          disabled={!menuWallet}
        >
          <Iconify icon="eva:edit-fill" sx={{ mr: 2 }} />
          Edit
        </MenuItem>
        <MenuItem disabled sx={{ color: 'error.main' }}>
          <Iconify icon="eva:trash-2-outline" sx={{ mr: 2 }} />
          Delete
        </MenuItem>
      </Popover>

      <Dialog open={Boolean(detailWallet)} onClose={closeDetails} fullWidth maxWidth="sm">
        <DialogTitle>Wallet details</DialogTitle>
        <DialogContent>
          {detailWallet && (
            <Stack spacing={2.5} sx={{ pt: 1 }}>
              <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
                <Typography variant="h6">{detailWallet.name}</Typography>
                <Chip size="small" label={typeLabel(detailWallet.ownerType, detailWallet.customerName)} />
              </Stack>

              <DetailRow label="Balance">
                <Typography variant="h5" sx={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
                  {fCurrency(detailWallet.balance)}
                  <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                    {detailWallet.currency || 'VND'}
                  </Typography>
                </Typography>
              </DetailRow>

              <Divider />

              <DetailRow label="Account number">
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
                      aria-label="Copy account number"
                      onClick={() => copyIban(detailWallet.iban)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <Iconify icon="eva:copy-outline" width={18} height={18} />
                    </IconButton>
                  )}
                </Stack>
              </DetailRow>

              {detailWallet.user?.fullName && (
                <DetailRow label="Owner user">
                  <Typography variant="body2">{detailWallet.user.fullName}</Typography>
                </DetailRow>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={closeDetails} sx={{ cursor: 'pointer' }}>
            Close
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
              View transactions
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(editWallet)} onClose={closeEdit} fullWidth maxWidth="xs">
        <DialogTitle>Edit wallet</DialogTitle>
        <DialogContent>
          <Stack component="form" id="wallet-edit-form" onSubmit={saveRename} spacing={2} sx={{ pt: 1 }} noValidate>
            <TextField
              label="Wallet name"
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              required
              autoFocus
              inputProps={{ minLength: 3, maxLength: 50 }}
              helperText="3–50 characters"
            />
            <TextField
              label="Account number"
              value={editWallet?.iban || ''}
              disabled
              helperText="Assigned by the system and cannot be changed"
            />
            <TextField
              label="Balance"
              value={editWallet ? `${fCurrency(editWallet.balance)} ${editWallet.currency || 'VND'}` : ''}
              disabled
              helperText="Change balance via Add funds, Transfer, or Withdraw"
            />
            <TextField
              label="Owner type"
              value={editWallet ? typeLabel(editWallet.ownerType, editWallet.customerName) : ''}
              disabled
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={closeEdit} disabled={saving} sx={{ cursor: 'pointer' }}>
            Cancel
          </Button>
          <LoadingButton
            type="submit"
            form="wallet-edit-form"
            variant="contained"
            loading={saving}
            sx={{ cursor: 'pointer' }}
          >
            Save
          </LoadingButton>
        </DialogActions>
      </Dialog>
    </>
  );
}
