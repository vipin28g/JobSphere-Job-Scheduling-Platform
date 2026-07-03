import React from 'react';
import { Box, Toolbar } from '@mui/material';
import Sidebar, { drawerWidth } from './Sidebar';
import Navbar from './Navbar';

const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', backgroundColor: 'background.default' }}>
      <Navbar />
      <Sidebar />
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: `calc(100% - ${drawerWidth}px)`,
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Toolbar /> {/* Spacer for the fixed AppBar */}
        <Box sx={{ flexGrow: 1, mt: 2 }}>
          {children}
        </Box>
      </Box>
    </Box>
  );
};

export default Layout;
