import { useEffect, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import DashboardLayout from './layouts/dashboard/DashboardLayout';
import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';
import Unauthorized from './pages/auth/Unauthorized';
import Dashboard from './pages/dashboard/Dashboard';
import Transaction from './pages/transaction/Transaction';
import BasicTabs from './pages/transfer/BasicTabs';
import NewWallet from './pages/wallet/NewWallet';
import ReceiveFunds from './pages/wallet/ReceiveFunds';
import Wallet from './pages/wallet/Wallet';
import Approvals from './pages/approvals/Approvals';
import Activity from './pages/activity/Activity';
import Customers from './pages/customers/Customers';
import Settings from './pages/settings/Settings';
import PrivateRoute from './PrivateRoute';
import ProtectedRoute from './ProtectedRoute';
import AuthService from './services/AuthService';
import { ensureActiveOrganization } from './services/ensureOrganization';

export default function App() {
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    AuthService.refresh()
      .then(() => (AuthService.getAccessToken() ? ensureActiveOrganization() : undefined))
      .catch(() => undefined)
      .finally(() => setAuthReady(true));
  }, []);

  if (!authReady) {
    return null;
  }

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />

      <Route path="unauthorized" element={<PrivateRoute />}>
        <Route index element={<Unauthorized />} />
      </Route>

      <Route path="/" element={<PrivateRoute />}>
        <Route path="" element={<DashboardLayout />}>
          <Route path="" element={<Dashboard />}>
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route index element={<Dashboard />} />
            </Route>
          </Route>

          <Route path="wallets" element={<PrivateRoute />}>
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route index element={<Wallet />} />
            </Route>
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route path="new" element={<NewWallet />} />
            </Route>
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route path="receive" element={<ReceiveFunds />} />
            </Route>
            {/* Legacy path: add-funds lives under /transfers (TransactionRequest API). */}
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route path="addFunds" element={<Navigate to="/transfers" replace />} />
            </Route>
          </Route>

          <Route path="transfers" element={<PrivateRoute />}>
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route index element={<BasicTabs />} />
            </Route>
          </Route>

          <Route path="transactions" element={<PrivateRoute />}>
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route index element={<Transaction />} />
            </Route>
          </Route>

          <Route path="approvals" element={<PrivateRoute />}>
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route index element={<Approvals />} />
            </Route>
          </Route>

          <Route path="activity" element={<PrivateRoute />}>
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route index element={<Activity />} />
            </Route>
          </Route>

          <Route path="customers" element={<PrivateRoute />}>
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route index element={<Customers />} />
            </Route>
          </Route>

          <Route path="settings" element={<PrivateRoute />}>
            <Route element={<ProtectedRoute roles={['ROLE_USER', 'ROLE_ADMIN']} />}>
              <Route index element={<Settings />} />
            </Route>
          </Route>

          <Route path="profile" element={<Navigate to="/settings?tab=profile" replace />} />
        </Route>
      </Route>
    </Routes>
  );
}
