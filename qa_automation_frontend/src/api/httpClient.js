/**
 * Minimal HTTP client built on fetch with consistent error handling.
 *
 * Reusable flow name: HttpRequestFlow
 * Single entrypoint: requestJson()
 *
 * Contract:
 * - Inputs:
 *   - baseUrl: string (no trailing slash)
 *   - path: string (leading slash recommended)
 *   - options: { method, headers, body, signal }
 * - Output:
 *   - Resolves to parsed JSON for 2xx responses.
 * - Errors:
 *   - Throws HttpError for non-2xx responses (includes status, requestId, bodyText).
 *   - Throws Error for network/parse errors, with context appended.
 * - Side effects:
 *   - Network call via fetch
 */

function createRequestId() {
  // Not cryptographically secure; intended for correlating logs and errors.
  return `req_${Date.now()}_${Math.random().toString(16).slice(2)}`;
}

export class HttpError extends Error {
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
export async function requestJson(baseUrl, path, options = {}) {
  /**
   * Performs an HTTP request expecting JSON response.
   */
  const requestId = createRequestId();
  const url = `${String(baseUrl || '').replace(/\/+$/, '')}${path.startsWith('/') ? '' : '/'}${path}`;

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
    const wrapped = new Error(`Network/parse error for ${method} ${path} (requestId=${requestId}): ${err.message}`);
    wrapped.cause = err;
    throw wrapped;
  }
}
