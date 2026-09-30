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

export default function AddFunds() {
  const { t } = useTranslation(['transfers', 'common', 'wallets']);
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
      // Active org via X-Organization-Id; admin list is unscoped on the server.
      HttpService.getListWithAuth('/wallets?page=0&size=1000')
        .then((list) => setWallets(list))
        .catch((error) => {
          enqueueSnackbar(error.response?.data?.message || t('wallets:messages.loadFailed'), {
            variant: 'error',
          });
        })
        .finally(() => setLoadingWallets(false));
    };
    load();
    const onOrg = () => {
      setLoadingWallets(true);
      setSelectedWallet(null);
      setFormValues(emptyForm);
      load();
    };
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
  }, [enqueueSnackbar, t]);

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
    if (!formValues.toWalletIban) {
      enqueueSnackbar(t('transfers:addFunds.selectWallet'), { variant: 'warning' });
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
    // TransactionRequest: amount, description, fromWalletIban, toWalletIban, typeId
    HttpService.postWithAuth(
      '/wallets/addFunds',
      {
        amount,
        description,
        fromWalletIban: formValues.fromWalletIban,
        toWalletIban: formValues.toWalletIban,
        typeId: 3,
      },
      idempotencyHeaders()
    )
      .then(() => {
        enqueueSnackbar(t('transfers:addFunds.success'), { variant: 'success' });
        navigate('/transactions');
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
        <Typography variant="h6">{t('transfers:addFunds.title')}</Typography>
        <Typography variant="body2" color="text.secondary">
          {isAdmin ? t('transfers:addFunds.subtitleAdmin') : t('transfers:addFunds.subtitleOrg')}
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
        id="toWalletIban"
        loading={loadingWallets}
        noOptionsText={
          loadingWallets ? t('common:status.loading') : t('transfers:helpers.noWallets')
        }
        options={wallets}
        value={selectedWallet}
        getOptionLabel={(wallet) => {
          if (!wallet?.name) return '';
          const owner = isAdmin && wallet.user?.fullName ? ` · ${wallet.user.fullName}` : '';
          return `${wallet.name}${owner} · ${wallet.iban}`;
        }}
        isOptionEqualToValue={(option, value) => option?.iban === value?.iban}
        onChange={handleWalletChange}
        renderInput={(params) => <TextField {...params} label={t('transfers:fields.wallet')} required />}
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
          {t('transfers:addFunds.submit')}
        </LoadingButton>
      </Stack>
    </Stack>
  );
}
