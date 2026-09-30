import { LoadingButton } from '@mui/lab';
import { IconButton, InputAdornment, Stack, TextField } from '@mui/material';
import { useSnackbar } from 'notistack';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Iconify from '../../components/iconify';
import AuthService from '../../services/AuthService';

export default function SignupForm() {
  const defaultValues = {
    firstName: '',
    lastName: '',
    username: '',
    email: '',
    password: '',
  };

  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const { enqueueSnackbar } = useSnackbar();
  const [formValues, setFormValues] = useState(defaultValues);
  const { t } = useTranslation(['auth', 'common']);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormValues({
      ...formValues,
      [name]: value,
    });
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const firstName = formValues.firstName.trim();
    const lastName = formValues.lastName.trim();
    const username = formValues.username.trim();
    const email = formValues.email.trim();
    const { password } = formValues;

    if (firstName.length < 3 || firstName.length > 50) {
      enqueueSnackbar(t('auth:signup.firstNameLength'), { variant: 'warning' });
      return;
    }
    if (lastName.length < 3 || lastName.length > 50) {
      enqueueSnackbar(t('auth:signup.lastNameLength'), { variant: 'warning' });
      return;
    }
    if (username.length < 3 || username.length > 20) {
      enqueueSnackbar(t('auth:signup.usernameLength'), { variant: 'warning' });
      return;
    }
    if (password.length < 12 || password.length > 100) {
      enqueueSnackbar(t('auth:signup.passwordLength'), { variant: 'warning' });
      return;
    }

    AuthService.signup({ firstName, lastName, username, email, password })
      .then(() => {
        enqueueSnackbar(t('auth:signup.success'), { variant: 'success' });
        navigate('/login');
      })
      .catch((error) => {
        if (error.response?.data?.errors) {
          error.response?.data?.errors.map((e) => enqueueSnackbar(e.message, { variant: 'error' }));
        } else if (error.response?.data?.message) {
          enqueueSnackbar(error.response?.data?.message, { variant: 'error' });
        } else {
          enqueueSnackbar(error.message, { variant: 'error' });
        }
      });
  };

  return (
    <Stack component="form" onSubmit={handleSubmit} spacing={3} noValidate>
      <TextField
        id="firstName"
        name="firstName"
        label={t('common:fields.firstName')}
        autoComplete="given-name"
        autoFocus
        required
        value={formValues.firstName}
        onChange={handleInputChange}
        inputProps={{ minLength: 3, maxLength: 50 }}
      />
      <TextField
        id="lastName"
        name="lastName"
        label={t('common:fields.lastName')}
        autoComplete="family-name"
        required
        value={formValues.lastName}
        onChange={handleInputChange}
        inputProps={{ minLength: 3, maxLength: 50 }}
      />
      <TextField
        id="username"
        name="username"
        label={t('common:fields.username')}
        autoComplete="username"
        required
        value={formValues.username}
        onChange={handleInputChange}
        inputProps={{ minLength: 3, maxLength: 20 }}
      />
      <TextField
        id="email"
        name="email"
        label={t('auth:signup.email')}
        type="email"
        autoComplete="email"
        required
        value={formValues.email}
        onChange={handleInputChange}
        inputProps={{ minLength: 6, maxLength: 50 }}
      />
      <TextField
        id="password"
        name="password"
        label={t('common:fields.password')}
        autoComplete="new-password"
        type={showPassword ? 'text' : 'password'}
        required
        helperText={t('auth:signup.passwordHelper')}
        value={formValues.password}
        onChange={handleInputChange}
        inputProps={{ minLength: 12, maxLength: 100 }}
        InputProps={{
          endAdornment: (
            <InputAdornment position="end">
              <IconButton
                onClick={() => setShowPassword(!showPassword)}
                edge="end"
                aria-label={showPassword ? t('auth:login.hidePassword') : t('auth:login.showPassword')}
                sx={{ cursor: 'pointer' }}
              >
                <Iconify icon={showPassword ? 'eva:eye-fill' : 'eva:eye-off-fill'} />
              </IconButton>
            </InputAdornment>
          ),
        }}
      />
      <LoadingButton fullWidth size="large" type="submit" variant="contained" sx={{ cursor: 'pointer' }}>
        {t('auth:signup.submit')}
      </LoadingButton>
    </Stack>
  );
}
