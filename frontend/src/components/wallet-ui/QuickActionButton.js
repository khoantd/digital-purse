import PropTypes from 'prop-types';
import { ButtonBase, Stack, Typography } from '@mui/material';
import Iconify from '../iconify';

export default function QuickActionButton({ icon, label, onClick, color = 'primary' }) {
  return (
    <ButtonBase
      onClick={onClick}
      focusRipple
      sx={{
        width: '100%',
        minHeight: 88,
        py: 2,
        px: 2,
        borderRadius: 2,
        bgcolor: (theme) => theme.palette[color].lighter,
        color: (theme) => theme.palette[color].darker,
        transition: (theme) =>
          theme.transitions.create(['background-color', 'transform'], {
            duration: theme.transitions.duration.shorter,
          }),
        cursor: 'pointer',
        '&:hover': {
          bgcolor: (theme) => theme.palette[color].light,
          transform: 'translateY(-1px)',
        },
        '@media (prefers-reduced-motion: reduce)': {
          transition: 'none',
          '&:hover': { transform: 'none' },
        },
      }}
    >
      <Stack spacing={1} alignItems="center">
        <Iconify icon={icon} width={24} height={24} />
        <Typography variant="subtitle2">{label}</Typography>
      </Stack>
    </ButtonBase>
  );
}

QuickActionButton.propTypes = {
  icon: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
  color: PropTypes.oneOf(['primary', 'secondary', 'info', 'success', 'warning', 'error']),
};
