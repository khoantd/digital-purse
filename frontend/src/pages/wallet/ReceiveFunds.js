import {
  Box,
  Button,
  Card,
  Container,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { enqueueSnackbar } from 'notistack';
import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Iconify from '../../components/iconify';
import HttpService from '../../services/HttpService';
import { fCurrency } from '../../utils/formatNumber';

function qrImageUrl(payload) {
  if (!payload) return '';
  return `https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${encodeURIComponent(payload)}`;
}

export default function ReceiveFunds() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [wallets, setWallets] = useState([]);
  const [walletId, setWalletId] = useState('');
  const [loading, setLoading] = useState(true);

  const selected = useMemo(
    () => wallets.find((w) => String(w.id) === String(walletId)) || null,
    [wallets, walletId]
  );

  useEffect(() => {
    const load = () => {
      setLoading(true);
      HttpService.getListWithAuth('/wallets?page=0&size=1000')
        .then((list) => {
          setWallets(list);
          const preferred = searchParams.get('walletId');
          if (preferred && list.some((w) => String(w.id) === preferred)) {
            setWalletId(preferred);
          } else if (list.length > 0) {
            setWalletId(String(list[0].id));
          } else {
            setWalletId('');
          }
        })
        .catch((error) => {
          if (error?.response?.status === 401) {
            navigate('/login');
          } else {
            enqueueSnackbar(error.response?.data?.message || 'Could not load wallets', { variant: 'error' });
          }
        })
        .finally(() => setLoading(false));
    };
    load();
    const onOrg = () => load();
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
  }, [enqueueSnackbar, navigate, searchParams]);

  const copyPayload = async () => {
    if (!selected?.vietQrPayload) return;
    try {
      await navigator.clipboard.writeText(selected.vietQrPayload);
      enqueueSnackbar('VietQR payload copied', { variant: 'success' });
    } catch {
      enqueueSnackbar('Could not copy', { variant: 'error' });
    }
  };

  return (
    <>
      <Helmet>
        <title> Receive | Digital Purse </title>
      </Helmet>
      <Container sx={{ minWidth: '100%' }}>
        <Stack spacing={0.5} sx={{ mb: 3 }}>
          <Typography variant="h4">Receive with VietQR</Typography>
          <Typography variant="body2" color="text.secondary">
            Show this QR so others can pay into an organization VND wallet (demo Mock rail — not a live bank transfer).
          </Typography>
        </Stack>

        <Card sx={{ p: { xs: 2.5, md: 4 }, borderRadius: 2, maxWidth: 560 }}>
          {loading ? (
            <Typography variant="body2" color="text.secondary">
              Loading…
            </Typography>
          ) : wallets.length === 0 ? (
            <Stack spacing={2} alignItems="flex-start">
              <Typography variant="body2" color="text.secondary">
                Create a wallet first to generate a VietQR code.
              </Typography>
              <Button variant="contained" onClick={() => navigate('/wallets/new')}>
                New wallet
              </Button>
            </Stack>
          ) : (
            <Stack spacing={3}>
              <TextField
                select
                label="Wallet"
                value={walletId}
                onChange={(e) => setWalletId(e.target.value)}
                fullWidth
              >
                {wallets.map((w) => (
                  <MenuItem key={w.id} value={String(w.id)}>
                    {w.name} · {fCurrency(w.balance)}
                  </MenuItem>
                ))}
              </TextField>

              {selected && (
                <>
                  <Stack alignItems="center" spacing={1.5}>
                    <Box
                      component="img"
                      src={qrImageUrl(selected.vietQrPayload)}
                      alt={`VietQR for ${selected.name}`}
                      sx={{
                        width: 240,
                        height: 240,
                        borderRadius: 1,
                        border: (theme) => `1px solid ${theme.palette.divider}`,
                        bgcolor: 'common.white',
                      }}
                    />
                    <Typography variant="subtitle1">{selected.name}</Typography>
                    <Typography variant="h5" sx={{ fontWeight: 700 }}>
                      {fCurrency(selected.balance)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {selected.currency || 'VND'} · Demo bank BIN 970436
                    </Typography>
                  </Stack>

                  <Box
                    sx={{
                      p: 1.5,
                      borderRadius: 1,
                      bgcolor: 'background.neutral',
                      fontFamily: 'monospace',
                      fontSize: 12,
                      wordBreak: 'break-all',
                    }}
                  >
                    {selected.iban}
                  </Box>

                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                    <Button
                      variant="outlined"
                      startIcon={<Iconify icon="eva:copy-outline" />}
                      onClick={copyPayload}
                      disabled={!selected.vietQrPayload}
                    >
                      Copy VietQR payload
                    </Button>
                    <Button
                      variant="contained"
                      startIcon={<Iconify icon="eva:arrow-forward-fill" />}
                      onClick={() => navigate('/transfers')}
                    >
                      Go to transfers
                    </Button>
                  </Stack>
                </>
              )}
            </Stack>
          )}
        </Card>
      </Container>
    </>
  );
}
