import React, { useState } from 'react';
import { Box, Card, CardContent, Typography, TextField, Button, Alert, Link, Container, MenuItem } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Signup: React.FC = () => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('DEVELOPER');
  const [organization, setOrganization] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  
  const { signup } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      await signup(username, email, password, role, organization);
      setSuccess('Account created successfully! Redirecting to login page...');
      setTimeout(() => {
        navigate('/login');
      }, 2000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Error creating account. Please try again.');
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
                Register Account
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Create your distributed scheduler profile
              </Typography>
            </Box>

            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}

            <form onSubmit={handleSubmit}>
              <TextField
                label="Username"
                fullWidth
                required
                margin="dense"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                sx={{ mb: 1.5 }}
              />
              <TextField
                label="Email Address"
                type="email"
                fullWidth
                required
                margin="dense"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                sx={{ mb: 1.5 }}
              />
              <TextField
                label="Password"
                type="password"
                fullWidth
                required
                margin="dense"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                sx={{ mb: 1.5 }}
              />
              <TextField
                select
                label="Role"
                fullWidth
                required
                margin="dense"
                value={role}
                onChange={(e) => setRole(e.target.value)}
                sx={{ mb: 1.5 }}
              >
                <MenuItem value="ADMIN">ADMIN</MenuItem>
                <MenuItem value="DEVELOPER">DEVELOPER</MenuItem>
                <MenuItem value="VIEWER">VIEWER</MenuItem>
              </TextField>
              <TextField
                label="Organization Name (Optional)"
                fullWidth
                margin="dense"
                value={organization}
                placeholder="e.g. Google Deepmind"
                onChange={(e) => setOrganization(e.target.value)}
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
                {loading ? 'Creating Account...' : 'Register'}
              </Button>
            </form>

            <Box sx={{ textAlignment: 'center', mt: 2, display: 'flex', justifyContent: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                Already have an account?{' '}
                <Link onClick={() => navigate('/login')} sx={{ cursor: 'pointer', fontWeight: 600, color: 'secondary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}>
                  Login here
                </Link>
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
};

export default Signup;
