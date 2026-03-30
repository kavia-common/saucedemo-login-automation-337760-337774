import React from 'react';

function formatPct(numerator, denominator) {
  if (!denominator) return '—';
  const pct = Math.round((numerator / denominator) * 100);
  return `${pct}%`;
}

// PUBLIC_INTERFACE
export function SummaryCards({ stats }) {
  /**
   * Summary cards for run status.
   *
   * stats contract:
   * - total: number
   * - passed: number
   * - failed: number
   * - running: number
   */
  const total = stats?.total || 0;
  const passed = stats?.passed || 0;
  const failed = stats?.failed || 0;
  const running = stats?.running || 0;

  return (
    <div className="qaSummaryGrid" role="region" aria-label="Summary">
      <div className="qaCard qaCard--clickless">
        <div className="qaCard__label">Total runs</div>
        <div className="qaCard__value">{total}</div>
        <div className="qaCard__meta">Last 25 shown</div>
      </div>

      <div className="qaCard qaCard--clickless">
        <div className="qaCard__label">Pass rate</div>
        <div className="qaCard__value">{formatPct(passed, Math.max(total - running, 0))}</div>
        <div className="qaCard__meta">
          <span className="qaPill qaPill--success">{passed} passed</span>
          <span className="qaPill qaPill--danger">{failed} failed</span>
        </div>
      </div>

      <div className="qaCard qaCard--clickless">
        <div className="qaCard__label">Running</div>
        <div className="qaCard__value">{running}</div>
        <div className="qaCard__meta">Auto-refresh enabled</div>
      </div>
    </div>
  );
}
