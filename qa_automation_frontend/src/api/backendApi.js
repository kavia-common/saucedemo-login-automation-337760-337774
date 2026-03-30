import { getApiBaseUrl } from './config';
import { requestJson } from './httpClient';

/**
 * backend_api adapter.
 *
 * Reusable flow name: TestRunApiFlow
 * Single entrypoint: createBackendApiClient()
 *
 * Contract:
 * - Inputs: none (reads env once through getApiBaseUrl()).
 * - Output: client object exposing methods used by UI.
 * - Errors: methods throw HttpError/Error as produced by requestJson().
 * - Side effects: network calls.
 */

// PUBLIC_INTERFACE
export function createBackendApiClient() {
  /** Creates a backend_api client instance. */
  const baseUrl = getApiBaseUrl();

  return {
    // PUBLIC_INTERFACE
    async health() {
      /** Returns backend health payload (shape backend-defined). */
      return requestJson(baseUrl, '/health', { method: 'GET' });
    },

    // PUBLIC_INTERFACE
    async listRuns({ limit = 25 } = {}) {
      /**
       * Fetches recent runs.
       * Expected backend endpoint (to be implemented in backend_api): GET /runs?limit=
       */
      return requestJson(baseUrl, `/runs?limit=${encodeURIComponent(String(limit))}`, { method: 'GET' });
    },

    // PUBLIC_INTERFACE
    async createRun({ scenarioTag = 'all', browser = 'chrome' } = {}) {
      /**
       * Triggers a new run.
       * Expected backend endpoint: POST /runs
       * Body: { scenarioTag, browser }
       */
      return requestJson(baseUrl, '/runs', { method: 'POST', body: { scenarioTag, browser } });
    },

    // PUBLIC_INTERFACE
    async getRun(runId) {
      /**
       * Fetches run details including step/scenario results.
       * Expected backend endpoint: GET /runs/{runId}
       */
      return requestJson(baseUrl, `/runs/${encodeURIComponent(String(runId))}`, { method: 'GET' });
    },

    // PUBLIC_INTERFACE
    async cancelRun(runId) {
      /**
       * Requests cancellation (best-effort).
       * Expected backend endpoint: POST /runs/{runId}/cancel
       */
      return requestJson(baseUrl, `/runs/${encodeURIComponent(String(runId))}/cancel`, { method: 'POST' });
    },
  };
}
