import { Box, Card, Container, Grid, Stack, Typography } from '@mui/material';
import { enqueueSnackbar } from 'notistack';
import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import {
  BalanceHero,
  EmptyState,
  OpsStatCard,
  QuickActionButton,
  WalletCard,
} from '../../components/wallet-ui';
import { AppWidgetSummary } from '../../sections/@dashboard/app';
import AuthService from '../../services/AuthService';
import HttpService from '../../services/HttpService';
import OrganizationContext from '../../services/OrganizationContext';
import { ensureActiveOrganization } from '../../services/ensureOrganization';
import { fCurrency } from '../../utils/formatNumber';

const EMPTY_STATS = {
  transferTotal: 0,
  withdrawTotal: 0,
  receiveTotal: 0,
  pendingApprovals: 0,
  transactionCount: 0,
  customerCount: 0,
  todayOutboundTotal: 0,
  todayTopUpTotal: 0,
};

export default function Dashboard() {
  const navigate = useNavigate();
  const [wallets, setWallets] = useState([]);
  const [stats, setStats] = useState(EMPTY_STATS);
  const [txCount, setTxCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const isAdmin = AuthService.isAdmin();
  const user = AuthService.getCurrentUser();
  const firstName = user?.firstName || user?.username || 'there';

  useEffect(() => {
    const userId = AuthService.getCurrentUser()?.id;
    if (!userId && !isAdmin) {
      navigate('/login');
      return undefined;
    }

    const load = async () => {
      setLoading(true);
      try {
        if (!isAdmin) {
          await ensureActiveOrganization();
        }
        const orgId = OrganizationContext.getActiveOrganizationId();
        const walletsUrl = '/wallets?page=0&size=1000';
        const walletsPromise = HttpService.getListWithAuth(walletsUrl);
        const statsPromise =
          orgId != null
            ? HttpService.getWithAuth(`/organizations/${orgId}/stats`).catch(() => EMPTY_STATS)
            : Promise.resolve(EMPTY_STATS);
        const txPromise = isAdmin
          ? HttpService.getListWithAuth('/transactions?page=0&size=1000')
          : Promise.resolve([]);

        const [walletList, orgStats, txList] = await Promise.all([
          walletsPromise,
          statsPromise,
          txPromise,
        ]);
        setWallets(walletList);
        setStats({ ...EMPTY_STATS, ...orgStats });
        setTxCount(isAdmin ? txList.length : Number(orgStats?.transactionCount) || 0);
      } catch (error) {
        if (error?.response?.status === 401) {
          navigate('/login');
        } else if (error.response?.data?.message) {
          enqueueSnackbar(error.response.data.message, { variant: 'error' });
        } else {
          enqueueSnackbar(error.message, { variant: 'error' });
        }
      } finally {
        setLoading(false);
      }
    };

    load();
    const onOrg = () => load();
    window.addEventListener('organization-changed', onOrg);
    return () => window.removeEventListener('organization-changed', onOrg);
  }, [isAdmin, navigate]);

  const totalBalance = useMemo(
    () => wallets.reduce((sum, w) => sum + (Number(w.balance) || 0), 0),
    [wallets]
  );

  const uniqueUsers = useMemo(() => {
    const ids = new Set(wallets.map((w) => w.user?.id).filter(Boolean));
    return ids.size;
  }, [wallets]);

  const organizationWallets = useMemo(
    () => wallets.filter((w) => w.ownerType !== 'CUSTOMER'),
    [wallets]
  );
  const customerWallets = useMemo(
    () => wallets.filter((w) => w.ownerType === 'CUSTOMER'),
    [wallets]
  );

  const previewLimit = isAdmin ? 4 : 3;

  const showOpsStats = OrganizationContext.getActiveOrganizationId() != null;

  const renderWalletCard = (wallet) => (
    <Grid item xs={12} sm={6} md={4} key={wallet.id}>
      <WalletCard
        name={wallet.name}
        balance={wallet.balance}
        iban={wallet.iban}
        currency={wallet.currency}
        ownerName={isAdmin ? wallet.user?.fullName : undefined}
        ownerType={wallet.ownerType}
        customerName={wallet.customerName}
        onClick={() =>
          navigate(
            isAdmin
              ? '/wallets'
              : `/transactions?walletId=${wallet.id}&walletName=${encodeURIComponent(wallet.name || '')}`
          )
        }
      />
    </Grid>
  );

  const fourColGridSx = {
    display: 'grid',
    gap: 2,
    gridTemplateColumns: {
      xs: '1fr',
      sm: 'repeat(2, 1fr)',
      md: 'repeat(4, 1fr)',
    },
  };

  const opsStatsRow = showOpsStats ? (
    <Box sx={fourColGridSx}>
      <OpsStatCard
        title="Transferred"
        value={stats.transferTotal}
        icon="eva:swap-outline"
        color="info"
        subtitle={
          stats.todayOutboundTotal
            ? `Today outbound ${fCurrency(stats.todayOutboundTotal)}`
            : undefined
        }
      />
      <OpsStatCard
        title="Withdrawn"
        value={stats.withdrawTotal}
        icon="eva:arrow-upward-fill"
        color="warning"
      />
      <OpsStatCard
        title="Received"
        value={stats.receiveTotal}
        icon="eva:arrow-downward-fill"
        color="success"
        subtitle={
          stats.todayTopUpTotal
            ? `Today ${fCurrency(stats.todayTopUpTotal)}`
            : undefined
        }
      />
      <OpsStatCard
        title="Pending approvals"
        value={stats.pendingApprovals}
        format="count"
        icon="eva:checkmark-circle-2-outline"
        color="error"
        onClick={() => navigate('/approvals')}
      />
    </Box>
  ) : null;

  const quickActionsRow = (
    <Box sx={fourColGridSx}>
      <QuickActionButton
        icon="eva:plus-fill"
        label="Add funds"
        color="success"
        onClick={() => navigate('/transfers?tab=add')}
      />
      <QuickActionButton
        icon="eva:swap-outline"
        label="Transfer"
        color="primary"
        onClick={() => navigate('/transfers?tab=send')}
      />
      <QuickActionButton
        icon="eva:arrow-downward-fill"
        label="Withdraw"
        color="warning"
        onClick={() => navigate('/transfers?tab=withdraw')}
      />
    </Box>
  );

  return (
    <>
      <Helmet>
        <title> Home | Digital Purse </title>
      </Helmet>
      <Container maxWidth="xl">
        <Stack spacing={0.5} sx={{ mb: 3 }}>
          <Typography variant="h4">Hi {firstName}</Typography>
          <Typography variant="body2" color="text.secondary">
            {isAdmin
              ? 'Admin overview of wallets and activity.'
              : 'Organization balance and quick actions for the active business.'}
          </Typography>
        </Stack>

        {!isAdmin && (
          <Stack spacing={2} sx={{ mb: 4 }}>
            <BalanceHero total={totalBalance} walletCount={wallets.length} />
            {opsStatsRow}
            {quickActionsRow}
          </Stack>
        )}

        {isAdmin && (
          <Stack spacing={2} sx={{ mb: 4 }}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6} md={3}>
                <AppWidgetSummary
                  title="Wallets"
                  total={wallets.length}
                  icon="ant-design:wallet-outlined"
                />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <AppWidgetSummary
                  title="Users with wallets"
                  total={uniqueUsers}
                  color="warning"
                  icon="ant-design:user-outlined"
                />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <AppWidgetSummary
                  title="Total balance"
                  total={totalBalance}
                  color="info"
                  icon="ant-design:money-collect-outlined"
                />
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: 'block', mt: 1, px: 1 }}
                >
                  Exact: {fCurrency(totalBalance)}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <AppWidgetSummary
                  title="Transactions"
                  total={txCount}
                  color="error"
                  icon="ant-design:transaction-outlined"
                />
              </Grid>
            </Grid>
            {opsStatsRow}
          </Stack>
        )}

        {loading ? (
          <Card sx={{ p: 2.5, borderRadius: 2 }}>
            <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
              Loading…
            </Typography>
          </Card>
        ) : wallets.length === 0 ? (
          <Card sx={{ p: 2.5, borderRadius: 2 }}>
            <EmptyState
              icon="ant-design:wallet-outlined"
              title="No wallets yet"
              description="Create a wallet to start adding funds and transferring money."
              actionLabel="New wallet"
              onAction={() => navigate('/wallets/new')}
            />
          </Card>
        ) : (
          <Stack spacing={2}>
            <Box>
              <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
                <Typography variant="h5">Organization Wallets</Typography>
                <Typography
                  variant="subtitle2"
                  color="primary"
                  onClick={() => navigate('/wallets')}
                  sx={{ cursor: 'pointer' }}
                >
                  View all
                </Typography>
              </Stack>
              <Card sx={{ p: 2.5, borderRadius: 2 }}>
                {organizationWallets.length === 0 ? (
                  <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
                    No organization wallets yet.
                  </Typography>
                ) : (
                  <Grid container spacing={2}>
                    {organizationWallets.slice(0, previewLimit).map(renderWalletCard)}
                  </Grid>
                )}
              </Card>
            </Box>

            <Box>
              <Typography variant="h5" sx={{ mb: 2 }}>
                Customers&apos; Wallets
              </Typography>
              <Card sx={{ p: 2.5, borderRadius: 2 }}>
                {customerWallets.length === 0 ? (
                  <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
                    No customer wallets yet.
                  </Typography>
                ) : (
                  <Grid container spacing={2}>
                    {customerWallets.slice(0, previewLimit).map(renderWalletCard)}
                  </Grid>
                )}
              </Card>
            </Box>
          </Stack>
        )}

        {!isAdmin && wallets.length > 0 && (
          <Box sx={{ mt: 2 }}>
            <Typography variant="caption" color="text.secondary">
              Tip: open a wallet to view its transactions, or use Transfers to send, add, or withdraw.
            </Typography>
          </Box>
        )}
      </Container>
    </>
  );
}
