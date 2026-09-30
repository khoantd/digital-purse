import { Box, Button, Container, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { Helmet } from 'react-helmet-async';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const StyledContent = styled('div')(({ theme }) => ({
  maxWidth: 480,
  margin: 'auto',
  minHeight: '100vh',
  display: 'flex',
  justifyContent: 'center',
  flexDirection: 'column',
  padding: theme.spacing(12, 0),
}));

export default function Page404() {
  const { t } = useTranslation(['auth', 'common']);

  return (
    <>
      <Helmet>
        <title>{t('auth:unauthorized.helmet')}</title>
      </Helmet>
      <Container>
        <StyledContent sx={{ textAlign: 'center', alignItems: 'center' }}>
          <Typography variant="h3" paragraph>
            {t('auth:unauthorized.title')}
          </Typography>
          <Typography sx={{ color: 'text.secondary' }}>{t('auth:unauthorized.body')}</Typography>
          <Box
            component="img"
            src="/assets/illustrations/illustration_401.jpg"
            sx={{ height: 260, mx: 'auto', my: { xs: 5, sm: 10 } }}
          />
          <Button to="/" size="large" variant="contained" component={RouterLink} sx={{ cursor: 'pointer' }}>
            {t('common:actions.goHome')}
          </Button>
        </StyledContent>
      </Container>
    </>
  );
}
