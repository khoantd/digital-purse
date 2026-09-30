import { LoadingButton } from '@mui/lab';
import {
  Autocomplete,
  Button,
  Card,
  Container,
  FormControl,
  FormControlLabel,
  FormLabel,
  InputAdornment,
  Radio,
  RadioGroup,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import { useEffect, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import HttpService from '../../services/HttpService';

const OWNER_ORGANIZATION = 'ORGANIZATION';
const OWNER_CUSTOMER = 'CUSTOMER';

export default function NewWallet() {
  const defaultValues = {
    name: '',
    balance: '',
    ownerType: OWNER_ORGANIZATION,
  };

  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [formValues, setFormValues] = useState(defaultValues);
  const [customers, setCustomers] = useState([]);
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [loadingCustomers, setLoadingCustomers] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const loadCustomers = () => {
      setLoadingCustomers(true);
      HttpService.getListWithAuth('/customers')
        .then(setCustomers)
        .catch(() => setCustomers([]))
        .finally(() => setLoadingCustomers(false));
    };
    loadCustomers();
    const onOrg = () => {
      setSelectedCustomer(null);
      loadCustomers();
    };
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormValues({
      ...formValues,
      [name]: value,
    });
  };

  const handleOwnerTypeChange = (e) => {
    const ownerType = e.target.value;
    setFormValues((prev) => ({ ...prev, ownerType }));
    if (ownerType === OWNER_ORGANIZATION) {
      setSelectedCustomer(null);
    }
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const name = formValues.name.trim();
    const balance = Number(formValues.balance);
    if (name.length < 3 || name.length > 50) {
      enqueueSnackbar('Wallet name must be 3–50 characters', { variant: 'warning' });
      return;
    }
    if (!Number.isFinite(balance) || balance <= 0) {
      enqueueSnackbar('Starting balance must be greater than zero', { variant: 'warning' });
      return;
    }
    if (formValues.ownerType === OWNER_CUSTOMER && !selectedCustomer?.id) {
      enqueueSnackbar('Select a customer for this wallet', { variant: 'warning' });
      return;
    }

    const payload = {
      name,
      balance,
      ownerType: formValues.ownerType,
    };
    if (formValues.ownerType === OWNER_CUSTOMER) {
      payload.customerId = selectedCustomer.id;
    }

    setSubmitting(true);
    HttpService.postWithAuth('/wallets', payload)
      .then(() => {
        enqueueSnackbar('Wallet created successfully', { variant: 'success' });
        navigate('/wallets');
      })
      .catch((error) => {
        if (error.response?.data?.errors) {
          error.response?.data?.errors.map((e) => enqueueSnackbar(e.message, { variant: 'error' }));
        } else if (error.response?.data?.message) {
          enqueueSnackbar(error.response?.data?.message, { variant: 'error' });
        } else {
          enqueueSnackbar(error.message, { variant: 'error' });
        }
      })
      .finally(() => setSubmitting(false));
  };

  return (
    <>
      <Helmet>
        <title> New Wallet | Digital Purse </title>
      </Helmet>
      <Container sx={{ minWidth: '100%' }}>
        <Stack spacing={0.5} sx={{ mb: 3 }}>
          <Typography variant="h4">New wallet</Typography>
          <Typography variant="body2" color="text.secondary">
            Choose whether this wallet is for the organization or a customer contact, then set a name and starting
            balance. A Vietnam account number (VN) is assigned automatically.
          </Typography>
        </Stack>
        <Card sx={{ p: { xs: 2.5, md: 4 }, borderRadius: 2, maxWidth: 480 }}>
          <Stack component="form" onSubmit={handleSubmit} spacing={3} noValidate>
            <FormControl>
              <FormLabel id="owner-type-label">Owner type</FormLabel>
              <RadioGroup
                row
                aria-labelledby="owner-type-label"
                name="ownerType"
                value={formValues.ownerType}
                onChange={handleOwnerTypeChange}
              >
                <FormControlLabel value={OWNER_ORGANIZATION} control={<Radio />} label="Organization" />
                <FormControlLabel value={OWNER_CUSTOMER} control={<Radio />} label="Customer" />
              </RadioGroup>
            </FormControl>
            {formValues.ownerType === OWNER_CUSTOMER && (
              <Autocomplete
                options={customers}
                loading={loadingCustomers}
                value={selectedCustomer}
                onChange={(_event, customer) => setSelectedCustomer(customer)}
                getOptionLabel={(option) => option?.name || ''}
                isOptionEqualToValue={(option, value) => option?.id === value?.id}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Customer"
                    required
                    helperText="Active customers in the current organization"
                  />
                )}
              />
            )}
            <TextField
              id="name"
              name="name"
              label="Wallet name"
              autoComplete="given-name"
              autoFocus
              required
              value={formValues.name}
              onChange={handleInputChange}
              inputProps={{ minLength: 3, maxLength: 50 }}
              helperText="3–50 characters"
            />
            <TextField
              id="balance"
              name="balance"
              label="Starting balance"
              autoComplete="off"
              required
              value={formValues.balance}
              onChange={handleInputChange}
              inputProps={{ inputMode: 'numeric', min: 1, step: 1 }}
              InputProps={{
                endAdornment: <InputAdornment position="end">₫</InputAdornment>,
              }}
              helperText="Vietnamese đồng (VND), whole amounts"
            />
            <Typography variant="caption" color="text.secondary">
              Account number is generated on the server (VND · VietQR-ready). Owner type is a label only; access stays
              org-scoped.
            </Typography>
            <Stack spacing={2} direction="row" justifyContent="flex-end">
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
                Create wallet
              </LoadingButton>
            </Stack>
          </Stack>
        </Card>
      </Container>
    </>
  );
}
