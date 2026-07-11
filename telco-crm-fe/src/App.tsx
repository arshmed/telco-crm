import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AppLayout } from "./components/layout/AppLayout";

import Login from "./pages/Login";
import Overview from "./pages/Overview";
import Customers from "./pages/Customers";
import CustomerDetail from "./pages/CustomerDetail";
import CatalogManager from "./pages/CatalogManager";
import OrderList from "./pages/OrderList";
import OrderWizard from "./pages/OrderWizard";
import LiveSaga from "./pages/LiveSaga";
import SubscriptionDetail from "./pages/SubscriptionDetail";
import Billing from "./pages/Billing";
import Payments from "./pages/Payments";
import PaymentDetail from "./pages/PaymentDetail";
import Tickets from "./pages/Tickets";
import TicketDetail from "./pages/TicketDetail";
import Admin from "./pages/Admin";
import AccessLogs from "./pages/AccessLogs";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        
        <Route path="/" element={<AppLayout />}>
          <Route index element={<Navigate to="/overview" replace />} />
          <Route path="overview" element={<Overview />} />
          
          <Route path="customers">
            <Route index element={<Customers />} />
            <Route path=":id" element={<CustomerDetail />} />
          </Route>
          
          <Route path="sales">
            <Route index element={<OrderList />} />
            <Route path="new" element={<OrderWizard />} />
            <Route path="saga/:orderId" element={<LiveSaga />} />
          </Route>
          
          <Route path="subscriptions/:id" element={<SubscriptionDetail />} />
          
          <Route path="catalog" element={<CatalogManager />} />
          
          <Route path="finance">
            <Route index element={<Navigate to="billing" replace />} />
            <Route path="billing" element={<Billing />} />
            <Route path="payments" element={<Payments />} />
            <Route path="payments/:id" element={<PaymentDetail />} />
          </Route>
          
          <Route path="support">
            <Route index element={<Tickets />} />
            <Route path=":id" element={<TicketDetail />} />
          </Route>
          
          <Route path="admin">
            <Route index element={<Admin />} />
            <Route path="logs" element={<AccessLogs />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
