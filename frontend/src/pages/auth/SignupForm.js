import { LoadingButton } from '@mui/lab';
import { IconButton, InputAdornment, Stack, TextField } from '@mui/material';
import { useSnackbar } from 'notistack';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
      enqueueSnackbar('First name must be 3–50 characters', { variant: 'warning' });
      return;
    }
    if (lastName.length < 3 || lastName.length > 50) {
      enqueueSnackbar('Last name must be 3–50 characters', { variant: 'warning' });
      return;
    }
    if (username.length < 3 || username.length > 20) {
      enqueueSnackbar('Username must be 3–20 characters', { variant: 'warning' });
      return;
    }
    if (password.length < 12 || password.length > 100) {
      enqueueSnackbar('Password must be 12–100 characters', { variant: 'warning' });
      return;
    }

    // SignupRequest: no roles (SEC-01); password min 12 + common denylist (SEC-13)
    AuthService.signup({ firstName, lastName, username, email, password })
      .then(() => {
        enqueueSnackbar('Signed up successfully', { variant: 'success' });
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
        label="First name"
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
        label="Last name"
        autoComplete="family-name"
        required
        value={formValues.lastName}
        onChange={handleInputChange}
        inputProps={{ minLength: 3, maxLength: 50 }}
      />
      <TextField
        id="username"
        name="username"
        label="Username"
        autoComplete="username"
        required
        value={formValues.username}
        onChange={handleInputChange}
        inputProps={{ minLength: 3, maxLength: 20 }}
      />
      <TextField
        id="email"
        name="email"
        label="Email"
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
        label="Password"
        autoComplete="new-password"
        type={showPassword ? 'text' : 'password'}
        required
        helperText="At least 12 characters; avoid common passwords"
        value={formValues.password}
        onChange={handleInputChange}
        inputProps={{ minLength: 12, maxLength: 100 }}
        InputProps={{
          endAdornment: (
            <InputAdornment position="end">
              <IconButton
                onClick={() => setShowPassword(!showPassword)}
                edge="end"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                sx={{ cursor: 'pointer' }}
              >
                <Iconify icon={showPassword ? 'eva:eye-fill' : 'eva:eye-off-fill'} />
              </IconButton>
            </InputAdornment>
          ),
        }}
      />
      <LoadingButton fullWidth size="large" type="submit" variant="contained" sx={{ cursor: 'pointer' }}>
        Create account
      </LoadingButton>
    </Stack>
  );
}
