import { Box, Container, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { Helmet } from 'react-helmet-async';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Logo from '../../components/logo';
import LanguagePopover from '../../layouts/dashboard/header/LanguagePopover';
import useResponsive from '../../hooks/useResponsive';
import SignupForm from './SignupForm';

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

export default function Signup() {
  const mdUp = useResponsive('up', 'md');
  const { t } = useTranslation('auth');

  return (
    <>
      <Helmet>
        <title>{t('signup.helmet')}</title>
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
        <Box
          sx={{
            position: 'fixed',
            top: { xs: 12, sm: 20, md: 36 },
            right: { xs: 12, sm: 20, md: 36 },
            zIndex: 2,
          }}
        >
          <LanguagePopover />
        </Box>
        {mdUp && (
          <StyledSection>
            <Box sx={{ px: 5, mt: 10, mb: 5 }}>
              <Typography variant="h3" sx={{ mb: 2, color: 'common.white' }}>
                {t('signup.heroTitle')}
              </Typography>
              <Typography variant="body1" sx={{ opacity: 0.85, maxWidth: 320 }}>
                {t('signup.heroBody')}
              </Typography>
            </Box>
            <Box sx={{ px: 3, pb: 6 }}>
              <img
                src="/assets/illustrations/illustration_signup.png"
                alt={t('signup.signupIllustrationAlt')}
                style={{ maxWidth: '100%' }}
              />
            </Box>
          </StyledSection>
        )}
        <Container maxWidth="sm">
          <StyledContent>
            <Typography variant="h4" gutterBottom>
              {t('signup.title')}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
              {t('signup.hasAccount')}{' '}
              <Link to="/login" style={{ textDecoration: 'none', fontWeight: 600, cursor: 'pointer' }}>
                {t('signup.loginLink')}
              </Link>
            </Typography>
            <SignupForm />
          </StyledContent>
        </Container>
      </StyledRoot>
    </>
  );
}
