import React, { useEffect, useState } from 'react';
import { Typography, Box, Card, CardContent, LinearProgress, Chip } from '@mui/material';
import api from '../services/api';
import MemoryIcon from '@mui/icons-material/Memory';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';

interface Worker {
  id: string;
  name: string;
  hostname: string;
  status: string;
  activeThreads: number;
  maxConcurrency: number;
  lastHeartbeat: string;
  registeredAt: string;
  recentMetrics: string; // JSON String
}

const Workers: React.FC = () => {
  const [workers, setWorkers] = useState<Worker[]>([]);

  const fetchWorkers = async () => {
    try {
      const res = await api.get('/api/workers');
      setWorkers(res.data);
    } catch (err) {
      console.error('Error fetching workers:', err);
    }
  };

  useEffect(() => {
    fetchWorkers();
    const interval = setInterval(fetchWorkers, 5000);
    return () => clearInterval(interval);
  }, []);

  const getMetrics = (metricsJson: string) => {
    try {
      return JSON.parse(metricsJson || '{}');
    } catch (e) {
      return {};
    }
  };

  const getHeartbeatStatus = (lastHeartbeat: string, status: string) => {
    if (status !== 'ONLINE') return false;
    const diff = Date.now() - new Date(lastHeartbeat).getTime();
    return diff < 15000; // Active if heartbeat in last 15 seconds
  };

  return (
    <Box>
      <Typography variant="h4" component="h2" sx={{ fontWeight: 800, mb: 3, color: 'text.primary' }}>
        Distributed Worker Cluster
      </Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3, mb: 4 }}>
        {workers.map((worker) => {
          const metrics = getMetrics(worker.recentMetrics);
          const isAlive = getHeartbeatStatus(worker.lastHeartbeat, worker.status);
          const cpu = metrics.cpuUsagePercent || 0;
          const memUsed = metrics.memoryUsedMB || 0;
          const memMax = metrics.memoryMaxMB || 1024;
          const memPercent = Math.round((memUsed / memMax) * 100);

          return (
            <Box key={worker.id}>
              <Card sx={{ border: isAlive ? '1px solid rgba(16, 185, 129, 0.2)' : '1px solid rgba(239, 68, 68, 0.2)' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <MemoryIcon color={isAlive ? 'secondary' : 'action'} />
                      <Typography variant="h6" sx={{ fontWeight: 700 }}>
                        {worker.name}
                      </Typography>
                    </Box>
                    <Chip
                      icon={isAlive ? <CheckCircleIcon /> : <ErrorIcon />}
                      label={isAlive ? 'ONLINE' : 'OFFLINE'}
                      color={isAlive ? 'success' : 'error'}
                      size="small"
                      sx={{ fontWeight: 700 }}
                    />
                  </Box>

                  <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, mb: 3 }}>
                    <Box>
                      <Typography variant="caption" color="text.secondary">HOSTNAME</Typography>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>{worker.hostname}</Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">THREAD CAPACITY</Typography>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {worker.activeThreads} / {worker.maxConcurrency} active
                      </Typography>
                    </Box>
                  </Box>

                  {isAlive && (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                      <Box>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                          <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.8rem', fontWeight: 600 }}>
                            CPU Utilization
                          </Typography>
                          <Typography variant="body2" sx={{ fontSize: '0.8rem', fontWeight: 700 }}>
                            {cpu}%
                          </Typography>
                        </Box>
                        <LinearProgress variant="determinate" value={cpu} color="primary" sx={{ height: 6, borderRadius: 3 }} />
                      </Box>

                      <Box>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                          <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.8rem', fontWeight: 600 }}>
                            Memory Consumption
                          </Typography>
                          <Typography variant="body2" sx={{ fontSize: '0.8rem', fontWeight: 700 }}>
                            {memUsed} MB / {memMax} MB ({memPercent}%)
                          </Typography>
                        </Box>
                        <LinearProgress variant="determinate" value={memPercent} color="secondary" sx={{ height: 6, borderRadius: 3 }} />
                      </Box>
                    </Box>
                  )}

                  <Box sx={{ mt: 3, pt: 2, borderTop: '1px solid rgba(255,255,255,0.05)', display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="caption" color="text.secondary">
                      Registered: {new Date(worker.registeredAt).toLocaleString()}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Last Check: {new Date(worker.lastHeartbeat).toLocaleTimeString()}
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Box>
          );
        })}
        {workers.length === 0 && (
          <Box sx={{ gridColumn: 'span 2' }}>
            <Card sx={{ p: 4, textAlign: 'center' }}>
              <Typography color="text.secondary">No workers currently registered in cluster.</Typography>
            </Card>
          </Box>
        )}
      </Box>
    </Box>
  );
};

export default Workers;
