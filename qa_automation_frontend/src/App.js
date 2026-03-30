import React, { useMemo, useState } from 'react';
import './App.css';

import { createBackendApiClient } from './api/backendApi';
import { BackendUnavailableError } from './api/httpClient';
import { Sidebar } from './components/Sidebar';
import { SummaryCards } from './components/SummaryCards';
import { RunsTable } from './components/RunsTable';
import { RunDetailsModal } from './components/RunDetailsModal';
import { usePoller } from './hooks/usePoller';

function computeStats(runs) {
  const list = runs || [];
  const stats = { total: list.length, passed: 0, failed: 0, running: 0 };
  for (const r of list) {
    const s = String(r?.status || '').toLowerCase();
    if (s === 'running' || s === 'in_progress') stats.running += 1;
    else if (s === 'passed' || s === 'success') stats.passed += 1;
    else if (s === 'failed' || s === 'error') stats.failed += 1;
  }
  return stats;
}

/**
 * Formats an error for display in the UI, providing actionable context
 * depending on the type of error (backend unavailable vs. other).
 */
function formatRunsError(error) {
  if (!error) return null;

  if (error instanceof BackendUnavailableError) {
    return {
      title: 'Backend API unavailable',
      body: 'The backend server is not reachable. Please ensure the backend_api service is running. Polling will retry automatically with backoff.',
      variant: 'warning',
    };
  }

  return {
    title: 'Could not load runs',
    body: String(error.message || error),
    variant: 'danger',
  };
}

// PUBLIC_INTERFACE
function App() {
  /** Main dashboard application. */
  const api = useMemo(() => createBackendApiClient(), []);
  const [activeNav, setActiveNav] = useState('runs');

  const [runs, setRuns] = useState([]);
  const [runsError, setRunsError] = useState(null);
  const [isTriggering, setIsTriggering] = useState(false);

  const [selectedRunId, setSelectedRunId] = useState(null);
  const [selectedRun, setSelectedRun] = useState(null);
  const [selectedRunLoading, setSelectedRunLoading] = useState(false);
  const [selectedRunError, setSelectedRunError] = useState(null);

  const stats = useMemo(() => computeStats(runs), [runs]);

  const refreshRuns = async () => {
    try {
      setRunsError(null);
      const payload = await api.listRuns({ limit: 25 });

      // Contract: backend may return either {items:[...]} or a plain array; normalize here.
      const items = Array.isArray(payload) ? payload : payload?.items || payload?.runs || [];
      setRuns(items);
    } catch (e) {
      setRunsError(e);
      // Re-throw so the poller can track consecutive errors for backoff
      throw e;
    }
  };

  const refreshSelectedRun = async (runId) => {
    if (!runId) return;
    try {
      setSelectedRunError(null);
      setSelectedRunLoading(true);
      const payload = await api.getRun(runId);
      setSelectedRun(payload);
    } catch (e) {
      setSelectedRunError(e);
    } finally {
      setSelectedRunLoading(false);
    }
  };

  // Near-live updates:
  // - Always poll runs list with backoff on errors.
  // - Additionally poll selected run when modal is open.
  usePoller({
    enabled: true,
    intervalMs: 2500,
    callback: refreshRuns,
    maxBackoffMs: 30000,
  });

  usePoller({
    enabled: Boolean(selectedRunId),
    intervalMs: 2000,
    callback: async () => {
      await refreshSelectedRun(selectedRunId);
    },
    maxBackoffMs: 15000,
  });

  const onTriggerRun = async () => {
    setIsTriggering(true);
    try {
      await api.createRun({ scenarioTag: 'all', browser: 'chrome' });
      await refreshRuns().catch(() => {}); // best-effort refresh after trigger
    } catch (e) {
      setRunsError(e);
    } finally {
      setIsTriggering(false);
    }
  };

  const openDetails = async (run) => {
    const runId = run?.id || run?.run_id;
    if (!runId) return;
    setSelectedRunId(String(runId));
    setSelectedRun(run);
    await refreshSelectedRun(String(runId));
  };

  const closeDetails = () => {
    setSelectedRunId(null);
    setSelectedRun(null);
    setSelectedRunError(null);
    setSelectedRunLoading(false);
  };

  const cancelRun = async (runId) => {
    try {
      await api.cancelRun(runId);
      await refreshSelectedRun(runId);
      await refreshRuns().catch(() => {}); // best-effort
    } catch (e) {
      setSelectedRunError(e);
    }
  };

  const errorInfo = formatRunsError(runsError);

  return (
    <div className="qaApp">
      <Sidebar activeItem={activeNav} onSelect={setActiveNav} />

      <main className="qaMain">
        <header className="qaTopbar">
          <div>
            <h1 className="qaH1">Dashboard</h1>
            <div className="qaSubtle">Trigger and monitor SauceDemo login test executions.</div>
          </div>
          <div className="qaTopbar__right">
            <div className="qaEnvBadge" title="Environment">
              {process.env.REACT_APP_NODE_ENV || process.env.NODE_ENV || 'development'}
            </div>
          </div>
        </header>

        <SummaryCards stats={stats} />

        {errorInfo ? (
          <div className={`qaAlert qaAlert--${errorInfo.variant}`} role="alert">
            <div className="qaAlert__title">{errorInfo.title}</div>
            <div className="qaAlert__body">{errorInfo.body}</div>
          </div>
        ) : null}

        <RunsTable runs={runs} onSelectRun={openDetails} onTriggerRun={onTriggerRun} isTriggering={isTriggering} />

        <RunDetailsModal
          open={Boolean(selectedRunId)}
          run={selectedRun}
          loading={selectedRunLoading}
          error={selectedRunError}
          onClose={closeDetails}
          onCancel={cancelRun}
        />
      </main>
    </div>
  );
}

export default App;
