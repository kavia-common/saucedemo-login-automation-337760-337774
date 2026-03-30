import React from 'react';

// PUBLIC_INTERFACE
export function Sidebar({ activeItem = 'runs', onSelect }) {
  /** Sidebar navigation component. */
  return (
    <aside className="qaSidebar" aria-label="Primary">
      <div className="qaSidebar__brand">
        <div className="qaSidebar__logo" aria-hidden="true">
          QA
        </div>
        <div className="qaSidebar__brandText">
          <div className="qaSidebar__title">SauceDemo</div>
          <div className="qaSidebar__subtitle">Automation</div>
        </div>
      </div>

      <nav className="qaSidebar__nav">
        <button
          type="button"
          className={`qaNavItem ${activeItem === 'runs' ? 'qaNavItem--active' : ''}`}
          onClick={() => onSelect?.('runs')}
        >
          Runs
        </button>
        <button
          type="button"
          className={`qaNavItem ${activeItem === 'scenarios' ? 'qaNavItem--active' : ''}`}
          onClick={() => onSelect?.('scenarios')}
          disabled
          title="Scenarios view will be enabled when backend exposes scenario catalog."
        >
          Scenarios
        </button>
        <button
          type="button"
          className={`qaNavItem ${activeItem === 'settings' ? 'qaNavItem--active' : ''}`}
          onClick={() => onSelect?.('settings')}
          disabled
          title="Settings will be enabled when backend exposes configuration endpoints."
        >
          Settings
        </button>
      </nav>

      <div className="qaSidebar__footer">
        <div className="qaSidebar__hint">
          API: <span className="qaMono">{process.env.REACT_APP_API_BASE || process.env.REACT_APP_BACKEND_URL || 'http://localhost:8000'}</span>
        </div>
      </div>
    </aside>
  );
}
