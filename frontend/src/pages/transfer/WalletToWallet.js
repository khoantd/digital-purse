import { LoadingButton } from '@mui/lab';
import { Autocomplete, Button, InputAdornment, Stack, TextField, Typography } from '@mui/material';
import { useSnackbar } from 'notistack';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation(['transfers', 'common', 'wallets']);
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
          enqueueSnackbar(error.response?.data?.message || t('wallets:messages.loadFailed'), {
            variant: 'error',
          });
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
  }, [enqueueSnackbar, t]);

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
      enqueueSnackbar(t('transfers:send.selectSender'), { variant: 'warning' });
      return;
    }
    const toWalletIban = formValues.toWalletIban.trim();
    if (!toWalletIban) {
      enqueueSnackbar(t('transfers:send.enterRecipient'), { variant: 'warning' });
      return;
    }
    const amount = Number(formValues.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      enqueueSnackbar(t('transfers:validation.validAmount'), { variant: 'warning' });
      return;
    }
    const description = formValues.description.trim();
    if (!description || description.length > 50) {
      enqueueSnackbar(t('transfers:validation.noteRequired'), { variant: 'warning' });
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
        enqueueSnackbar(pending ? t('transfers:send.pending') : t('transfers:send.success'), {
          variant: 'success',
        });
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
        <Typography variant="h6">{t('transfers:send.title')}</Typography>
        <Typography variant="body2" color="text.secondary">
          {isAdmin ? t('transfers:send.subtitleAdmin') : t('transfers:send.subtitleOrg')}
        </Typography>
      </Stack>
      <TextField
        id="amount"
        name="amount"
        label={t('common:fields.amount')}
        autoFocus
        required
        value={formValues.amount}
        onChange={handleInputChange}
        inputProps={{ inputMode: 'numeric', min: 1, step: 1 }}
        InputProps={{
          endAdornment: <InputAdornment position="end">₫</InputAdornment>,
        }}
        helperText={t('transfers:helpers.vnd')}
      />
      <Autocomplete
        ListboxProps={{ style: { maxHeight: 200, overflow: 'auto' } }}
        disablePortal
        id="fromWalletIban"
        loading={loadingWallets}
        noOptionsText={
          loadingWallets ? t('common:status.loading') : t('transfers:helpers.noWallets')
        }
        options={wallets}
        value={selectedFromWallet}
        getOptionLabel={(wallet) => {
          if (!wallet?.name) return '';
          const owner = isAdmin && wallet.user?.fullName ? ` · ${wallet.user.fullName}` : '';
          return `${wallet.name}${owner} · ${wallet.iban}`;
        }}
        isOptionEqualToValue={(option, value) => option?.iban === value?.iban}
        onChange={handleFromWalletChange}
        renderInput={(params) => (
          <TextField {...params} label={t('transfers:fields.fromWallet')} required />
        )}
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
            <TextField
              {...params}
              label={t('transfers:fields.customerOptional')}
              helperText={t('transfers:helpers.customerPrefill')}
            />
          )}
        />
      )}
      <TextField
        id="toWalletIban"
        name="toWalletIban"
        label={t('transfers:fields.recipientAccount')}
        autoComplete="off"
        required
        value={formValues.toWalletIban}
        onChange={(e) => {
          setSelectedCustomer(null);
          handleInputChange(e);
        }}
        placeholder={t('transfers:helpers.recipientPlaceholder')}
        helperText={t('transfers:helpers.recipientHint')}
      />
      <TextField
        id="description"
        name="description"
        label={t('transfers:fields.note')}
        autoComplete="off"
        required
        value={formValues.description}
        onChange={handleInputChange}
        inputProps={{ maxLength: 50 }}
        helperText={t('transfers:helpers.noteMax')}
      />
      <Stack spacing={2} direction="row" justifyContent="flex-end" sx={{ pt: 1 }}>
        <Button variant="outlined" onClick={() => navigate('/wallets')} sx={{ cursor: 'pointer' }}>
          {t('common:actions.cancel')}
        </Button>
        <LoadingButton
          size="large"
          type="submit"
          variant="contained"
          loading={submitting}
          sx={{ minWidth: 140, cursor: 'pointer' }}
        >
          {t('transfers:send.submit')}
        </LoadingButton>
      </Stack>
    </Stack>
  );
}
