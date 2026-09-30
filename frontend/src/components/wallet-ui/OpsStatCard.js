import PropTypes from 'prop-types';
import { alpha, styled } from '@mui/material/styles';
import { Card, Stack, Typography } from '@mui/material';
import Iconify from '../iconify';
import { fCurrency, fNumber } from '../../utils/formatNumber';

const StyledIcon = styled('div')(({ theme }) => ({
  display: 'flex',
  borderRadius: '50%',
  alignItems: 'center',
  width: theme.spacing(5),
  height: theme.spacing(5),
  justifyContent: 'center',
  flexShrink: 0,
}));

/**
 * Compact money / count stat for Dashboard ops row.
 * Pass `format="money"` for VND amounts, `format="count"` for integers.
 */
export default function OpsStatCard({
  title,
  value,
  icon,
  color = 'primary',
  format = 'money',
  subtitle,
  onClick,
  sx,
  ...other
}) {
  const display =
    format === 'count' ? fNumber(value) : fCurrency(Number(value) || 0);

  return (
    <Card
      onClick={onClick}
      sx={{
        p: 2,
        height: '100%',
        borderRadius: 2,
        boxShadow: 0,
        bgcolor: (theme) => theme.palette[color].lighter,
        color: (theme) => theme.palette[color].darker,
        cursor: onClick ? 'pointer' : 'default',
        transition: (theme) =>
          theme.transitions.create(['box-shadow', 'transform'], {
            duration: theme.transitions.duration.shorter,
          }),
        '&:hover': onClick
          ? {
              boxShadow: (theme) => theme.customShadows.z8,
              transform: 'translateY(-2px)',
            }
          : undefined,
        ...sx,
      }}
      {...other}
    >
      <Stack direction="row" spacing={2} alignItems="center">
        <StyledIcon
          sx={{
            color: (theme) => theme.palette[color].dark,
            backgroundImage: (theme) =>
              `linear-gradient(135deg, ${alpha(theme.palette[color].dark, 0)} 0%, ${alpha(
                theme.palette[color].dark,
                0.24
              )} 100%)`,
          }}
        >
          <Iconify icon={icon} width={20} height={20} />
        </StyledIcon>
        <Stack spacing={0.25} sx={{ minWidth: 0 }}>
          <Typography variant="subtitle2" sx={{ opacity: 0.72 }}>
            {title}
          </Typography>
          <Typography
            variant="h5"
            sx={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums', lineHeight: 1.2 }}
            noWrap
          >
            {display}
          </Typography>
          {subtitle ? (
            <Typography variant="caption" sx={{ opacity: 0.7 }}>
              {subtitle}
            </Typography>
          ) : null}
        </Stack>
      </Stack>
    </Card>
  );
}

OpsStatCard.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  icon: PropTypes.string,
  color: PropTypes.string,
  format: PropTypes.oneOf(['money', 'count']),
  subtitle: PropTypes.string,
  onClick: PropTypes.func,
  sx: PropTypes.object,
};
