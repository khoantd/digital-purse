import { Box, Container, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { Helmet } from 'react-helmet-async';
import { Link } from 'react-router-dom';
import Logo from '../../components/logo';
import useResponsive from '../../hooks/useResponsive';
import LoginForm from './LoginForm';

const StyledRoot = styled('div')(({ theme }) => ({
  [theme.breakpoints.up('md')]: {
    display: 'flex',
    minHeight: '100vh',
  },
}));

const StyledSection = styled('div')(({ theme }) => ({
  width: '100%',
  maxWidth: 480,
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  boxShadow: theme.customShadows.card,
  background: `linear-gradient(165deg, ${theme.palette.primary.darker} 0%, ${theme.palette.primary.main} 50%, ${theme.palette.info.dark} 100%)`,
  color: theme.palette.common.white,
}));

const StyledContent = styled('div')(({ theme }) => ({
  maxWidth: 420,
  margin: 'auto',
  minHeight: '100vh',
  display: 'flex',
  justifyContent: 'center',
  flexDirection: 'column',
  padding: theme.spacing(12, 0),
}));

export default function Login() {
  const mdUp = useResponsive('up', 'md');

  return (
    <>
      <Helmet>
        <title> Log in | Digital Purse </title>
      </Helmet>
      <StyledRoot>
        <Logo
          sx={{
            position: 'fixed',
            top: { xs: 16, sm: 24, md: 40 },
            left: { xs: 16, sm: 24, md: 40 },
            zIndex: 1,
            color: { xs: 'text.primary', md: 'common.white' },
          }}
        />
        {mdUp && (
          <StyledSection>
            <Box sx={{ px: 5, mt: 10, mb: 5 }}>
              <Typography variant="h3" sx={{ mb: 2, color: 'common.white' }}>
                Welcome back
              </Typography>
              <Typography variant="body1" sx={{ opacity: 0.85, maxWidth: 320 }}>
                Sign in to Digital Purse — organization wallets, transfers, and dual-control approvals.
              </Typography>
            </Box>
            <Box sx={{ px: 3, pb: 6 }}>
              <img src="/assets/illustrations/illustration_login.png" alt="Login" style={{ maxWidth: '100%' }} />
            </Box>
          </StyledSection>
        )}
        <Container maxWidth="sm">
          <StyledContent>
            <Typography variant="h4" gutterBottom>
              Log in
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
              Don&apos;t have an account?{' '}
              <Link to="/signup" style={{ textDecoration: 'none', fontWeight: 600, cursor: 'pointer' }}>
                Sign up
              </Link>
            </Typography>
            <LoginForm />
          </StyledContent>
        </Container>
      </StyledRoot>
    </>
  );
}
