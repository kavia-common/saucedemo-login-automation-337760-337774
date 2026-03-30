/**
 * API configuration for the frontend.
 *
 * Contract:
 * - Inputs: process.env.REACT_APP_API_BASE and/or process.env.REACT_APP_BACKEND_URL
 * - Output: normalized base URL string with no trailing slash
 * - Errors: none thrown; falls back to localhost for dev ergonomics
 *
 * Note: The orchestrator will set env vars in the container's .env file.
 */

function normalizeBaseUrl(url) {
  if (!url) return '';
  return String(url).replace(/\/+$/, '');
}

// PUBLIC_INTERFACE
export function getApiBaseUrl() {
  /**
   * Returns the normalized base URL to use for backend_api HTTP calls.
   *
   * Priority:
   * 1) REACT_APP_API_BASE
   * 2) REACT_APP_BACKEND_URL
   * 3) http://localhost:8000 (dev fallback)
   */
  const fromEnv =
    normalizeBaseUrl(process.env.REACT_APP_API_BASE) ||
    normalizeBaseUrl(process.env.REACT_APP_BACKEND_URL);

  return fromEnv || 'http://localhost:8000';
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
