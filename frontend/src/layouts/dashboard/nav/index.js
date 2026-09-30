import { Box, Drawer } from '@mui/material';
import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import Logo from '../../../components/logo';
import NavSection from '../../../components/nav-section';
import Scrollbar from '../../../components/scrollbar';
import useResponsive from '../../../hooks/useResponsive';
import AuthService from '../../../services/AuthService';
import OrganizationContext from '../../../services/OrganizationContext';
import { ensureActiveOrganization } from '../../../services/ensureOrganization';
import navConfig from './config';

const NAV_WIDTH = 280;

Nav.propTypes = {
  openNav: PropTypes.bool,
  onCloseNav: PropTypes.func,
};

function filterNav(orgRole) {
  const isPlatformAdmin = AuthService.isAdmin();
  return navConfig.filter((item) => {
    if (!item.roles) return true;
    if (isPlatformAdmin) return true;
    const role = (orgRole || '').toUpperCase();
    return item.roles.includes(role);
  });
}

export default function Nav({ openNav, onCloseNav }) {
  const { pathname } = useLocation();
  const [items, setItems] = useState(() => filterNav(null));

  const isDesktop = useResponsive('up', 'lg');

  useEffect(() => {
    if (openNav) {
      onCloseNav();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathname]);

  useEffect(() => {
    const refresh = () => {
      ensureActiveOrganization()
        .then((list) => {
          const activeId = OrganizationContext.getActiveOrganizationId();
          const active = list.find((org) => org.id === activeId);
          setItems(filterNav(active?.myRole));
        })
        .catch(() => setItems(filterNav(null)));
    };
    refresh();
    window.addEventListener('organization-changed', refresh);
    return () => window.removeEventListener('organization-changed', refresh);
  }, []);

  const renderContent = (
    <Scrollbar
      sx={{
        height: 1,
        '& .simplebar-content': { height: 1, display: 'flex', flexDirection: 'column' },
      }}
    >
      <Box sx={{ px: 2.5, py: 3, display: 'inline-flex' }}>
        <Logo />
      </Box>
      <NavSection data={items} />
      <Box sx={{ flexGrow: 1 }} />
    </Scrollbar>
  );

  return (
    <Box
      component="nav"
      sx={{
        flexShrink: { lg: 0 },
        width: { lg: NAV_WIDTH },
      }}
    >
      {isDesktop ? (
        <Drawer
          open
          variant="permanent"
          PaperProps={{
            sx: {
              width: NAV_WIDTH,
              bgcolor: 'background.default',
              borderRightStyle: 'dashed',
            },
          }}
        >
          {renderContent}
        </Drawer>
      ) : (
        <Drawer
          open={openNav}
          onClose={onCloseNav}
          ModalProps={{
            keepMounted: true,
          }}
          PaperProps={{
            sx: { width: NAV_WIDTH },
          }}
        >
          {renderContent}
        </Drawer>
      )}
    </Box>
  );
}
