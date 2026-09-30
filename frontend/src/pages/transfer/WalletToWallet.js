import { LoadingButton } from '@mui/lab';
import { Autocomplete, Button, InputAdornment, Stack, TextField, Typography } from '@mui/material';
import { useSnackbar } from 'notistack';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from '../../services/AuthService';
import HttpService from '../../services/HttpService';
import { idempotencyHeaders } from '../../utils/idempotency';

const emptyForm = {
  amount: '',
  fromWalletIban: '',
  toWalletIban: '',
  description: '',
};

export default function WalletToWallet() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [formValues, setFormValues] = useState(emptyForm);
  const [wallets, setWallets] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [selectedFromWallet, setSelectedFromWallet] = useState(null);
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [loadingWallets, setLoadingWallets] = useState(true);

  const isAdmin = AuthService.isAdmin();
  const linkedCustomers = customers.filter((c) => c.linkedWalletIban);

  useEffect(() => {
    const load = () => {
      HttpService.getListWithAuth('/wallets?page=0&size=1000')
        .then((list) => setWallets(list))
        .catch((error) => {
          enqueueSnackbar(error.response?.data?.message || 'Could not load wallets', { variant: 'error' });
        })
        .finally(() => setLoadingWallets(false));
      HttpService.getListWithAuth('/customers')
        .then(setCustomers)
        .catch(() => setCustomers([]));
    };
    load();
    const onOrg = () => {
      setLoadingWallets(true);
      setSelectedCustomer(null);
      load();
    };
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
  }, [enqueueSnackbar]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormValues((prev) => ({ ...prev, [name]: value }));
  };

  const handleFromWalletChange = (_event, wallet) => {
    setSelectedFromWallet(wallet);
    setFormValues((prev) => ({
      ...prev,
      fromWalletIban: wallet?.iban || '',
    }));
  };

  const handleCustomerChange = (_event, customer) => {
    setSelectedCustomer(customer);
    setFormValues((prev) => ({
      ...prev,
      toWalletIban: customer?.linkedWalletIban || prev.toWalletIban,
    }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!formValues.fromWalletIban) {
      enqueueSnackbar('Select a sender wallet', { variant: 'warning' });
      return;
    }
    const toWalletIban = formValues.toWalletIban.trim();
    if (!toWalletIban) {
      enqueueSnackbar('Enter the recipient account number', { variant: 'warning' });
      return;
    }
    const amount = Number(formValues.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      enqueueSnackbar('Enter a valid amount', { variant: 'warning' });
      return;
    }
    const description = formValues.description.trim();
    if (!description || description.length > 50) {
      enqueueSnackbar('Note is required (max 50 characters)', { variant: 'warning' });
      return;
    }

    setSubmitting(true);
    HttpService.postWithAuth(
      '/wallets/transfer',
      {
        amount,
        description,
        fromWalletIban: formValues.fromWalletIban,
        toWalletIban,
        typeId: 1,
      },
      idempotencyHeaders()
    )
      .then((response) => {
        const pending = response?.status === 'PENDING_APPROVAL';
        enqueueSnackbar(
          pending ? 'Transfer submitted for approval' : 'Transfer completed successfully',
          { variant: 'success' }
        );
        navigate(pending ? '/approvals' : '/transactions');
      })
      .catch((error) => {
        if (error.response?.data?.errors) {
          error.response.data.errors.forEach((e) => enqueueSnackbar(e.message, { variant: 'error' }));
        } else if (error.response?.data?.message) {
          enqueueSnackbar(error.response.data.message, { variant: 'error' });
        } else {
          enqueueSnackbar(error.message, { variant: 'error' });
        }
      })
      .finally(() => setSubmitting(false));
  };

  return (
    <Stack component="form" onSubmit={handleSubmit} spacing={3} sx={{ maxWidth: 440 }} noValidate>
      <Stack spacing={0.5}>
        <Typography variant="h6">Send money</Typography>
        <Typography variant="body2" color="text.secondary">
          {isAdmin
            ? 'Transfer from any wallet to another account number. Large spends may need dual-control approval.'
            : 'Transfer from an organization wallet. Amounts at or above the dual-control threshold need another member to approve.'}
        </Typography>
      </Stack>
      <TextField
        id="amount"
        name="amount"
        label="Amount"
        autoFocus
        required
        value={formValues.amount}
        onChange={handleInputChange}
        inputProps={{ inputMode: 'numeric', min: 1, step: 1 }}
        InputProps={{
          endAdornment: <InputAdornment position="end">₫</InputAdornment>,
        }}
        helperText="Vietnamese đồng (VND)"
      />
      <Autocomplete
        ListboxProps={{ style: { maxHeight: 200, overflow: 'auto' } }}
        disablePortal
        id="fromWalletIban"
        loading={loadingWallets}
        noOptionsText={loadingWallets ? 'Loading…' : 'No wallets'}
        options={wallets}
        value={selectedFromWallet}
        getOptionLabel={(wallet) => {
          if (!wallet?.name) return '';
          const owner = isAdmin && wallet.user?.fullName ? ` · ${wallet.user.fullName}` : '';
          return `${wallet.name}${owner} · ${wallet.iban}`;
        }}
        isOptionEqualToValue={(option, value) => option?.iban === value?.iban}
        onChange={handleFromWalletChange}
        renderInput={(params) => <TextField {...params} label="From wallet" required />}
      />
      {linkedCustomers.length > 0 && (
        <Autocomplete
          disablePortal
          id="customerPayee"
          options={linkedCustomers}
          value={selectedCustomer}
          getOptionLabel={(c) => {
            if (!c?.name) return '';
            return `${c.name} · ${c.linkedWalletIban}`;
          }}
          isOptionEqualToValue={(option, value) => option?.id === value?.id}
          onChange={handleCustomerChange}
          renderInput={(params) => (
            <TextField {...params} label="Customer (optional)" helperText="Prefills recipient from linked payee" />
          )}
        />
      )}
      <TextField
        id="toWalletIban"
        name="toWalletIban"
        label="Recipient account number"
        autoComplete="off"
        required
        value={formValues.toWalletIban}
        onChange={(e) => {
          setSelectedCustomer(null);
          handleInputChange(e);
        }}
        placeholder="VN…"
        helperText="Vietnam account number (starts with VN)"
      />
      <TextField
        id="description"
        name="description"
        label="Note"
        autoComplete="off"
        required
        value={formValues.description}
        onChange={handleInputChange}
        inputProps={{ maxLength: 50 }}
        helperText="Max 50 characters"
      />
      <Stack spacing={2} direction="row" justifyContent="flex-end" sx={{ pt: 1 }}>
        <Button variant="outlined" onClick={() => navigate('/wallets')} sx={{ cursor: 'pointer' }}>
          Cancel
        </Button>
        <LoadingButton
          size="large"
          type="submit"
          variant="contained"
          loading={submitting}
          sx={{ minWidth: 140, cursor: 'pointer' }}
        >
          Send
        </LoadingButton>
      </Stack>
    </Stack>
  );
}
