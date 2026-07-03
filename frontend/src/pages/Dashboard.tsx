import React, { useEffect, useState } from 'react';
import { Card, CardContent, Typography, Box, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Chip, Alert, Button } from '@mui/material';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import api from '../services/api';
import WorkIcon from '@mui/icons-material/Work';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import PlayCircleFilledIcon from '@mui/icons-material/PlayCircleFilled';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import MemoryIcon from '@mui/icons-material/Memory';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

interface DashboardStats {
  totalJobs: number;
  queuedJobs: number;
  runningJobs: number;
  completedJobs: number;
  failedJobs: number;
  dlqJobs: number;
  activeWorkers: number;
  avgProcessingTimeSeconds: number;
  jobsByStatus: Record<string, number>;
}

interface RecentJob {
  id: string;
  name: string;
  type: string;
  status: string;
  queueName: string;
  updatedAt: string;
}

const COLORS = ['#9c27b0', '#00e5ff', '#10b981', '#ef4444', '#f59e0b', '#7b1fa2'];

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentJobs, setRecentJobs] = useState<RecentJob[]>([]);
  const [error, setError] = useState<string | null>(null);

  const fetchStats = async () => {
    try {
      setError(null);
      const statsRes = await api.get('/api/dashboard/stats');
      setStats(statsRes.data);

      const jobsRes = await api.get('/api/jobs', {
        params: { page: 0, size: 8, sort: 'updatedAt,desc' }
      });
      setRecentJobs(jobsRes.data.content);
    } catch (err) {
      console.error('Error fetching dashboard statistics:', err);
      setError('Connection to backend failed. Please make sure the backend is running and the tunnel security page is bypassed.');
    }
  };

  useEffect(() => {
    fetchStats();

    // Setup WebSockets
    const socket = new SockJS((import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080') + '/ws');
    const stompClient = Stomp.over(socket);
    stompClient.debug = () => {}; // disable debug logs to keep console clean

    stompClient.connect({}, () => {
      stompClient.subscribe('/topic/metrics', () => {
        fetchStats(); // Refetch stats on WebSocket message event!
      });
    }, (error) => {
      console.error('WebSocket connection error:', error);
    });

    return () => {
      if (stompClient && stompClient.connected) {
        stompClient.disconnect(() => {});
      }
    };
  }, []);

  if (error) {
    return (
      <Box sx={{ p: 3, textAlign: 'center' }}>
        <Alert severity="error" sx={{ mb: 2, justifyContent: 'center' }}>{error}</Alert>
        <Button variant="contained" color="primary" onClick={fetchStats}>Retry Connection</Button>
      </Box>
    );
  }

  if (!stats) {
    return <Typography>Loading dashboard metrics...</Typography>;
  }

  // Formatting chart data
  const pieData = Object.entries(stats.jobsByStatus).map(([name, value]) => ({
    name,
    value,
  }));

  // Mock historical performance data for the AreaChart
  const performanceData = [
    { time: '10:00', completed: stats.completedJobs - 12 > 0 ? stats.completedJobs - 12 : 5, failed: 1 },
    { time: '11:00', completed: stats.completedJobs - 8 > 0 ? stats.completedJobs - 8 : 10, failed: 2 },
    { time: '12:00', completed: stats.completedJobs - 4 > 0 ? stats.completedJobs - 4 : 15, failed: 0 },
    { time: '13:00', completed: stats.completedJobs, failed: stats.failedJobs },
  ];

  const statCards = [
    { title: 'Total Submissions', value: stats.totalJobs, icon: <WorkIcon sx={{ fontSize: 40, color: 'primary.light' }} /> },
    { title: 'Currently Queued', value: stats.queuedJobs, icon: <PendingActionsIcon sx={{ fontSize: 40, color: 'warning.main' }} /> },
    { title: 'Executing Jobs', value: stats.runningJobs, icon: <PlayCircleFilledIcon sx={{ fontSize: 40, color: 'secondary.main' }} /> },
    { title: 'Success Jobs', value: stats.completedJobs, icon: <CheckCircleIcon sx={{ fontSize: 40, color: 'success.main' }} /> },
    { title: 'Failed Runs', value: stats.failedJobs, icon: <CancelIcon sx={{ fontSize: 40, color: 'error.main' }} /> },
    { title: 'Active Workers', value: stats.activeWorkers, icon: <MemoryIcon sx={{ fontSize: 40, color: 'secondary.light' }} /> },
  ];

  const getStatusChipColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'RUNNING': return 'secondary';
      case 'QUEUED': return 'warning';
      case 'FAILED': return 'error';
      case 'DLQ': return 'error';
      default: return 'default';
    }
  };

  return (
    <Box sx={{ flexGrow: 1 }}>
      <Typography variant="h4" component="h2" sx={{ fontWeight: 800, mb: 3, color: 'text.primary' }}>
        System Monitoring Dashboard
      </Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)', lg: 'repeat(6, 1fr)' }, gap: 3, mb: 4 }}>
        {statCards.map((card, idx) => (
          <Box key={idx}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', '&:hover': { transform: 'scale(1.03)', transition: 'all 0.2s' } }}>
              <CardContent sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pb: '16px !important' }}>
                <Box>
                  <Typography color="text.secondary" variant="body2" sx={{ fontWeight: 600 }}>
                    {card.title}
                  </Typography>
                  <Typography variant="h4" sx={{ fontWeight: 800, mt: 1, color: 'text.primary' }}>
                    {card.value}
                  </Typography>
                </Box>
                {card.icon}
              </CardContent>
            </Card>
          </Box>
        ))}
      </Box>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '2fr 1fr' }, gap: 3, mb: 4 }}>
        {/* Execution Area Chart */}
        <Box>
          <Card sx={{ p: 2, height: '100%' }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Job Execution History (hourly)
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={performanceData}>
                  <defs>
                    <linearGradient id="colorCompleted" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                    </linearGradient>
                    <linearGradient id="colorFailed" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#ef4444" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#ef4444" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#2a2d3d" />
                  <XAxis dataKey="time" stroke="#a1a1aa" />
                  <YAxis stroke="#a1a1aa" />
                  <Tooltip contentStyle={{ backgroundColor: '#161922', border: 'none', borderRadius: 8 }} />
                  <Area type="monotone" dataKey="completed" stroke="#10b981" fillOpacity={1} fill="url(#colorCompleted)" />
                  <Area type="monotone" dataKey="failed" stroke="#ef4444" fillOpacity={1} fill="url(#colorFailed)" />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Box>

        {/* Status Pie Chart */}
        <Box>
          <Card sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column', justifyItems: 'center' }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Job Status Breakdown
              </Typography>
              <Box sx={{ display: 'flex', justifyContent: 'center', height: 260 }}>
                {pieData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={pieData}
                        cx="50%"
                        cy="50%"
                        innerRadius={60}
                        outerRadius={90}
                        paddingAngle={5}
                        dataKey="value"
                      >
                        {pieData.map((_entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip contentStyle={{ backgroundColor: '#161922', border: 'none', borderRadius: 8 }} />
                    </PieChart>
                  </ResponsiveContainer>
                ) : (
                  <Typography variant="body2" color="text.secondary" sx={{ alignSelf: 'center' }}>
                    No jobs running or completed yet.
                  </Typography>
                )}
              </Box>
            </CardContent>
          </Card>
        </Box>
      </Box>

      {/* Recent Jobs Table */}
      <Card sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
            Recent Activity Log
          </Typography>
          <TableContainer component={Paper} sx={{ backgroundColor: 'transparent', boxShadow: 'none' }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Job ID</TableCell>
                  <TableCell>Job Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Queue</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Last Updated</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {recentJobs.map((job) => (
                  <TableRow key={job.id} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                    <TableCell sx={{ fontSize: '0.85rem', fontFamily: 'monospace' }}>
                      {job.id.substring(0, 8)}...
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>{job.name}</TableCell>
                    <TableCell>{job.type}</TableCell>
                    <TableCell>{job.queueName}</TableCell>
                    <TableCell>
                      <Chip label={job.status} color={getStatusChipColor(job.status) as any} size="small" sx={{ fontWeight: 700 }} />
                    </TableCell>
                    <TableCell sx={{ fontSize: '0.85rem', color: 'text.secondary' }}>
                      {new Date(job.updatedAt).toLocaleString()}
                    </TableCell>
                  </TableRow>
                ))}
                {recentJobs.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                      No recent jobs found.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
};

export default Dashboard;
