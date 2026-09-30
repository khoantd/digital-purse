import PropTypes from 'prop-types';
import { Typography } from '@mui/material';
import { fCurrency } from '../../utils/formatNumber';

/**
 * Signed amount styling inspired by Base / Copilot Money activity lists.
 * @param {'credit'|'debit'|'neutral'} tone
 */
export default function MoneyText({ amount, tone = 'neutral', variant = 'subtitle2', sx, ...other }) {
  const value = Number(amount) || 0;
  const prefix = tone === 'credit' ? '+' : tone === 'debit' ? '−' : '';
  const color =
    tone === 'credit' ? 'success.dark' : tone === 'debit' ? 'text.primary' : 'text.primary';

  return (
    <Typography
      variant={variant}
      sx={{
        fontWeight: 600,
        color,
        fontVariantNumeric: 'tabular-nums',
        ...sx,
      }}
      {...other}
    >
      {prefix}
      {fCurrency(Math.abs(value))}
    </Typography>
  );
}

MoneyText.propTypes = {
  amount: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  tone: PropTypes.oneOf(['credit', 'debit', 'neutral']),
  variant: PropTypes.string,
  sx: PropTypes.object,
};
