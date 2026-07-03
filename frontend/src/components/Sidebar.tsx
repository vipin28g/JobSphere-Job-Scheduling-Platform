import React from 'react';
import { Drawer, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Toolbar, Box, Typography } from '@mui/material';
import { useNavigate, useLocation } from 'react-router-dom';
import DashboardIcon from '@mui/icons-material/Dashboard';
import QueueIcon from '@mui/icons-material/Queue';
import WorkIcon from '@mui/icons-material/Work';
import MemoryIcon from '@mui/icons-material/Memory';
import ReportIcon from '@mui/icons-material/Report';
import HistoryIcon from '@mui/icons-material/History';
import { useAuth } from '../context/AuthContext';

const drawerWidth = 240;

const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  const menuItems = [
    { text: 'Dashboard', icon: <DashboardIcon />, path: '/' },
    { text: 'Queues', icon: <QueueIcon />, path: '/queues' },
    { text: 'Jobs & Workflows', icon: <WorkIcon />, path: '/jobs' },
    { text: 'Worker Nodes', icon: <MemoryIcon />, path: '/workers' },
    { text: 'Dead Letter Queue', icon: <ReportIcon />, path: '/dlq' },
  ];

  // Only Admin has access to audit logs
  if (user?.role === 'ADMIN') {
    menuItems.push({ text: 'Audit Logs', icon: <HistoryIcon />, path: '/audit-logs' });
  }

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        [`& .MuiDrawer-paper`]: {
          width: drawerWidth,
          boxSizing: 'border-box',
          backgroundColor: '#0c0d14',
          borderRight: '1px solid rgba(255, 255, 255, 0.05)',
        },
      }}
    >
      <Toolbar sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1 }}>
        <Box
          component="span"
          sx={{
            width: 12,
            height: 12,
            borderRadius: '50%',
            backgroundColor: 'secondary.main',
            boxShadow: '0 0 10px #00e5ff',
          }}
        />
        <Typography variant="h6" noWrap sx={{ fontWeight: 800, background: 'linear-gradient(45deg, #9c27b0 30%, #00e5ff 90%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          JobSphere
        </Typography>
      </Toolbar>
      <Box sx={{ overflow: 'auto', mt: 2 }}>
        <List>
          {menuItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <ListItem key={item.text} disablePadding sx={{ display: 'block', px: 1, py: 0.5 }}>
                <ListItemButton
                  onClick={() => navigate(item.path)}
                  sx={{
                    minHeight: 48,
                    px: 2.5,
                    borderRadius: 2,
                    backgroundColor: isActive ? 'rgba(156, 39, 176, 0.15)' : 'transparent',
                    borderLeft: isActive ? '4px solid #9c27b0' : '4px solid transparent',
                    color: isActive ? 'primary.light' : 'text.secondary',
                    '&:hover': {
                      backgroundColor: 'rgba(255, 255, 255, 0.03)',
                      color: 'text.primary',
                    },
                  }}
                >
                  <ListItemIcon
                    sx={{
                      minWidth: 0,
                      mr: 3,
                      justifyContent: 'center',
                      color: isActive ? 'primary.light' : 'text.secondary',
                    }}
                  >
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText>
                    <Typography sx={{ fontSize: '0.9rem', fontWeight: isActive ? 600 : 500, color: 'inherit' }}>
                      {item.text}
                    </Typography>
                  </ListItemText>
                </ListItemButton>
              </ListItem>
            );
          })}
        </List>
      </Box>
    </Drawer>
  );
};

export default Sidebar;
export { drawerWidth };
