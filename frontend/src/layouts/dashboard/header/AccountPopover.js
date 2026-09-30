import { useEffect, useMemo, useState } from 'react';

import { useNavigate } from 'react-router-dom';
import { Avatar, Box, Divider, IconButton, MenuItem, Popover, Stack, Typography } from '@mui/material';
import { alpha } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';
import AuthService from '../../../services/AuthService';

function initialsFrom(user) {
  const a = (user?.firstName || '').trim().charAt(0);
  const b = (user?.lastName || '').trim().charAt(0);
  const fallback = (user?.username || '?').trim().charAt(0);
  return `${a}${b}`.toUpperCase() || fallback.toUpperCase();
}

export default function AccountPopover() {
  const [open, setOpen] = useState(null);
  const [currentUser, setCurrentUser] = useState(() => AuthService.getCurrentUser());
  const navigate = useNavigate();
  const { t } = useTranslation('header');

  const menuOptions = useMemo(
    () => [
      {
        labelKey: 'home',
        path: '/',
      },
      {
        labelKey: 'profile',
        path: '/settings?tab=profile',
      },
      {
        labelKey: 'settings',
        path: '/settings?tab=general',
      },
    ],
    []
  );

  useEffect(() => {
    const sync = () => setCurrentUser(AuthService.getCurrentUser());
    window.addEventListener('profile-updated', sync);
    return () => window.removeEventListener('profile-updated', sync);
  }, []);

  const handleOpen = (event) => {
    setCurrentUser(AuthService.getCurrentUser());
    setOpen(event.currentTarget);
  };

  const handleClose = () => {
    setOpen(null);
  };

  const handleNavigate = (path) => {
    handleClose();
    if (path) {
      navigate(path);
    }
  };

  const handleLogout = () => {
    AuthService.logout().finally(() => navigate('/login'));
  };

  const displayName = [currentUser?.firstName, currentUser?.lastName].filter(Boolean).join(' ') || currentUser?.username;

  return (
    <>
      <IconButton
        onClick={handleOpen}
        aria-label={t('accountMenu')}
        sx={{
          p: 0,
          cursor: 'pointer',
          ...(open && {
            '&:before': {
              zIndex: 1,
              content: "''",
              width: '100%',
              height: '100%',
              borderRadius: '50%',
              position: 'absolute',
              bgcolor: (theme) => alpha(theme.palette.grey[900], 0.8),
            },
          }),
        }}
      >
        <Avatar
          sx={{
            bgcolor: 'primary.main',
            width: 40,
            height: 40,
            fontSize: 14,
            fontWeight: 600,
          }}
        >
          {initialsFrom(currentUser)}
        </Avatar>
      </IconButton>
      <Popover
        open={Boolean(open)}
        anchorEl={open}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{
          sx: {
            p: 0,
            mt: 1.5,
            ml: 0.75,
            width: 220,
            '& .MuiMenuItem-root': {
              typography: 'body2',
              borderRadius: 0.75,
            },
          },
        }}
      >
        <Box sx={{ my: 1.5, px: 2.5 }}>
          <Typography variant="subtitle2" noWrap>
            {displayName}
          </Typography>
          {currentUser?.email ? (
            <Typography variant="body2" sx={{ color: 'text.secondary' }} noWrap>
              {currentUser.email}
            </Typography>
          ) : null}
        </Box>
        <Divider sx={{ borderStyle: 'dashed' }} />
        <Stack sx={{ p: 1 }}>
          {menuOptions.map((option) => (
            <MenuItem
              key={option.labelKey}
              onClick={() => handleNavigate(option.path)}
              sx={{ cursor: 'pointer' }}
            >
              {t(option.labelKey)}
            </MenuItem>
          ))}
        </Stack>
        <Divider sx={{ borderStyle: 'dashed' }} />
        <MenuItem onClick={handleLogout} sx={{ m: 1, cursor: 'pointer' }}>
          {t('logout')}
        </MenuItem>
      </Popover>
    </>
  );
}
