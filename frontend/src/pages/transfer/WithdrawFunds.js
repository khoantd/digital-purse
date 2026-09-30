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

export default function WithdrawFunds() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [formValues, setFormValues] = useState(emptyForm);
  const [wallets, setWallets] = useState([]);
  const [selectedWallet, setSelectedWallet] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [loadingWallets, setLoadingWallets] = useState(true);

  const isAdmin = AuthService.isAdmin();

  useEffect(() => {
    const load = () => {
      HttpService.getListWithAuth('/wallets?page=0&size=1000')
        .then((list) => setWallets(list))
        .catch((error) => {
          enqueueSnackbar(error.response?.data?.message || 'Could not load wallets', { variant: 'error' });
        })
        .finally(() => setLoadingWallets(false));
    };
    load();
    const onOrg = () => {
      setLoadingWallets(true);
      load();
    };
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
  }, [enqueueSnackbar]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormValues((prev) => ({ ...prev, [name]: value }));
  };

  const handleWalletChange = (_event, wallet) => {
    setSelectedWallet(wallet);
    setFormValues((prev) => ({
      ...prev,
      fromWalletIban: wallet?.iban || '',
      toWalletIban: wallet?.iban || '',
    }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!formValues.fromWalletIban) {
      enqueueSnackbar('Select a wallet', { variant: 'warning' });
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
      '/wallets/withdrawFunds',
      {
        amount,
        description,
        fromWalletIban: formValues.fromWalletIban,
        toWalletIban: formValues.toWalletIban,
        typeId: 2,
      },
      idempotencyHeaders()
    )
      .then((response) => {
        const pending = response?.status === 'PENDING_APPROVAL';
        enqueueSnackbar(
          pending ? 'Withdrawal submitted for approval' : 'Withdrawal completed successfully',
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
        <Typography variant="h6">Withdraw funds</Typography>
        <Typography variant="body2" color="text.secondary">
          {isAdmin
            ? 'Move money out of any wallet. Large withdrawals may need dual-control approval.'
            : 'Move money out of an organization wallet. Large amounts need another member to approve.'}
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
        value={selectedWallet}
        getOptionLabel={(wallet) => {
          if (!wallet?.name) return '';
          const owner = isAdmin && wallet.user?.fullName ? ` · ${wallet.user.fullName}` : '';
          return `${wallet.name}${owner} · ${wallet.iban}`;
        }}
        isOptionEqualToValue={(option, value) => option?.iban === value?.iban}
        onChange={handleWalletChange}
        renderInput={(params) => <TextField {...params} label="Wallet" required />}
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
          color="warning"
          loading={submitting}
          sx={{ minWidth: 140, cursor: 'pointer' }}
        >
          Withdraw
        </LoadingButton>
      </Stack>
    </Stack>
  );
}
