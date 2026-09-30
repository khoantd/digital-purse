import PropTypes from 'prop-types';
import { Box, Button, Stack, Typography } from '@mui/material';
import Iconify from '../iconify';

export default function EmptyState({ icon, title, description, actionLabel, onAction }) {
  return (
    <Box
      sx={{
        py: { xs: 6, md: 8 },
        px: 3,
        textAlign: 'center',
      }}
    >
      <Stack spacing={2} alignItems="center" maxWidth={420} mx="auto">
        <Box
          sx={{
            width: 64,
            height: 64,
            borderRadius: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            bgcolor: 'primary.lighter',
            color: 'primary.dark',
          }}
        >
          <Iconify icon={icon} width={32} height={32} />
        </Box>
        <Typography variant="h6">{title}</Typography>
        {description && (
          <Typography variant="body2" color="text.secondary">
            {description}
          </Typography>
        )}
        {actionLabel && onAction && (
          <Button
            variant="contained"
            onClick={onAction}
            startIcon={<Iconify icon="eva:plus-fill" />}
            sx={{ mt: 1, cursor: 'pointer' }}
          >
            {actionLabel}
          </Button>
        )}
      </Stack>
    </Box>
  );
}

EmptyState.propTypes = {
  icon: PropTypes.string,
  title: PropTypes.string.isRequired,
  description: PropTypes.string,
  actionLabel: PropTypes.string,
  onAction: PropTypes.func,
};

EmptyState.defaultProps = {
  icon: 'eva:inbox-outline',
  description: '',
  actionLabel: undefined,
  onAction: undefined,
};
