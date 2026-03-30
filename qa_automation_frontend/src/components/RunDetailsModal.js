import React, { useEffect } from 'react';

function stop(e) {
  e.stopPropagation();
}

function formatJson(value) {
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

// PUBLIC_INTERFACE
export function RunDetailsModal({ open, run, loading, error, onClose, onCancel }) {
  /** Modal to show run details. */
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => {
      if (e.key === 'Escape') onClose?.();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) return null;

  const runId = run?.id || run?.run_id || '—';
  const status = run?.status || 'unknown';

  return (
    <div className="qaModalOverlay" role="dialog" aria-modal="true" aria-label="Run details" onClick={onClose}>
      <div className="qaModal" onClick={stop}>
        <div className="qaModal__header">
          <div>
            <div className="qaModal__title">Run {runId}</div>
            <div className="qaModal__subtitle">
              Status: <span className="qaMono">{status}</span>
            </div>
          </div>
          <button type="button" className="qaBtn qaBtn--ghost" onClick={onClose} aria-label="Close">
            Close
          </button>
        </div>

        <div className="qaModal__body">
          {loading ? (
            <div className="qaLoading">Loading run details…</div>
          ) : error ? (
            <div className="qaAlert qaAlert--danger">
              <div className="qaAlert__title">Could not load run</div>
              <div className="qaAlert__body">{String(error.message || error)}</div>
            </div>
          ) : !run ? (
            <div className="qaLoading">No run selected.</div>
          ) : (
            <>
              <div className="qaModal__actions">
                <button
                  type="button"
                  className="qaBtn qaBtn--danger"
                  onClick={() => onCancel?.(runId)}
                  disabled={String(status).toLowerCase() !== 'running'}
                  title={String(status).toLowerCase() !== 'running' ? 'Only running jobs can be cancelled.' : 'Cancel run'}
                >
                  Cancel run
                </button>
              </div>

              <div className="qaDetailGrid">
                <div className="qaDetailItem">
                  <div className="qaDetailItem__label">Started</div>
                  <div className="qaDetailItem__value qaMono">
                    {run.started_at || run.startedAt || run.created_at || run.createdAt || '—'}
                  </div>
                </div>
                <div className="qaDetailItem">
                  <div className="qaDetailItem__label">Finished</div>
                  <div className="qaDetailItem__value qaMono">{run.finished_at || run.finishedAt || '—'}</div>
                </div>
                <div className="qaDetailItem">
                  <div className="qaDetailItem__label">Browser</div>
                  <div className="qaDetailItem__value qaMono">{run.browser || 'chrome'}</div>
                </div>
                <div className="qaDetailItem">
                  <div className="qaDetailItem__label">Scenario tag</div>
                  <div className="qaDetailItem__value qaMono">{run.scenarioTag || run.scenario_tag || 'all'}</div>
                </div>
              </div>

              <div className="qaSection qaSection--tight">
                <h3 className="qaH3">Raw payload</h3>
                <pre className="qaPre">{formatJson(run)}</pre>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
