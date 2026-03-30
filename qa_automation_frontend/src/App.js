import React, { useMemo, useState } from 'react';
import './App.css';

import { createBackendApiClient } from './api/backendApi';
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
  // - Always poll runs list.
  // - Additionally poll selected run when modal is open.
  usePoller({
    enabled: true,
    intervalMs: 2500,
    callback: refreshRuns,
  });

  usePoller({
    enabled: Boolean(selectedRunId),
    intervalMs: 2000,
    callback: async () => {
      await refreshSelectedRun(selectedRunId);
    },
  });

  const onTriggerRun = async () => {
    setIsTriggering(true);
    try {
      await api.createRun({ scenarioTag: 'all', browser: 'chrome' });
      await refreshRuns();
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
      await refreshRuns();
    } catch (e) {
      setSelectedRunError(e);
    }
  };

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

        {runsError ? (
          <div className="qaAlert qaAlert--danger" role="alert">
            <div className="qaAlert__title">Could not load runs</div>
            <div className="qaAlert__body">{String(runsError.message || runsError)}</div>
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
