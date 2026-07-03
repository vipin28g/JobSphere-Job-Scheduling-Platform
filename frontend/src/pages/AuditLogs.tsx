import React, { useEffect, useState } from 'react';
import { Typography, Box, Card, CardContent, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, TextField, MenuItem, Pagination, Chip } from '@mui/material';
import api from '../services/api';

interface AuditLog {
  id: string;
  username: string;
  action: string;
  targetType: string;
  targetId: string;
  details: string;
  timestamp: string;
}

const AuditLogs: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [actionFilter, setActionFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchLogs = async () => {
    try {
      const res = await api.get('/api/audit-logs', {
        params: {
          page,
          size: 15,
          action: actionFilter || null,
          sort: 'timestamp,desc',
        }
      });
      setLogs(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      console.error('Error fetching audit logs:', err);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [page, actionFilter]);

  return (
    <Box>
      <Typography variant="h4" component="h2" sx={{ fontWeight: 800, mb: 3, color: 'text.primary' }}>
        Administrative Audit Trail
      </Typography>

      <Card sx={{ mb: 3, p: 2 }}>
        <Box sx={{ maxWidth: 300 }}>
          <TextField
            select
            label="Filter by Action"
            fullWidth
            value={actionFilter}
            onChange={(e) => { setActionFilter(e.target.value); setPage(0); }}
            size="small"
          >
            <MenuItem value="">All Actions</MenuItem>
            <MenuItem value="CREATE_ORG">CREATE_ORG</MenuItem>
            <MenuItem value="CREATE_PROJECT">CREATE_PROJECT</MenuItem>
            <MenuItem value="CREATE_QUEUE">CREATE_QUEUE</MenuItem>
            <MenuItem value="PAUSE_QUEUE">PAUSE_QUEUE</MenuItem>
            <MenuItem value="RESUME_QUEUE">RESUME_QUEUE</MenuItem>
            <MenuItem value="UPDATE_QUEUE_SETTINGS">UPDATE_QUEUE_SETTINGS</MenuItem>
            <MenuItem value="CANCEL_JOB">CANCEL_JOB</MenuItem>
            <MenuItem value="RETRY_DLQ_JOB">RETRY_DLQ_JOB</MenuItem>
            <MenuItem value="DELETE_DLQ_JOB">DELETE_DLQ_JOB</MenuItem>
          </TextField>
        </Box>
      </Card>

      <Card>
        <CardContent>
          <TableContainer component={Paper} sx={{ backgroundColor: 'transparent', boxShadow: 'none' }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Timestamp</TableCell>
                  <TableCell>Operator</TableCell>
                  <TableCell>Action</TableCell>
                  <TableCell>Target Type</TableCell>
                  <TableCell>Target ID</TableCell>
                  <TableCell>Details</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {logs.map((log) => (
                  <TableRow key={log.id} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                    <TableCell sx={{ fontSize: '0.85rem', color: 'text.secondary' }}>
                      {new Date(log.timestamp).toLocaleString()}
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>{log.username}</TableCell>
                    <TableCell>
                      <Chip label={log.action} size="small" variant="outlined" color="primary" sx={{ fontWeight: 600 }} />
                    </TableCell>
                    <TableCell>{log.targetType}</TableCell>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                      {log.targetId ? `${log.targetId.substring(0, 8)}...` : 'N/A'}
                    </TableCell>
                    <TableCell sx={{ fontWeight: 500 }}>{log.details}</TableCell>
                  </TableRow>
                ))}
                {logs.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                      No audit logs found.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>

          {totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
              <Pagination
                count={totalPages}
                page={page + 1}
                onChange={(_, value) => setPage(value - 1)}
                color="primary"
              />
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default AuditLogs;
