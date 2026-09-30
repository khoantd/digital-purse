import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Avatar,
  Box,
  Card,
  CardContent,
  Grid,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { enqueueSnackbar } from 'notistack';
import { useNavigate } from 'react-router-dom';
import Iconify from '../../components/iconify';
import AuthService from '../../services/AuthService';
import HttpService from '../../services/HttpService';

const PASSWORD_MIN = 12;

function initialsFrom(firstName, lastName, username) {
  const a = (firstName || '').trim().charAt(0);
  const b = (lastName || '').trim().charAt(0);
  const fallback = (username || '?').trim().charAt(0);
  return `${a}${b}`.toUpperCase() || fallback.toUpperCase();
}

/**
 * Personal profile + password forms for use inside Settings (Profile tab).
 */
export default function ProfilePanel() {
  const { t } = useTranslation(['settings', 'common']);
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '' });
  const [savingProfile, setSavingProfile] = useState(false);
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    HttpService.getWithAuth('/users/me')
      .then((me) => {
        setProfile(me);
        setForm({
          firstName: me.firstName || '',
          lastName: me.lastName || '',
          email: me.email || '',
        });
        AuthService.updateCurrentUser({
          id: me.id,
          username: me.username,
          firstName: me.firstName,
          lastName: me.lastName,
          email: me.email,
          roles: me.roles,
        });
      })
      .catch((error) => {
        if (error?.response?.status === 401) {
          navigate('/login');
        } else {
          enqueueSnackbar(error.response?.data?.message || t('snackbar.profileLoadFailed'), { variant: 'error' });
        }
      })
      .finally(() => setLoading(false));
  }, [navigate, t]);

  useEffect(() => {
    load();
  }, [load]);

  const profileDirty = useMemo(() => {
    if (!profile) return false;
    return (
      form.firstName.trim() !== (profile.firstName || '') ||
      form.lastName.trim() !== (profile.lastName || '') ||
      form.email.trim() !== (profile.email || '')
    );
  }, [form, profile]);

  const passwordReady = useMemo(() => {
    const { currentPassword, newPassword, confirmPassword } = passwordForm;
    return (
      currentPassword.length > 0 &&
      newPassword.length >= PASSWORD_MIN &&
      newPassword === confirmPassword
    );
  }, [passwordForm]);

  const saveProfile = (event) => {
    event.preventDefault();
    const firstName = form.firstName.trim();
    const lastName = form.lastName.trim();
    const email = form.email.trim();
    if (firstName.length < 3 || lastName.length < 3) {
      enqueueSnackbar(t('snackbar.nameMinLength'), { variant: 'warning' });
      return;
    }
    if (!email.includes('@')) {
      enqueueSnackbar(t('snackbar.invalidEmail'), { variant: 'warning' });
      return;
    }
    setSavingProfile(true);
    setStatusMessage(t('profile.statusSavingProfile'));
    HttpService.putWithAuth('/users/me', { firstName, lastName, email })
      .then((me) => {
        setProfile(me);
        setForm({
          firstName: me.firstName || '',
          lastName: me.lastName || '',
          email: me.email || '',
        });
        AuthService.updateCurrentUser({
          id: me.id,
          username: me.username,
          firstName: me.firstName,
          lastName: me.lastName,
          email: me.email,
          roles: me.roles,
        });
        setStatusMessage(t('profile.statusProfileSaved'));
        enqueueSnackbar(t('snackbar.profileUpdated'), { variant: 'success' });
        window.dispatchEvent(new Event('profile-updated'));
      })
      .catch((error) => {
        setStatusMessage('');
        enqueueSnackbar(error.response?.data?.message || t('snackbar.profileUpdateFailed'), { variant: 'error' });
      })
      .finally(() => setSavingProfile(false));
  };

  const changePassword = (event) => {
    event.preventDefault();
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      enqueueSnackbar(t('snackbar.passwordMismatch'), { variant: 'warning' });
      return;
    }
    if (passwordForm.newPassword.length < PASSWORD_MIN) {
      enqueueSnackbar(t('snackbar.passwordMinLength', { min: PASSWORD_MIN }), { variant: 'warning' });
      return;
    }
    setSavingPassword(true);
    setStatusMessage(t('profile.statusUpdatingPassword'));
    HttpService.putWithAuth('/users/me/password', {
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
      .then(() => {
        setStatusMessage(t('profile.statusPasswordUpdated'));
        enqueueSnackbar(t('snackbar.passwordUpdatedSignIn'), { variant: 'success' });
        return AuthService.logout();
      })
      .then(() => navigate('/login'))
      .catch((error) => {
        setStatusMessage('');
        enqueueSnackbar(error.response?.data?.message || t('snackbar.passwordChangeFailed'), { variant: 'error' });
      })
      .finally(() => setSavingPassword(false));
  };

  const passwordAdornment = (show, setShow, label) => (
    <InputAdornment position="end">
      <IconButton
        onClick={() => setShow(!show)}
        edge="end"
        aria-label={show ? t('profile.hideField', { field: label }) : t('profile.showField', { field: label })}
        sx={{ cursor: 'pointer' }}
      >
        <Iconify icon={show ? 'eva:eye-fill' : 'eva:eye-off-fill'} />
      </IconButton>
    </InputAdornment>
  );

  if (loading && !profile) {
    return (
      <Typography variant="body2" color="text.secondary">
        {t('profile.loading')}
      </Typography>
    );
  }

  return (
    <>
      <Box
        component="div"
        role="status"
        aria-live="polite"
        sx={{
          position: 'absolute',
          width: 1,
          height: 1,
          overflow: 'hidden',
          clip: 'rect(0 0 0 0)',
          whiteSpace: 'nowrap',
        }}
      >
        {statusMessage}
      </Box>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} sx={{ mb: 3 }}>
        <Avatar
          sx={{
            width: 56,
            height: 56,
            bgcolor: 'primary.main',
            fontSize: 20,
            fontWeight: 600,
          }}
          aria-hidden
        >
          {initialsFrom(profile?.firstName, profile?.lastName, profile?.username)}
        </Avatar>
        <Box>
          <Typography variant="h6">
            {profile?.firstName} {profile?.lastName}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {profile?.email || '—'} · @{profile?.username}
          </Typography>
        </Box>
      </Stack>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 0.5 }}>
                {t('profile.cardTitle')}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {t('profile.cardDescription')}
              </Typography>
              <Box component="form" onSubmit={saveProfile} noValidate>
                <Stack spacing={2}>
                  <TextField
                    label={t('common:fields.username')}
                    value={profile?.username || ''}
                    disabled
                    fullWidth
                    helperText={t('profile.usernameHelper')}
                  />
                  <TextField
                    id="profile-first-name"
                    label={t('common:fields.firstName')}
                    value={form.firstName}
                    onChange={(e) => setForm((f) => ({ ...f, firstName: e.target.value }))}
                    fullWidth
                    required
                    inputProps={{ minLength: 3, maxLength: 50 }}
                  />
                  <TextField
                    id="profile-last-name"
                    label={t('common:fields.lastName')}
                    value={form.lastName}
                    onChange={(e) => setForm((f) => ({ ...f, lastName: e.target.value }))}
                    fullWidth
                    required
                    inputProps={{ minLength: 3, maxLength: 50 }}
                  />
                  <TextField
                    id="profile-email"
                    label={t('common:fields.email')}
                    type="email"
                    value={form.email}
                    onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                    fullWidth
                    required
                  />
                  <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <LoadingButton
                      type="submit"
                      variant="contained"
                      loading={savingProfile}
                      disabled={!profileDirty}
                      sx={{ cursor: profileDirty ? 'pointer' : 'default' }}
                    >
                      {t('general.save')}
                    </LoadingButton>
                  </Box>
                </Stack>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 0.5 }}>
                {t('profile.securityTitle')}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {t('profile.securityDescription')}
              </Typography>
              <Box component="form" onSubmit={changePassword} noValidate>
                <Stack spacing={2}>
                  <TextField
                    id="profile-current-password"
                    label={t('profile.currentPassword')}
                    type={showCurrent ? 'text' : 'password'}
                    value={passwordForm.currentPassword}
                    onChange={(e) =>
                      setPasswordForm((f) => ({ ...f, currentPassword: e.target.value }))
                    }
                    fullWidth
                    required
                    InputProps={{
                      endAdornment: passwordAdornment(showCurrent, setShowCurrent, t('profile.currentPassword')),
                    }}
                  />
                  <TextField
                    id="profile-new-password"
                    label={t('profile.newPassword')}
                    type={showNew ? 'text' : 'password'}
                    value={passwordForm.newPassword}
                    onChange={(e) => setPasswordForm((f) => ({ ...f, newPassword: e.target.value }))}
                    fullWidth
                    required
                    helperText={t('profile.passwordHelper', { min: PASSWORD_MIN })}
                    InputProps={{
                      endAdornment: passwordAdornment(showNew, setShowNew, t('profile.newPassword')),
                    }}
                  />
                  <TextField
                    id="profile-confirm-password"
                    label={t('profile.confirmPassword')}
                    type={showConfirm ? 'text' : 'password'}
                    value={passwordForm.confirmPassword}
                    onChange={(e) =>
                      setPasswordForm((f) => ({ ...f, confirmPassword: e.target.value }))
                    }
                    fullWidth
                    required
                    error={
                      passwordForm.confirmPassword.length > 0 &&
                      passwordForm.newPassword !== passwordForm.confirmPassword
                    }
                    helperText={
                      passwordForm.confirmPassword.length > 0 &&
                      passwordForm.newPassword !== passwordForm.confirmPassword
                        ? t('profile.passwordMismatch')
                        : ' '
                    }
                    InputProps={{
                      endAdornment: passwordAdornment(showConfirm, setShowConfirm, t('profile.confirmPassword')),
                    }}
                  />
                  <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <LoadingButton
                      type="submit"
                      variant="contained"
                      color="inherit"
                      loading={savingPassword}
                      disabled={!passwordReady}
                      sx={{ cursor: passwordReady ? 'pointer' : 'default' }}
                    >
                      {t('profile.updatePassword')}
                    </LoadingButton>
                  </Box>
                </Stack>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </>
  );
}
