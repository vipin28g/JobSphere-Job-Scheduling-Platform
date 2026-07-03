import React from 'react';
import { AppBar, Toolbar, Typography, Box, Chip, IconButton } from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import { useAuth } from '../context/AuthContext';
import { drawerWidth } from './Sidebar';

const Navbar: React.FC = () => {
  const { user, logout } = useAuth();

  const getRoleColor = (role?: string) => {
    switch (role) {
      case 'ADMIN':
        return 'error';
      case 'DEVELOPER':
        return 'secondary';
      case 'VIEWER':
      default:
        return 'default';
    }
  };

  return (
    <AppBar
      position="fixed"
      sx={{
        width: `calc(100% - ${drawerWidth}px)`,
        ml: `${drawerWidth}px`,
        backgroundColor: 'rgba(10, 11, 13, 0.7)',
        backdropFilter: 'blur(12px)',
        borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
        boxShadow: 'none',
      }}
    >
      <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 700, color: 'text.primary' }}>
          Distributed Scheduler Console
        </Typography>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {user && (
            <>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <AccountCircleIcon sx={{ color: 'text.secondary' }} />
                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>
                  {user.username}
                </Typography>
              </Box>
              <Chip
                label={user.role}
                size="small"
                color={getRoleColor(user.role) as any}
                sx={{ fontWeight: 700, fontSize: '0.75rem' }}
              />
              <IconButton color="inherit" onClick={logout} sx={{ ml: 1, '&:hover': { color: 'primary.main' } }}>
                <LogoutIcon />
              </IconButton>
            </>
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default Navbar;
