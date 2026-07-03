import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import theme from './theme';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './components/Layout';

// Pages
import Login from './pages/Login';
import Signup from './pages/Signup';
import Dashboard from './pages/Dashboard';
import Queues from './pages/Queues';
import Jobs from './pages/Jobs';
import Workers from './pages/Workers';
import DLQ from './pages/DLQ';
import AuditLogs from './pages/AuditLogs';

// PrivateRoute protection wrapper
const PrivateRoute: React.FC<{ children: React.ReactNode; requiredRole?: string }> = ({ children, requiredRole }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return null; // wait for storage token validation check
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && user.role !== requiredRole) {
    return <Navigate to="/" replace />;
  }

  return <Layout>{children}</Layout>;
};

const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />

      {/* Protected routes */}
      <Route path="/" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
      <Route path="/queues" element={<PrivateRoute><Queues /></PrivateRoute>} />
      <Route path="/jobs" element={<PrivateRoute><Jobs /></PrivateRoute>} />
      <Route path="/workers" element={<PrivateRoute><Workers /></PrivateRoute>} />
      <Route path="/dlq" element={<PrivateRoute><DLQ /></PrivateRoute>} />
      <Route path="/audit-logs" element={<PrivateRoute requiredRole="ADMIN"><AuditLogs /></PrivateRoute>} />

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

const App: React.FC = () => {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <Router>
          <AppRoutes />
        </Router>
      </AuthProvider>
    </ThemeProvider>
  );
};

export default App;
