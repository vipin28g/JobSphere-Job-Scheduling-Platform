import React, { useState } from 'react';
import { Box, Card, CardContent, Typography, TextField, Button, Alert, Link, Container } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(username, password);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        display: 'flex',
        minHeight: '100vh',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(circle at 10% 20%, #1e1b29 0%, #0a0b0d 90%)',
      }}
    >
      <Container maxWidth="xs">
        <Card sx={{ p: 2 }}>
          <CardContent>
            <Box sx={{ textAlign: 'center', mb: 3 }}>
              <Typography variant="h4" component="h1" sx={{ fontWeight: 800, background: 'linear-gradient(45deg, #9c27b0 30%, #00e5ff 90%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', mb: 1 }}>
                Antigravity
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Enter your credentials to access the scheduler panel
              </Typography>
            </Box>

            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

            <form onSubmit={handleSubmit}>
              <TextField
                label="Username"
                fullWidth
                required
                margin="normal"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                sx={{ mb: 2 }}
              />
              <TextField
                label="Password"
                type="password"
                fullWidth
                required
                margin="normal"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                sx={{ mb: 3 }}
              />
              <Button
                type="submit"
                fullWidth
                variant="contained"
                size="large"
                disabled={loading}
                sx={{
                  background: 'linear-gradient(45deg, #7b1fa2 30%, #00a0b2 90%)',
                  color: 'white',
                  fontWeight: 700,
                  py: 1.5,
                  mb: 2,
                }}
              >
                {loading ? 'Logging in...' : 'Sign In'}
              </Button>
            </form>

            <Box sx={{ textAlignment: 'center', mt: 2, display: 'flex', justifyContent: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                Don't have an account?{' '}
                <Link onClick={() => navigate('/signup')} sx={{ cursor: 'pointer', fontWeight: 600, color: 'secondary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}>
                  Register here
                </Link>
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
};

export default Login;
