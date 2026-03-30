/**
 * API configuration for the frontend.
 *
 * Flow name: ApiConfigFlow
 * Single entrypoint: getApiBaseUrl()
 *
 * Contract:
 * - Inputs: process.env.REACT_APP_API_BASE and/or process.env.REACT_APP_BACKEND_URL
 * - Output: normalized base URL string with no trailing slash
 * - Errors: none thrown; falls back to empty string (relative URL through CRA proxy)
 *
 * Note: The orchestrator will set env vars in the container's .env file.
 * When running in CRA dev mode, an empty base URL causes requests to go
 * through the CRA dev-server proxy (configured in package.json "proxy" field),
 * which forwards them to http://localhost:8000 (the backend_api).
 *
 * When running in production builds, the full URL from env vars is used directly.
 *
 * Invariant: returned value never has a trailing slash.
 */

function normalizeBaseUrl(url) {
  if (!url) return '';
  return String(url).replace(/\/+$/, '');
}

/**
 * Detects whether the configured backend URL shares the same hostname as the
 * current page but differs only by port. In CRA dev mode, such a configuration
 * should use the CRA proxy (relative URLs) instead of direct cross-origin calls,
 * because the browser cannot typically reach a different port on the proxy host.
 *
 * Example: page served from https://host.example:3000, backend at https://host.example:8000
 * → same hostname, different port → use proxy (return true).
 */
function shouldUseProxy(backendUrl) {
  if (!backendUrl) return false;
  // Only apply proxy logic in development mode
  if (process.env.NODE_ENV !== 'development' && process.env.REACT_APP_NODE_ENV !== 'development') {
    return false;
  }
  try {
    const backend = new URL(backendUrl);
    if (typeof window !== 'undefined' && window.location && window.location.hostname) {
      // Same hostname but different port → the CRA proxy is the correct path
      if (backend.hostname === window.location.hostname && backend.port !== window.location.port) {
        return true;
      }
    }
  } catch {
    // Not a valid URL — let it pass through as-is
  }
  return false;
}

// PUBLIC_INTERFACE
export function getApiBaseUrl() {
  /**
   * Returns the normalized base URL to use for backend_api HTTP calls.
   *
   * Priority:
   * 1) REACT_APP_API_BASE (if set and non-empty)
   * 2) REACT_APP_BACKEND_URL (if set and non-empty)
   * 3) '' (empty string — relative URL through CRA dev-server proxy)
   *
   * Special handling: In development mode, if the configured URL points to the
   * same hostname as the current page (just a different port), we return ''
   * (empty string) to use the CRA dev-server proxy instead of direct cross-origin
   * requests. This prevents "Failed to fetch" errors caused by the browser being
   * unable to reach a different port on the proxy host.
   *
   * In production builds, the configured URL is always used as-is.
   *
   * Invariant: returned value never has a trailing slash.
   */
  const fromEnv =
    normalizeBaseUrl(process.env.REACT_APP_API_BASE) ||
    normalizeBaseUrl(process.env.REACT_APP_BACKEND_URL);

  // In dev mode, prefer relative URLs through CRA proxy when backend is on same host
  if (fromEnv && shouldUseProxy(fromEnv)) {
    // eslint-disable-next-line no-console
    console.info(
      `[ApiConfig] Backend URL "${fromEnv}" is on the same host as the frontend. ` +
      'Using CRA proxy (relative URLs) instead of direct cross-origin requests.'
    );
    return '';
  }

  return fromEnv || '';
}

// PUBLIC_INTERFACE
export function getFrontendUrl() {
  /** Returns the frontend public URL if provided (used for deep-links, etc.). */
  return normalizeBaseUrl(process.env.REACT_APP_FRONTEND_URL) || '';
}

// PUBLIC_INTERFACE
export function getWsUrl() {
  /** Returns the WebSocket base URL if provided (optional). */
  return normalizeBaseUrl(process.env.REACT_APP_WS_URL) || '';
}

// PUBLIC_INTERFACE
export function isBackendCrossOrigin() {
  /**
   * Returns true if the effective backend URL is on a different origin
   * than the current page. Useful for diagnostics and error messages.
   */
  const base = getApiBaseUrl();
  if (!base) return false;
  try {
    const parsed = new URL(base);
    if (typeof window !== 'undefined' && window.location) {
      return parsed.origin !== window.location.origin;
    }
    return true;
  } catch {
    return false;
  }
}
