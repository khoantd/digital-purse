import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, ButtonBase, LinearProgress, Stack, Tooltip, Typography } from '@mui/material';
import HttpService from '../../../services/HttpService';
import OrganizationContext from '../../../services/OrganizationContext';

const EMPTY = { transactionQuota: 0, transactionUsed: 0, remaining: 0 };

function progressColor(remaining, quota) {
  if (quota <= 0 || remaining <= 0) return 'error';
  if (remaining < Math.max(100, quota * 0.1)) return 'warning';
  return 'primary';
}

/**
 * Compact lifetime transaction-quota meter for the dashboard header.
 */
export default function TransactionQuotaMeter() {
  const navigate = useNavigate();
  const [data, setData] = useState(EMPTY);
  const [visible, setVisible] = useState(false);

  const load = useCallback(() => {
    const id = OrganizationContext.getActiveOrganizationId();
    if (id == null) {
      setVisible(false);
      setData(EMPTY);
      return;
    }
    HttpService.getWithAuth(`/organizations/${id}/subscription`)
      .then((res) => {
        setData({
          transactionQuota: Number(res?.transactionQuota) || 0,
          transactionUsed: Number(res?.transactionUsed) || 0,
          remaining: Number(res?.remaining) || 0,
        });
        setVisible(true);
      })
      .catch(() => {
        setVisible(false);
      });
  }, []);

  useEffect(() => {
    load();
    window.addEventListener('organization-changed', load);
    window.addEventListener('focus', load);
    return () => {
      window.removeEventListener('organization-changed', load);
      window.removeEventListener('focus', load);
    };
  }, [load]);

  if (!visible || data.transactionQuota <= 0) {
    return null;
  }

  const pct =
    data.transactionQuota > 0
      ? Math.min(100, (data.transactionUsed / data.transactionQuota) * 100)
      : 0;
  const color = progressColor(data.remaining, data.transactionQuota);
  const label = `${data.transactionUsed.toLocaleString()} / ${data.transactionQuota.toLocaleString()}`;
  const tip =
    data.remaining <= 0
      ? 'Transaction quota used up'
      : `${data.remaining.toLocaleString()} transactions remaining`;

  return (
    <Tooltip title={tip} arrow>
      <ButtonBase
        onClick={() => navigate('/settings?tab=subscription')}
        aria-label={`Transaction quota ${label}`}
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'stretch',
          gap: 0.5,
          minWidth: { xs: 96, sm: 160 },
          maxWidth: 200,
          px: 1.25,
          py: 0.75,
          borderRadius: 1,
          border: (theme) => `1px solid ${theme.palette.divider}`,
          bgcolor: 'background.paper',
          textAlign: 'left',
          cursor: 'pointer',
          overflow: 'visible',
          transition: (theme) =>
            theme.transitions.create(['border-color', 'background-color'], {
              duration: theme.transitions.duration.shorter,
            }),
          '&:hover': {
            bgcolor: 'action.hover',
            borderColor: 'text.disabled',
          },
          '@media (prefers-reduced-motion: reduce)': {
            transition: 'none',
          },
        }}
      >
        <Stack direction="row" alignItems="baseline" justifyContent="space-between" spacing={1} sx={{ width: 1 }}>
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ display: { xs: 'none', sm: 'block' }, lineHeight: 1.2, flexShrink: 0 }}
          >
            Transactions
          </Typography>
          <Typography
            variant="body2"
            component="span"
            sx={{
              fontWeight: 700,
              lineHeight: 1.2,
              whiteSpace: 'nowrap',
              fontVariantNumeric: 'tabular-nums',
              ml: { xs: 0, sm: 'auto' },
            }}
          >
            <Box component="span" sx={{ color: 'text.primary' }}>
              {data.transactionUsed.toLocaleString()}
            </Box>
            <Box component="span" sx={{ color: 'text.secondary', fontWeight: 500, mx: 0.5 }}>
              /
            </Box>
            <Box component="span" sx={{ color: 'text.secondary', fontWeight: 600 }}>
              {data.transactionQuota.toLocaleString()}
            </Box>
          </Typography>
        </Stack>
        <Box sx={{ width: 1 }}>
          <LinearProgress
            variant="determinate"
            value={pct}
            color={color}
            sx={{ height: 4, borderRadius: 1 }}
            aria-hidden
          />
        </Box>
      </ButtonBase>
    </Tooltip>
  );
}
