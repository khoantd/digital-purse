import PropTypes from 'prop-types';
import { Box, Card, Stack, Typography } from '@mui/material';
import { fCurrency } from '../../utils/formatNumber';

export default function BalanceHero({ total, walletCount, subtitle }) {
  return (
    <Card
      sx={{
        p: 2,
        borderRadius: 2,
        background: (theme) =>
          `linear-gradient(135deg, ${theme.palette.primary.darker} 0%, ${theme.palette.primary.main} 55%, ${theme.palette.info.main} 100%)`,
        color: 'common.white',
        boxShadow: (theme) => theme.customShadows.primary,
      }}
    >
      <Stack spacing={1}>
        <Typography variant="overline" sx={{ opacity: 0.8, letterSpacing: 1.2 }}>
          Total balance
        </Typography>
        <Typography variant="h2" sx={{ fontWeight: 700, lineHeight: 1.15 }}>
          {fCurrency(total)}
        </Typography>
        <Typography variant="body2" sx={{ opacity: 0.85 }}>
          {subtitle ||
            (walletCount === 1
              ? 'Across 1 wallet'
              : `Across ${walletCount || 0} wallets`)}
        </Typography>
      </Stack>
      <Box
        sx={{
          mt: 2,
          height: 4,
          borderRadius: 1,
          bgcolor: 'rgba(255,255,255,0.2)',
          overflow: 'hidden',
        }}
      />
    </Card>
  );
}

BalanceHero.propTypes = {
  total: PropTypes.number,
  walletCount: PropTypes.number,
  subtitle: PropTypes.string,
};

BalanceHero.defaultProps = {
  total: 0,
  walletCount: 0,
  subtitle: '',
};
