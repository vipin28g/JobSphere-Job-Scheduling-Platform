import React, { useEffect, useState } from 'react';
import { Typography, Box, Card, CardContent, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Chip, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem, Slider } from '@mui/material';
import api from '../services/api';
import AddIcon from '@mui/icons-material/Add';
import PauseIcon from '@mui/icons-material/Pause';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SettingsIcon from '@mui/icons-material/Settings';
import { useAuth } from '../context/AuthContext';

interface Queue {
  id: string;
  name: string;
  description: string;
  projectName: string;
  priority: number;
  concurrencyLimit: number;
  isPaused: boolean;
  rateLimitLimit: number;
  rateLimitWindowSeconds: number;
  retryPolicyName: string;
}

interface Project {
  id: string;
  name: string;
}

interface RetryPolicy {
  id: string;
  name: string;
}

const Queues: React.FC = () => {
  const { user } = useAuth();
  const [queues, setQueues] = useState<Queue[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [policies, setPolicies] = useState<RetryPolicy[]>([]);
  
  // Create Modal
  const [openCreate, setOpenCreate] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [selectedProj, setSelectedProj] = useState('');
  const [priority, setPriority] = useState(1);
  const [concurrency, setConcurrency] = useState(5);
  const [rateLimit, setRateLimit] = useState<number | ''>('');
  const [rateWindow, setRateWindow] = useState<number | ''>('');
  const [selectedPolicy, setSelectedPolicy] = useState('');

  // Settings Edit Modal
  const [openSettings, setOpenSettings] = useState(false);
  const [editQueueId, setEditQueueId] = useState('');
  const [editConcurrency, setEditConcurrency] = useState(5);
  const [editRateLimit, setEditRateLimit] = useState<number | ''>('');
  const [editRateWindow, setEditRateWindow] = useState<number | ''>('');

  const fetchQueues = async () => {
    try {
      const res = await api.get('/api/queues');
      setQueues(res.data);
    } catch (err) {
      console.error('Error fetching queues:', err);
    }
  };

  const fetchCreateOptions = async () => {
    try {
      const projRes = await api.get('/api/projects');
      setProjects(projRes.data);

      const polRes = await api.get('/api/retry-policies');
      setPolicies(polRes.data);
    } catch (err) {
      console.error('Error fetching modal options:', err);
    }
  };

  useEffect(() => {
    fetchQueues();
    if (user?.role !== 'VIEWER') {
      fetchCreateOptions();
    }
  }, [user]);

  const handleCreate = async () => {
    try {
      await api.post('/api/queues', {
        name: newName,
        description: newDesc,
        projectId: selectedProj,
        priority,
        concurrencyLimit: concurrency,
        rateLimitLimit: rateLimit === '' ? null : rateLimit,
        rateLimitWindowSeconds: rateWindow === '' ? null : rateWindow,
        retryPolicyId: selectedPolicy || null,
      });
      fetchQueues();
      setOpenCreate(false);
      // Reset form
      setNewName('');
      setNewDesc('');
      setSelectedProj('');
      setPriority(1);
      setConcurrency(5);
      setRateLimit('');
      setRateWindow('');
      setSelectedPolicy('');
    } catch (err) {
      alert('Error creating queue: ' + err);
    }
  };

  const handlePauseResume = async (queueId: string, isCurrentlyPaused: boolean) => {
    try {
      const endpoint = isCurrentlyPaused ? 'resume' : 'pause';
      await api.put(`/api/queues/${queueId}/${endpoint}`);
      fetchQueues();
    } catch (err) {
      alert('Error changing queue pause state: ' + err);
    }
  };

  const handleOpenSettings = (q: Queue) => {
    setEditQueueId(q.id);
    setEditConcurrency(q.concurrencyLimit);
    setEditRateLimit(q.rateLimitLimit || '');
    setEditRateWindow(q.rateLimitWindowSeconds || '');
    setOpenSettings(true);
  };

  const handleSaveSettings = async () => {
    try {
      await api.put(`/api/queues/${editQueueId}/settings`, null, {
        params: {
          concurrencyLimit: editConcurrency,
          rateLimitLimit: editRateLimit === '' ? null : editRateLimit,
          rateLimitWindowSeconds: editRateWindow === '' ? null : editRateWindow,
        }
      });
      fetchQueues();
      setOpenSettings(false);
    } catch (err) {
      alert('Error updating queue settings: ' + err);
    }
  };

  const isEditable = user?.role === 'ADMIN' || user?.role === 'DEVELOPER';

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h2" sx={{ fontWeight: 800, color: 'text.primary' }}>
          Queue Orchestrator
        </Typography>
        {isEditable && (
          <Button variant="contained" color="primary" startIcon={<AddIcon />} onClick={() => setOpenCreate(true)}>
            Create New Queue
          </Button>
        )}
      </Box>

      <Card>
        <CardContent>
          <TableContainer component={Paper} sx={{ backgroundColor: 'transparent', boxShadow: 'none' }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Project</TableCell>
                  <TableCell align="center">Priority</TableCell>
                  <TableCell align="center">Concurrency Limit</TableCell>
                  <TableCell align="center">Rate Limit</TableCell>
                  <TableCell>Retry Policy</TableCell>
                  <TableCell align="center">Status</TableCell>
                  {isEditable && <TableCell align="center">Actions</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {queues.map((q) => (
                  <TableRow key={q.id} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                    <TableCell sx={{ fontWeight: 700 }}>{q.name}</TableCell>
                    <TableCell>{q.description}</TableCell>
                    <TableCell>{q.projectName}</TableCell>
                    <TableCell align="center">
                      <Chip label={q.priority} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell align="center">
                      <Typography sx={{ fontWeight: 600 }}>{q.concurrencyLimit} threads</Typography>
                    </TableCell>
                    <TableCell align="center">
                      {q.rateLimitLimit ? (
                        <Typography sx={{ fontSize: '0.85rem' }}>
                          {q.rateLimitLimit} reqs / {q.rateLimitWindowSeconds}s
                        </Typography>
                      ) : (
                        <Typography color="text.secondary" sx={{ fontSize: '0.85rem' }}>Unlimited</Typography>
                      )}
                    </TableCell>
                    <TableCell sx={{ fontWeight: 500, color: 'secondary.light' }}>
                      {q.retryPolicyName || 'None (No Retry)'}
                    </TableCell>
                    <TableCell align="center">
                      <Chip
                        label={q.isPaused ? 'PAUSED' : 'ACTIVE'}
                        color={q.isPaused ? 'warning' : 'success'}
                        size="small"
                        sx={{ fontWeight: 700 }}
                      />
                    </TableCell>
                    {isEditable && (
                      <TableCell align="center">
                        <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                          <Button
                            variant="outlined"
                            size="small"
                            color={q.isPaused ? 'success' : 'warning'}
                            startIcon={q.isPaused ? <PlayArrowIcon /> : <PauseIcon />}
                            onClick={() => handlePauseResume(q.id, q.isPaused)}
                          >
                            {q.isPaused ? 'Resume' : 'Pause'}
                          </Button>
                          <Button
                            variant="outlined"
                            size="small"
                            color="secondary"
                            startIcon={<SettingsIcon />}
                            onClick={() => handleOpenSettings(q)}
                          >
                            Settings
                          </Button>
                        </Box>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* CREATE QUEUE DIALOG */}
      <Dialog open={openCreate} onClose={() => setOpenCreate(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 800 }}>Create New Queue</DialogTitle>
        <DialogContent sx={{ pt: '10px !important' }}>
          <TextField
            label="Queue Name"
            fullWidth
            required
            margin="dense"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            label="Description"
            fullWidth
            margin="dense"
            value={newDesc}
            onChange={(e) => setNewDesc(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            select
            label="Project"
            fullWidth
            required
            margin="dense"
            value={selectedProj}
            onChange={(e) => setSelectedProj(e.target.value)}
            sx={{ mb: 2 }}
          >
            {projects.map((p) => (
              <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>
            ))}
          </TextField>
          <TextField
            type="number"
            label="Priority (lower value runs first)"
            fullWidth
            required
            margin="dense"
            value={priority}
            onChange={(e) => setPriority(Number(e.target.value))}
            sx={{ mb: 2 }}
          />
          <TextField
            type="number"
            label="Concurrency limit (max parallel worker threads)"
            fullWidth
            required
            margin="dense"
            value={concurrency}
            onChange={(e) => setConcurrency(Number(e.target.value))}
            sx={{ mb: 2 }}
          />
          <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
            <TextField
              type="number"
              label="Rate Limit Reqs Count (Optional)"
              fullWidth
              value={rateLimit}
              onChange={(e) => setRateLimit(e.target.value === '' ? '' : Number(e.target.value))}
            />
            <TextField
              type="number"
              label="Rate Limit Window Seconds"
              fullWidth
              value={rateWindow}
              onChange={(e) => setRateWindow(e.target.value === '' ? '' : Number(e.target.value))}
            />
          </Box>
          <TextField
            select
            label="Retry Policy (Optional)"
            fullWidth
            margin="dense"
            value={selectedPolicy}
            onChange={(e) => setSelectedPolicy(e.target.value)}
          >
            <MenuItem value="">None (Failed jobs fail immediately)</MenuItem>
            {policies.map((p) => (
              <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button onClick={() => setOpenCreate(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!newName || !selectedProj}>
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* EDIT SETTINGS DIALOG */}
      <Dialog open={openSettings} onClose={() => setOpenSettings(false)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: 800 }}>Queue Parameters</DialogTitle>
        <DialogContent sx={{ pt: '10px !important' }}>
          <Box sx={{ py: 2 }}>
            <Typography gutterBottom sx={{ fontWeight: 600 }}>Concurrency Threads</Typography>
            <Slider
              value={editConcurrency}
              min={1}
              max={20}
              step={1}
              valueLabelDisplay="auto"
              onChange={(_, val) => setEditConcurrency(val as number)}
              sx={{ mb: 3 }}
            />
          </Box>
          <TextField
            type="number"
            label="Rate Limit Capacity"
            fullWidth
            margin="normal"
            value={editRateLimit}
            onChange={(e) => setEditRateLimit(e.target.value === '' ? '' : Number(e.target.value))}
          />
          <TextField
            type="number"
            label="Rate Limit Window Seconds"
            fullWidth
            margin="normal"
            value={editRateWindow}
            onChange={(e) => setEditRateWindow(e.target.value === '' ? '' : Number(e.target.value))}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button onClick={() => setOpenSettings(false)}>Cancel</Button>
          <Button variant="contained" color="secondary" onClick={handleSaveSettings}>
            Save Settings
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Queues;
