import SvgColor from '../../../components/svg-color';

const icon = (name) => <SvgColor src={`/assets/icons/navbar/${name}.svg`} sx={{ width: 1, height: 1 }} />;

const navConfig = [
  {
    title: 'dashboard',
    path: '/',
    icon: icon('ic_analytics'),
  },
  {
    title: 'wallets',
    path: '/wallets',
    icon: icon('ic_wallet'),
  },
  {
    title: 'customers',
    path: '/customers',
    icon: icon('ic_user'),
  },
  {
    title: 'transfers',
    path: '/transfers',
    icon: icon('ic_transfer'),
  },
  {
    title: 'transactions',
    path: '/transactions',
    icon: icon('ic_transaction'),
  },
  {
    title: 'approvals',
    path: '/approvals',
    icon: icon('ic_lock'),
  },
  {
    title: 'activity',
    path: '/activity',
    icon: icon('ic_blog'),
    roles: ['OWNER', 'ADMIN'],
  },
  {
    title: 'settings',
    path: '/settings',
    icon: icon('ic_settings'),
  },
];

export default navConfig;
