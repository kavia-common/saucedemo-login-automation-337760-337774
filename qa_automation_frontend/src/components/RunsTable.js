import React from 'react';

function formatTs(ts) {
  if (!ts) return '—';
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return String(ts);
  return d.toLocaleString();
}

function statusTone(status) {
  const s = String(status || '').toLowerCase();
  if (s === 'passed' || s === 'success') return 'qaPill--success';
  if (s === 'failed' || s === 'error') return 'qaPill--danger';
  if (s === 'running' || s === 'in_progress') return 'qaPill--info';
  if (s === 'queued' || s === 'pending') return 'qaPill--muted';
  return 'qaPill--muted';
}

// PUBLIC_INTERFACE
export function RunsTable({ runs, onSelectRun, onTriggerRun, isTriggering }) {
  /** Table listing recent runs and allowing selection for details. */
  return (
    <div className="qaSection">
      <div className="qaSection__header">
        <div>
          <h2 className="qaH2">Test runs</h2>
          <div className="qaSubtle">Trigger new runs and inspect historical results.</div>
        </div>

        <div className="qaSection__actions">
          <button type="button" className="qaBtn qaBtn--primary" onClick={onTriggerRun} disabled={isTriggering}>
            {isTriggering ? 'Starting…' : 'Run now'}
          </button>
        </div>
      </div>

      <div className="qaTableWrap" role="region" aria-label="Recent runs">
        <table className="qaTable">
          <thead>
            <tr>
              <th>Run ID</th>
              <th>Status</th>
              <th>Started</th>
              <th>Duration</th>
              <th>Scenarios</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {runs?.length ? (
              runs.map((r) => (
                <tr key={r.id || r.run_id || JSON.stringify(r)} className="qaTable__row">
                  <td className="qaMono">{r.id || r.run_id || '—'}</td>
                  <td>
                    <span className={`qaPill ${statusTone(r.status)}`}>{r.status || 'unknown'}</span>
                  </td>
                  <td>{formatTs(r.started_at || r.startedAt || r.created_at || r.createdAt)}</td>
                  <td className="qaMono">{r.duration_ms != null ? `${r.duration_ms}ms` : r.duration || '—'}</td>
                  <td className="qaMono">
                    {r.scenarios_total != null ? r.scenarios_total : r.scenariosTotal != null ? r.scenariosTotal : '—'}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <button
                      type="button"
                      className="qaBtn qaBtn--ghost"
                      onClick={() => onSelectRun?.(r)}
                      aria-label="Open run details"
                    >
                      Details
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={6} className="qaEmptyCell">
                  No runs yet. Click <span className="qaInlineCode">Run now</span> to start a test execution.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
