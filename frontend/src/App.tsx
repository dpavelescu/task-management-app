import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { EnhancedAuthProvider } from './contexts/EnhancedAuthProvider';
import { useAuth } from './hooks/useAuth';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import { LoginPage } from './pages/LoginPage';
import Register from './pages/Register';
import { Tasks } from './pages/Tasks';
import Layout from './components/Layout';
import { ErrorBoundary } from './components/ErrorBoundary';
import { ErrorNotificationProvider } from './components/ErrorNotificationProvider';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});

function AuthRedirect({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  
  if (isAuthenticated) {
    return <Navigate to="/tasks" />;
  }
  return <>{children}</>;
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }
  return <>{children}</>;
}

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ErrorBoundary>
        <ErrorNotificationProvider>
          <Router>
            <EnhancedAuthProvider>
                <Routes>
                  <Route 
                    path="/login" 
                    element={
                      <AuthRedirect>
                        <LoginPage />
                      </AuthRedirect>
                    } 
                  />
                  <Route 
                    path="/register" 
                    element={
                      <AuthRedirect>
                        <Register />
                      </AuthRedirect>
                    } 
                  />
                  <Route
                    path="/tasks"
                    element={
                      <ProtectedRoute>
                        <Layout>
                          <Tasks />
                        </Layout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/"
                    element={
                      <ProtectedRoute>
                        <Layout>
                          <Tasks />
                        </Layout>
                      </ProtectedRoute>
                    }
                  />
                </Routes>
              </EnhancedAuthProvider>
            </Router>
        </ErrorNotificationProvider>
      </ErrorBoundary>
    </ThemeProvider>
  );
}

export default App
