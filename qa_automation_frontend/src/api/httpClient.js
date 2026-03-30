/**
 * Minimal HTTP client built on fetch with consistent error handling.
 *
 * Reusable flow name: HttpRequestFlow
 * Single entrypoint: requestJson()
 *
 * Contract:
 * - Inputs:
 *   - baseUrl: string (no trailing slash; empty string for relative URLs)
 *   - path: string (leading slash recommended)
 *   - options: { method, headers, body, signal }
 * - Output:
 *   - Resolves to parsed JSON for 2xx responses.
 * - Errors:
 *   - Throws HttpError for non-2xx responses (includes status, requestId, url, bodyText).
 *   - Throws BackendUnavailableError for network connectivity failures (nothing listening, DNS, CORS).
 *   - Throws Error for other parse/network errors, with context appended.
 * - Side effects:
 *   - Network call via fetch
 */

function createRequestId() {
  // Not cryptographically secure; intended for correlating logs and errors.
  return `req_${Date.now()}_${Math.random().toString(16).slice(2)}`;
}

// PUBLIC_INTERFACE
export class HttpError extends Error {
  /**
   * Error representing a non-2xx HTTP response from the backend.
   * Includes status code, request ID, URL, and response body text.
   */
  constructor(message, { status, requestId, url, bodyText }) {
    super(message);
    this.name = 'HttpError';
    this.status = status;
    this.requestId = requestId;
    this.url = url;
    this.bodyText = bodyText;
  }
}

// PUBLIC_INTERFACE
export class BackendUnavailableError extends Error {
  /**
   * Error indicating the backend API is not reachable.
   * This typically means nothing is listening on the configured port,
   * there is a CORS issue, or the network path is broken.
   */
  constructor(message, { requestId, url, cause }) {
    super(message);
    this.name = 'BackendUnavailableError';
    this.requestId = requestId;
    this.url = url;
    this.cause = cause;
  }
}

/**
 * Determines whether a fetch error is a network connectivity failure
 * (as opposed to a parse error or other issue).
 */
function isNetworkError(err) {
  if (!err) return false;
  const msg = String(err.message || '').toLowerCase();
  return (
    msg.includes('failed to fetch') ||
    msg.includes('networkerror') ||
    msg.includes('network error') ||
    msg.includes('network request failed') ||
    msg.includes('load failed') ||
    msg.includes('cors') ||
    err.name === 'TypeError' // fetch throws TypeError for network failures
  );
}

// PUBLIC_INTERFACE
export async function requestJson(baseUrl, path, options = {}) {
  /**
   * Performs an HTTP request expecting JSON response.
   *
   * Contract:
   * - baseUrl: string (may be empty for relative/proxied URLs)
   * - path: string (should start with '/')
   * - options.method: HTTP method (default 'GET')
   * - options.headers: additional headers
   * - options.body: request body (auto-serialized to JSON)
   * - options.signal: AbortSignal for cancellation
   *
   * Returns: parsed JSON payload
   * Throws: HttpError | BackendUnavailableError | Error
   */
  const requestId = createRequestId();
  const normalizedBase = String(baseUrl || '').replace(/\/+$/, '');
  const pathPrefix = path.startsWith('/') ? '' : '/';
  const url = `${normalizedBase}${pathPrefix}${path}`;

  const headers = {
    Accept: 'application/json',
    ...(options.headers || {}),
  };

  const method = options.method || 'GET';
  const hasBody = options.body !== undefined && options.body !== null;

  if (hasBody && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const fetchOptions = {
    method,
    headers: {
      ...headers,
      'X-Request-Id': requestId,
    },
    body: hasBody && headers['Content-Type'] === 'application/json' ? JSON.stringify(options.body) : options.body,
    signal: options.signal,
  };

  try {
    const res = await fetch(url, fetchOptions);
    const contentType = res.headers.get('content-type') || '';
    const isJson = contentType.includes('application/json');

    if (!res.ok) {
      const bodyText = await res.text().catch(() => '');
      throw new HttpError(`HTTP ${res.status} for ${method} ${path}`, {
        status: res.status,
        requestId,
        url,
        bodyText,
      });
    }

    if (res.status === 204) return null;

    if (!isJson) {
      const text = await res.text();
      // Preserve debuggability: if backend returns text, return as string.
      return text;
    }

    return await res.json();
  } catch (err) {
    if (err instanceof HttpError) throw err;

    // Detect network-level failures and wrap with a clear, user-friendly error
    if (isNetworkError(err)) {
      const displayUrl = normalizedBase || '(same-origin proxy)';
      throw new BackendUnavailableError(
        `Backend API is not reachable at ${displayUrl}. ` +
        `The backend server may not be running. (${method} ${path}, requestId=${requestId})`,
        { requestId, url, cause: err }
      );
    }

    // Other errors (JSON parse failures, etc.)
    const wrapped = new Error(`Network/parse error for ${method} ${path} (requestId=${requestId}): ${err.message}`);
    wrapped.cause = err;
    throw wrapped;
  }
}
