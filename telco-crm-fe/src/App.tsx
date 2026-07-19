import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AppLayout } from "./components/layout/AppLayout";
import { AuthProvider } from "./context/AuthContext";
import { ToastProvider } from "./context/ToastContext";
import { ProtectedRoute } from "./components/auth/ProtectedRoute";
import { ROLES } from "./constants/roles";

import Login from "./pages/Login";
import Overview from "./pages/Overview";
import Customers from "./pages/Customers";
import CustomerDetail from "./pages/CustomerDetail";
import CatalogManager from "./pages/CatalogManager";
import OrderList from "./pages/OrderList";
import OrderWizard from "./pages/OrderWizard";
import LiveSaga from "./pages/LiveSaga";
import SubscriptionList from "./pages/SubscriptionList";
import SubscriptionDetail from "./pages/SubscriptionDetail";
import Billing from "./pages/Billing";
import Payments from "./pages/Payments";
import PaymentDetail from "./pages/PaymentDetail";
import Tickets from "./pages/Tickets";
import TicketDetail from "./pages/TicketDetail";
import Admin from "./pages/Admin";
import RolesPermissions from "./pages/RolesPermissions";
import AccessLogs from "./pages/AccessLogs";

const CUSTOMER_MANAGEMENT_ROLES = [ROLES.CALL_CENTER_AGENT, ROLES.FIELD_DEALER];
const ORDER_ROLES = [ROLES.CALL_CENTER_AGENT, ROLES.FIELD_DEALER, ROLES.CUSTOMER];
const ORDER_CREATION_ROLES = [ROLES.CALL_CENTER_AGENT, ROLES.FIELD_DEALER];
const SUBSCRIPTION_ROLES = [ROLES.CALL_CENTER_AGENT, ROLES.FIELD_DEALER, ROLES.CUSTOMER];
const FINANCE_ROLES = [ROLES.BILLING_OPERATOR, ROLES.CUSTOMER];
const CATALOG_ROLES = [ROLES.MARKETING_MANAGER];
const SUPPORT_ROLES = [ROLES.CALL_CENTER_AGENT, ROLES.CUSTOMER];

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />

          <Route path="/" element={<AppLayout />}>
            <Route index element={<Navigate to="/overview" replace />} />
            <Route path="overview" element={<Overview />} />

            <Route path="customers">
              <Route
                index
                element={
                  <ProtectedRoute allowedRoles={CUSTOMER_MANAGEMENT_ROLES}>
                    <Customers />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id"
                element={
                  <ProtectedRoute allowedRoles={CUSTOMER_MANAGEMENT_ROLES}>
                    <CustomerDetail />
                  </ProtectedRoute>
                }
              />
            </Route>

            <Route path="sales">
              <Route
                index
                element={
                  <ProtectedRoute allowedRoles={ORDER_ROLES}>
                    <OrderList />
                  </ProtectedRoute>
                }
              />
              <Route
                path="new"
                element={
                  <ProtectedRoute allowedRoles={ORDER_CREATION_ROLES}>
                    <OrderWizard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="saga/:orderId"
                element={
                  <ProtectedRoute allowedRoles={ORDER_CREATION_ROLES}>
                    <LiveSaga />
                  </ProtectedRoute>
                }
              />
            </Route>

            <Route path="subscriptions">
              <Route
                index
                element={
                  <ProtectedRoute allowedRoles={SUBSCRIPTION_ROLES}>
                    <SubscriptionList />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id"
                element={
                  <ProtectedRoute allowedRoles={SUBSCRIPTION_ROLES}>
                    <SubscriptionDetail />
                  </ProtectedRoute>
                }
              />
            </Route>

            <Route
              path="catalog"
              element={
                <ProtectedRoute allowedRoles={CATALOG_ROLES}>
                  <CatalogManager />
                </ProtectedRoute>
              }
            />

            <Route path="finance">
              <Route index element={<Navigate to="billing" replace />} />
              <Route
                path="billing"
                element={
                  <ProtectedRoute allowedRoles={FINANCE_ROLES}>
                    <Billing />
                  </ProtectedRoute>
                }
              />
              <Route
                path="payments"
                element={
                  <ProtectedRoute allowedRoles={FINANCE_ROLES}>
                    <Payments />
                  </ProtectedRoute>
                }
              />
              <Route
                path="payments/:id"
                element={
                  <ProtectedRoute allowedRoles={FINANCE_ROLES}>
                    <PaymentDetail />
                  </ProtectedRoute>
                }
              />
            </Route>

            <Route path="support">
              <Route
                index
                element={
                  <ProtectedRoute allowedRoles={SUPPORT_ROLES}>
                    <Tickets />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id"
                element={
                  <ProtectedRoute allowedRoles={SUPPORT_ROLES}>
                    <TicketDetail />
                  </ProtectedRoute>
                }
              />
            </Route>

            <Route path="admin">
              <Route
                index
                element={
                  <ProtectedRoute allowedRoles={[ROLES.SYSTEM_ADMIN]}>
                    <Admin />
                  </ProtectedRoute>
                }
              />
              <Route
                path="roles"
                element={
                  <ProtectedRoute allowedRoles={[ROLES.SYSTEM_ADMIN]}>
                    <RolesPermissions />
                  </ProtectedRoute>
                }
              />
              <Route
                path="logs"
                element={
                  <ProtectedRoute allowedRoles={[ROLES.SYSTEM_ADMIN]}>
                    <AccessLogs />
                  </ProtectedRoute>
                }
              />
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
