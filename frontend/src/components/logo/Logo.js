import PropTypes from 'prop-types';
import { forwardRef } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Link, Typography } from '@mui/material';

const Logo = forwardRef(({ disabledLink = false, sx, ...other }, ref) => {
  const logo = (
    <Box
      ref={ref}
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 1,
        cursor: 'pointer',
        ...sx,
      }}
      {...other}
    >
      <Box
        component="img"
        src={`${process.env.PUBLIC_URL}/logo.png`}
        alt=""
        sx={{ width: 40, height: 40, flexShrink: 0 }}
      />
      <Typography
        variant="subtitle1"
        component="span"
        sx={{
          fontWeight: 700,
          letterSpacing: 0.2,
          color: 'inherit',
          whiteSpace: 'nowrap',
        }}
      >
        Digital Purse
      </Typography>
    </Box>
  );

  if (disabledLink) {
    return logo;
  }

  return (
    <Link to="/" component={RouterLink} sx={{ display: 'contents' }} underline="none">
      {logo}
    </Link>
  );
});

Logo.propTypes = {
  sx: PropTypes.object,
  disabledLink: PropTypes.bool,
};

export default Logo;
