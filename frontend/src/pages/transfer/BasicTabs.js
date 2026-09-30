import { Card, Container, Stack } from '@mui/material';
import Box from '@mui/material/Box';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import Typography from '@mui/material/Typography';
import PropTypes from 'prop-types';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import Iconify from '../../components/iconify';
import AddFunds from './AddFunds';
import WalletToWallet from './WalletToWallet';
import WithdrawFunds from './WithdrawFunds';

/** Maps URL ?tab= to money endpoints: transfer / addFunds / withdrawFunds */
const TAB_KEYS = ['send', 'add', 'withdraw'];

function tabIndexFromParam(tab) {
  const idx = TAB_KEYS.indexOf(String(tab || '').toLowerCase());
  return idx >= 0 ? idx : 0;
}

function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: { xs: 2.5, md: 4 } }}>{children}</Box>}
    </div>
  );
}

TabPanel.propTypes = {
  children: PropTypes.node,
  index: PropTypes.number.isRequired,
  value: PropTypes.number.isRequired,
};

function a11yProps(index) {
  return {
    id: `simple-tab-${index}`,
    'aria-controls': `simple-tabpanel-${index}`,
  };
}

export default function BasicTabs() {
  const { t } = useTranslation('transfers');
  const [searchParams, setSearchParams] = useSearchParams();
  const value = tabIndexFromParam(searchParams.get('tab'));

  const handleChange = (_event, newValue) => {
    setSearchParams({ tab: TAB_KEYS[newValue] }, { replace: true });
  };

  return (
    <>
      <Helmet>
        <title>{t('helmet')}</title>
      </Helmet>
      <Container sx={{ minWidth: '100%' }}>
        <Stack spacing={0.5} sx={{ mb: 3 }}>
          <Typography variant="h4">{t('title')}</Typography>
          <Typography variant="body2" color="text.secondary">
            {t('subtitle')}
          </Typography>
        </Stack>
        <Card sx={{ borderRadius: 2, maxWidth: 720 }}>
          <Box sx={{ borderBottom: 1, borderColor: 'divider', px: { xs: 1, md: 2 }, pt: 1 }}>
            <Tabs
              value={value}
              onChange={handleChange}
              aria-label={t('tabsAria')}
              variant="scrollable"
              allowScrollButtonsMobile
            >
              <Tab
                icon={<Iconify icon="eva:swap-outline" width={18} height={18} />}
                iconPosition="start"
                label={t('tabs.send')}
                {...a11yProps(0)}
              />
              <Tab
                icon={<Iconify icon="eva:plus-fill" width={18} height={18} />}
                iconPosition="start"
                label={t('tabs.addFunds')}
                {...a11yProps(1)}
              />
              <Tab
                icon={<Iconify icon="eva:arrow-downward-fill" width={18} height={18} />}
                iconPosition="start"
                label={t('tabs.withdraw')}
                {...a11yProps(2)}
              />
            </Tabs>
          </Box>
          <TabPanel value={value} index={0}>
            <WalletToWallet />
          </TabPanel>
          <TabPanel value={value} index={1}>
            <AddFunds />
          </TabPanel>
          <TabPanel value={value} index={2}>
            <WithdrawFunds />
          </TabPanel>
        </Card>
      </Container>
    </>
  );
}
