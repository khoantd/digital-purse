import PropTypes from 'prop-types';
import { Box, Button, Card, Chip, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import Iconify from '../iconify';
import { fCurrency } from '../../utils/formatNumber';

function maskIban(iban) {
  if (!iban || iban.length < 8) return iban || '—';
  return `${iban.slice(0, 4)} ··· ${iban.slice(-4)}`;
}

export default function WalletCard({
  name,
  balance,
  iban,
  currency,
  ownerName,
  ownerType,
  customerName,
  onClick,
  onDetails,
  onEdit,
}) {
  const { t } = useTranslation('common');
  const hasActions = Boolean(onDetails || onEdit);
  const typeLabel =
    ownerType === 'CUSTOMER'
      ? customerName
        ? t('ownerType.customerNamed', { name: customerName })
        : t('ownerType.customer')
      : t('ownerType.organization');

  return (
    <Card
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={
        onClick
          ? (e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onClick();
              }
            }
          : undefined
      }
      sx={{
        p: 2.5,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        borderRadius: 2,
        border: (theme) => `1px solid ${theme.palette.divider}`,
        boxShadow: 'none',
        cursor: onClick ? 'pointer' : 'default',
        transition: (theme) =>
          theme.transitions.create(['border-color', 'box-shadow'], {
            duration: theme.transitions.duration.shorter,
          }),
        '&:hover': onClick
          ? {
              borderColor: 'primary.main',
              boxShadow: (theme) => theme.customShadows.z8,
            }
          : undefined,
        '@media (prefers-reduced-motion: reduce)': {
          transition: 'none',
        },
      }}
    >
      <Stack direction="row" spacing={2} alignItems="flex-start" sx={{ flex: 1, minHeight: 0 }}>
        <Box
          sx={{
            width: 44,
            height: 44,
            borderRadius: 1.5,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            bgcolor: 'primary.lighter',
            color: 'primary.dark',
            flexShrink: 0,
          }}
        >
          <Iconify icon="ant-design:wallet-outlined" width={22} height={22} />
        </Box>
        <Stack spacing={0.75} sx={{ minWidth: 0, flex: 1 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
            <Typography variant="subtitle1" noWrap>
              {name}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ flexShrink: 0 }}>
              {currency || 'VND'}
            </Typography>
          </Stack>
          <Chip
            size="small"
            label={typeLabel}
            sx={{ alignSelf: 'flex-start', maxWidth: '100%', height: 22, '& .MuiChip-label': { px: 1 } }}
            variant="outlined"
          />
          {ownerName && (
            <Typography variant="caption" color="text.secondary" noWrap>
              {ownerName}
            </Typography>
          )}
          <Box sx={{ flexGrow: 1, minHeight: 8 }} />
          <Typography variant="h5" sx={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
            {fCurrency(balance)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {maskIban(iban)}
          </Typography>
        </Stack>
      </Stack>

      {hasActions && (
        <Stack
          direction="row"
          spacing={1}
          sx={{ mt: 2, pt: 1.5, borderTop: (theme) => `1px solid ${theme.palette.divider}` }}
        >
          {onDetails && (
            <Button
              size="small"
              variant="outlined"
              color="inherit"
              startIcon={<Iconify icon="eva:eye-outline" width={16} height={16} />}
              aria-label={t('actions.detailsFor', { name })}
              onClick={(e) => {
                e.stopPropagation();
                onDetails();
              }}
              sx={{ cursor: 'pointer', flex: 1 }}
            >
              {t('actions.details')}
            </Button>
          )}
          {onEdit && (
            <Button
              size="small"
              variant="outlined"
              color="inherit"
              startIcon={<Iconify icon="eva:edit-outline" width={16} height={16} />}
              aria-label={t('actions.editFor', { name })}
              onClick={(e) => {
                e.stopPropagation();
                onEdit();
              }}
              sx={{ cursor: 'pointer', flex: 1 }}
            >
              {t('actions.edit')}
            </Button>
          )}
        </Stack>
      )}
    </Card>
  );
}

WalletCard.propTypes = {
  name: PropTypes.string.isRequired,
  balance: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  iban: PropTypes.string,
  currency: PropTypes.string,
  ownerName: PropTypes.string,
  ownerType: PropTypes.string,
  customerName: PropTypes.string,
  onClick: PropTypes.func,
  onDetails: PropTypes.func,
  onEdit: PropTypes.func,
};

WalletCard.defaultProps = {
  balance: 0,
  iban: '',
  currency: 'VND',
  ownerName: '',
  ownerType: 'ORGANIZATION',
  customerName: '',
  onClick: undefined,
  onDetails: undefined,
  onEdit: undefined,
};
