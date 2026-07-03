import React, { useEffect, useState } from 'react';
import { Typography, Box, Card, CardContent, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper } from '@mui/material';
import api from '../services/api';
import ReplayIcon from '@mui/icons-material/Replay';
import DeleteIcon from '@mui/icons-material/Delete';
import { useAuth } from '../context/AuthContext';

interface DLQJob {
  id: string;
  jobId: string;
  jobName: string;
  queueName: string;
  failedAt: string;
  errorMessage: string;
  payload: string;
}

const DLQ: React.FC = () => {
  const { user } = useAuth();
  const [dlqJobs, setDlqJobs] = useState<DLQJob[]>([]);

  const fetchDLQ = async () => {
    try {
      const res = await api.get('/api/dlq');
      setDlqJobs(res.data.content);
    } catch (err) {
      console.error('Error fetching DLQ jobs:', err);
    }
  };

  useEffect(() => {
    fetchDLQ();
  }, []);

  const handleRetry = async (jobId: string) => {
    try {
      await api.post(`/api/dlq/${jobId}/retry`);
      fetchDLQ();
    } catch (err) {
      alert('Error retrying job: ' + err);
    }
  };

  const handleDelete = async (jobId: string) => {
    if (!window.confirm('Are you sure you want to permanently delete this job from DLQ?')) return;
    try {
      await api.delete(`/api/dlq/${jobId}`);
      fetchDLQ();
    } catch (err) {
      alert('Error deleting job: ' + err);
    }
  };

  const isEditable = user?.role === 'ADMIN' || user?.role === 'DEVELOPER';

  return (
    <Box>
      <Typography variant="h4" component="h2" sx={{ fontWeight: 800, mb: 3, color: 'text.primary' }}>
        Dead Letter Queue (DLQ)
      </Typography>

      <Card>
        <CardContent>
          <TableContainer component={Paper} sx={{ backgroundColor: 'transparent', boxShadow: 'none' }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Job Name</TableCell>
                  <TableCell>Original Queue</TableCell>
                  <TableCell>Failed At</TableCell>
                  <TableCell>Error details</TableCell>
                  <TableCell>Payload</TableCell>
                  {isEditable && <TableCell align="center">Actions</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {dlqJobs.map((item) => (
                  <TableRow key={item.id} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                    <TableCell sx={{ fontWeight: 700 }}>{item.jobName}</TableCell>
                    <TableCell>{item.queueName}</TableCell>
                    <TableCell sx={{ color: 'text.secondary', fontSize: '0.85rem' }}>
                      {new Date(item.failedAt).toLocaleString()}
                    </TableCell>
                    <TableCell sx={{ color: 'error.main', maxWidth: 250, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {item.errorMessage}
                    </TableCell>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.75rem', maxWidth: 150, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {item.payload}
                    </TableCell>
                    {isEditable && (
                      <TableCell align="center">
                        <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                          <Button
                            variant="outlined"
                            size="small"
                            color="success"
                            startIcon={<ReplayIcon />}
                            onClick={() => handleRetry(item.jobId)}
                          >
                            Requeue
                          </Button>
                          <Button
                            variant="outlined"
                            size="small"
                            color="error"
                            startIcon={<DeleteIcon />}
                            onClick={() => handleDelete(item.jobId)}
                          >
                            Purge
                          </Button>
                        </Box>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
                {dlqJobs.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                      Dead Letter Queue is empty. No failed jobs quarantined!
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

export default DLQ;
