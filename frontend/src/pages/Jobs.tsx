import React, { useEffect, useState } from 'react';
import { Typography, Box, Card, CardContent, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Chip, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem, Drawer, LinearProgress, Pagination } from '@mui/material';
import api from '../services/api';
import AddIcon from '@mui/icons-material/Add';
import VisibilityIcon from '@mui/icons-material/Visibility';
import CancelIcon from '@mui/icons-material/Cancel';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { useAuth } from '../context/AuthContext';

interface Job {
  id: string;
  name: string;
  type: string;
  status: string;
  payload: string;
  queueName: string;
  priority: number;
  runAt: string;
  cronExpression: string;
  parentJobName: string;
  currentRetryCount: number;
  maxRetries: number;
  createdAt: string;
}

interface Queue {
  id: string;
  name: string;
}

interface JobExecution {
  id: string;
  workerId: string;
  status: string;
  startedAt: string;
  endedAt: string;
  errorMessage: string;
  logs: string;
  progress: number;
}

const Jobs: React.FC = () => {
  const { user } = useAuth();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [queues, setQueues] = useState<Queue[]>([]);
  const [parentJobsList, setParentJobsList] = useState<Job[]>([]);
  
  // Pagination & Filtering
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedQueue, setSelectedQueue] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  // Create Job Modal
  const [openCreate, setOpenCreate] = useState(false);
  const [name, setName] = useState('');
  const [type, setType] = useState('IMMEDIATE');
  const [payload, setPayload] = useState('{\n  "duration": 5\n}');
  const [queueId, setQueueId] = useState('');
  const [priority, setPriority] = useState<number | ''>('');
  const [runAt, setRunAt] = useState('');
  const [cron, setCron] = useState('');
  const [parentId, setParentId] = useState('');

  // Drawer for logs/details
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [activeJob, setActiveJob] = useState<Job | null>(null);
  const [executions, setExecutions] = useState<JobExecution[]>([]);
  const [activeProgress, setActiveProgress] = useState<number>(0);

  const fetchJobs = async () => {
    try {
      const res = await api.get('/api/jobs', {
        params: {
          page,
          size: 10,
          queueId: selectedQueue || null,
          status: selectedStatus || null,
          search: searchTerm || null,
          sort: 'createdAt,desc',
        }
      });
      setJobs(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      console.error('Error fetching jobs:', err);
    }
  };

  const fetchQueues = async () => {
    try {
      const res = await api.get('/api/queues');
      setQueues(res.data);
    } catch (err) {
      console.error('Error fetching queues:', err);
    }
  };

  const fetchParentJobsOptions = async () => {
    try {
      const res = await api.get('/api/jobs', { params: { size: 100 } });
      setParentJobsList(res.data.content);
    } catch (err) {
      console.error('Error fetching parent jobs options:', err);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, [page, selectedQueue, selectedStatus, searchTerm]);

  useEffect(() => {
    fetchQueues();
  }, []);

  const handleOpenCreate = () => {
    fetchParentJobsOptions();
    setOpenCreate(true);
  };

  const handleCreate = async () => {
    try {
      // Validate JSON payload
      JSON.parse(payload);

      await api.post('/api/jobs', {
        name,
        type,
        payload,
        queueId,
        priority: priority === '' ? null : priority,
        runAt: runAt ? new Date(runAt).toISOString() : null,
        cronExpression: cron || null,
        parentJobId: parentId || null,
      });

      setOpenCreate(false);
      fetchJobs();
      // Reset form
      setName('');
      setType('IMMEDIATE');
      setPayload('{\n  "duration": 5\n}');
      setQueueId('');
      setPriority('');
      setRunAt('');
      setCron('');
      setParentId('');
    } catch (err) {
      alert('Error creating job. Ensure payload is valid JSON and inputs are correct: ' + err);
    }
  };

  const handleCancel = async (jobId: string) => {
    if (!window.confirm('Are you sure you want to cancel this job?')) return;
    try {
      await api.put(`/api/jobs/${jobId}/cancel`);
      fetchJobs();
    } catch (err) {
      alert('Error cancelling job: ' + err);
    }
  };

  // Websocket for real-time progress inside drawer
  useEffect(() => {
    if (!drawerOpen || !activeJob) return;

    // Fetch executions initially
    const fetchExecutions = async () => {
      try {
        const res = await api.get(`/api/jobs/${activeJob.id}/executions`);
        setExecutions(res.data);
        if (res.data.length > 0) {
          setActiveProgress(res.data[0].progress);
        }
      } catch (err) {
        console.error('Error fetching execution history:', err);
      }
    };

    fetchExecutions();

    // Subscribe to progress
    const socket = new SockJS((import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080') + '/ws');
    const stompClient = Stomp.over(socket);
    stompClient.debug = () => {};

    stompClient.connect({}, () => {
      stompClient.subscribe(`/topic/job-progress/${activeJob.id}`, (msg) => {
        const data = JSON.parse(msg.body);
        setActiveProgress(data.progress);
        fetchExecutions(); // Refresh execution list for new logs!
      });
    }, (error) => {
      console.error('WebSocket connection error:', error);
    });

    return () => {
      if (stompClient && stompClient.connected) {
        stompClient.disconnect(() => {});
      }
    };
  }, [drawerOpen, activeJob]);

  const handleOpenDrawer = (job: Job) => {
    setActiveJob(job);
    setActiveProgress(0);
    setExecutions([]);
    setDrawerOpen(true);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'RUNNING': return 'secondary';
      case 'QUEUED': return 'warning';
      case 'FAILED': return 'error';
      case 'DLQ': return 'error';
      default: return 'default';
    }
  };

  const isEditable = user?.role === 'ADMIN' || user?.role === 'DEVELOPER';

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h2" sx={{ fontWeight: 800, color: 'text.primary' }}>
          Jobs & Workflow Pipeline
        </Typography>
        {isEditable && (
          <Button variant="contained" color="primary" startIcon={<AddIcon />} onClick={handleOpenCreate}>
            Enqueue Job
          </Button>
        )}
      </Box>

      {/* Filter Options */}
      <Card sx={{ mb: 3, p: 2 }}>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr 2fr' }, gap: 2, alignItems: 'center' }}>
          <Box>
            <TextField
              select
              label="Filter by Queue"
              fullWidth
              value={selectedQueue}
              onChange={(e) => { setSelectedQueue(e.target.value); setPage(0); }}
              size="small"
            >
              <MenuItem value="">All Queues</MenuItem>
              {queues.map((q) => (
                <MenuItem key={q.id} value={q.id}>{q.name}</MenuItem>
              ))}
            </TextField>
          </Box>
          <Box>
            <TextField
              select
              label="Filter by Status"
              fullWidth
              value={selectedStatus}
              onChange={(e) => { setSelectedStatus(e.target.value); setPage(0); }}
              size="small"
            >
              <MenuItem value="">All Statuses</MenuItem>
              <MenuItem value="QUEUED">QUEUED</MenuItem>
              <MenuItem value="SCHEDULED">SCHEDULED</MenuItem>
              <MenuItem value="RUNNING">RUNNING</MenuItem>
              <MenuItem value="COMPLETED">COMPLETED</MenuItem>
              <MenuItem value="FAILED">FAILED</MenuItem>
              <MenuItem value="DLQ">DLQ</MenuItem>
            </TextField>
          </Box>
          <Box>
            <TextField
              label="Search Job Name"
              fullWidth
              value={searchTerm}
              onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
              size="small"
            />
          </Box>
        </Box>
      </Card>

      {/* Jobs Grid Table */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TableContainer component={Paper} sx={{ backgroundColor: 'transparent', boxShadow: 'none' }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Job ID</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Queue</TableCell>
                  <TableCell align="center">Priority</TableCell>
                  <TableCell align="center">Retries</TableCell>
                  <TableCell>Parent Dependency</TableCell>
                  <TableCell align="center">Status</TableCell>
                  <TableCell align="center">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {jobs.map((job) => (
                  <TableRow key={job.id} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                      {job.id.substring(0, 8)}...
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>{job.name}</TableCell>
                    <TableCell>{job.type}</TableCell>
                    <TableCell>{job.queueName}</TableCell>
                    <TableCell align="center">{job.priority}</TableCell>
                    <TableCell align="center">{job.currentRetryCount} / {job.maxRetries}</TableCell>
                    <TableCell sx={{ color: 'secondary.light', fontWeight: 500 }}>
                      {job.parentJobName || 'None'}
                    </TableCell>
                    <TableCell align="center">
                      <Chip label={job.status} color={getStatusColor(job.status) as any} size="small" sx={{ fontWeight: 700 }} />
                    </TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                        <Button
                          variant="outlined"
                          size="small"
                          startIcon={<VisibilityIcon />}
                          onClick={() => handleOpenDrawer(job)}
                        >
                          Logs
                        </Button>
                        {isEditable && (job.status === 'QUEUED' || job.status === 'SCHEDULED') && (
                          <Button
                            variant="outlined"
                            size="small"
                            color="error"
                            startIcon={<CancelIcon />}
                            onClick={() => handleCancel(job.id)}
                          >
                            Cancel
                          </Button>
                        )}
                      </Box>
                    </TableCell>
                  </TableRow>
                ))}
                {jobs.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={9} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                      No jobs match the current filters.
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

      {/* CREATE JOB DIALOG */}
      <Dialog open={openCreate} onClose={() => setOpenCreate(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 800 }}>Submit Background Job</DialogTitle>
        <DialogContent sx={{ pt: '10px !important' }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            <TextField
              label="Job Name"
              fullWidth
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
              <TextField
                select
                label="Job Type"
                fullWidth
                value={type}
                onChange={(e) => setType(e.target.value)}
              >
                <MenuItem value="IMMEDIATE">IMMEDIATE</MenuItem>
                <MenuItem value="DELAYED">DELAYED</MenuItem>
                <MenuItem value="SCHEDULED">SCHEDULED</MenuItem>
                <MenuItem value="CRON">CRON (Recurring)</MenuItem>
              </TextField>
              <TextField
                select
                label="Target Queue"
                fullWidth
                required
                value={queueId}
                onChange={(e) => setQueueId(e.target.value)}
              >
                {queues.map((q) => (
                  <MenuItem key={q.id} value={q.id}>{q.name}</MenuItem>
                ))}
              </TextField>
            </Box>

            {type === 'CRON' && (
              <TextField
                label="CRON Expression"
                fullWidth
                required
                placeholder="e.g. */10 * * * * * (every 10 seconds)"
                value={cron}
                onChange={(e) => setCron(e.target.value)}
              />
            )}

            {(type === 'DELAYED' || type === 'SCHEDULED') && (
              <TextField
                label="Execution Time (ISO format)"
                type="datetime-local"
                fullWidth
                required
                slotProps={{ inputLabel: { shrink: true } }}
                value={runAt}
                onChange={(e) => setRunAt(e.target.value)}
              />
            )}

            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
              <TextField
                type="number"
                label="Custom Priority Override (Optional)"
                fullWidth
                value={priority}
                onChange={(e) => setPriority(e.target.value === '' ? '' : Number(e.target.value))}
              />
              <TextField
                select
                label="Parent Dependency (DAG Workflow)"
                fullWidth
                value={parentId}
                onChange={(e) => setParentId(e.target.value)}
              >
                <MenuItem value="">None (Independent job)</MenuItem>
                {parentJobsList.map((j) => (
                  <MenuItem key={j.id} value={j.id}>{j.name} ({j.status})</MenuItem>
                ))}
              </TextField>
            </Box>

            <TextField
              label="Payload Data (JSON)"
              fullWidth
              required
              multiline
              rows={4}
              value={payload}
              onChange={(e) => setPayload(e.target.value)}
            />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button onClick={() => setOpenCreate(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!name || !queueId || !payload}>
            Submit
          </Button>
        </DialogActions>
      </Dialog>

      {/* JOB DETAILS / EXECUTION LOG DRAWER */}
      <Drawer anchor="right" open={drawerOpen} onClose={() => setDrawerOpen(false)}>
        <Box sx={{ width: 450, p: 3, backgroundColor: 'background.default', height: '100%', overflowY: 'auto' }}>
          {activeJob && (
            <>
              <Typography variant="h5" sx={{ fontWeight: 800, mb: 1 }}>Job Log Viewer</Typography>
              <Typography color="text.secondary" variant="body2" sx={{ fontFamily: 'monospace', mb: 3 }}>
                ID: {activeJob.id}
              </Typography>

              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, mb: 3 }}>
                <Box>
                  <Typography variant="caption" color="text.secondary">NAME</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>{activeJob.name}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">STATUS</Typography>
                  <Box sx={{ mt: 0.5 }}>
                    <Chip label={activeJob.status} color={getStatusColor(activeJob.status) as any} size="small" sx={{ fontWeight: 700 }} />
                  </Box>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">TYPE</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>{activeJob.type}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">SUBMITTED AT</Typography>
                  <Typography variant="body2">{new Date(activeJob.createdAt).toLocaleString()}</Typography>
                </Box>
              </Box>

              {activeJob.status === 'RUNNING' && (
                <Box sx={{ mb: 3 }}>
                  <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
                    Active Execution Progress: {activeProgress}%
                  </Typography>
                  <LinearProgress variant="determinate" value={activeProgress} color="secondary" sx={{ height: 8, borderRadius: 4 }} />
                </Box>
              )}

              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>Execution Attempts</Typography>
              {executions.length === 0 ? (
                <Typography color="text.secondary" variant="body2">No executions registered yet. Job is waiting in queue.</Typography>
              ) : (
                executions.map((exec, idx) => (
                  <Card key={exec.id} sx={{ mb: 2, border: '1px solid rgba(255,255,255,0.05)', backgroundColor: '#13151f' }}>
                    <CardContent sx={{ p: '16px !important' }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                        <Typography variant="body2" sx={{ fontWeight: 700, color: 'secondary.light' }}>
                          Attempt #{executions.length - idx}
                        </Typography>
                        <Chip label={exec.status} color={getStatusColor(exec.status) as any} size="small" sx={{ fontWeight: 700, height: 18 }} />
                      </Box>
                      <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                        Worker: {exec.workerId.substring(0,8)}... | Duration:{' '}
                        {exec.endedAt ? `${Math.round((new Date(exec.endedAt).getTime() - new Date(exec.startedAt).getTime()) / 1000)}s` : 'Running...'}
                      </Typography>
                      
                      {exec.errorMessage && (
                        <Typography variant="body2" color="error.main" sx={{ mt: 1, fontWeight: 500, fontSize: '0.85rem' }}>
                          Error: {exec.errorMessage}
                        </Typography>
                      )}

                      <Box sx={{ mt: 2 }}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700 }}>LOGS</Typography>
                        <Box sx={{
                          mt: 0.5,
                          p: 1.5,
                          backgroundColor: '#07080c',
                          borderRadius: 1,
                          fontFamily: 'monospace',
                          fontSize: '0.75rem',
                          whiteSpace: 'pre-wrap',
                          maxHeight: 200,
                          overflowY: 'auto',
                          border: '1px solid rgba(255,255,255,0.03)',
                        }}>
                          {exec.logs}
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>
                ))
              )}
            </>
          )}
        </Box>
      </Drawer>
    </Box>
  );
};

export default Jobs;
