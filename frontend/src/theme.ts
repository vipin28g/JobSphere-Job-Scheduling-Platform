import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#9c27b0', // Vibrant Purple
      light: '#ba68c8',
      dark: '#7b1fa2',
    },
    secondary: {
      main: '#00e5ff', // Neon Cyan
      light: '#33ebff',
      dark: '#00a0b2',
    },
    background: {
      default: '#0a0b0d', // Sleek Near Black
      paper: '#12141c',   // Slate Blue/Black
    },
    text: {
      primary: '#f5f5f7',
      secondary: '#a1a1aa',
    },
    success: {
      main: '#10b981', // Emerald Green
    },
    error: {
      main: '#ef4444', // Red
    },
    warning: {
      main: '#f59e0b', // Amber
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontWeight: 800,
      letterSpacing: '-0.025em',
    },
    h4: {
      fontWeight: 700,
      letterSpacing: '-0.02em',
    },
    h6: {
      fontWeight: 600,
    },
    button: {
      textTransform: 'none',
      fontWeight: 600,
    },
  },
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: '8px 16px',
          transition: 'all 0.2s ease-in-out',
          '&:hover': {
            transform: 'translateY(-1px)',
            boxShadow: '0 4px 12px rgba(156, 39, 176, 0.3)',
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          background: 'rgba(18, 20, 28, 0.8)',
          backdropFilter: 'blur(12px)',
          border: '1px solid rgba(255, 255, 255, 0.05)',
          borderRadius: 16,
          boxShadow: '0 8px 32px 0 rgba(0, 0, 0, 0.37)',
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        head: {
          fontWeight: 700,
          backgroundColor: '#161922',
        },
      },
    },
  },
});

export default theme;
