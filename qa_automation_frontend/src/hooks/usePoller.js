import { useEffect, useRef } from 'react';

/**
 * Reusable flow name: PollingFlow
 * Single entrypoint: usePoller()
 *
 * Contract:
 * - Inputs:
 *   - enabled: boolean
 *   - intervalMs: number (>= 250 recommended)
 *   - callback: async or sync function, invoked on an interval
 *   - maxBackoffMs: number (optional, max backoff interval on consecutive errors, default 30000)
 * - Output: none
 * - Errors: callback errors are caught and logged to console (not thrown) to avoid unmount loops.
 *           Consecutive errors trigger exponential backoff to reduce noise and network load.
 * - Side effects: sets/clears timers
 *
 * Observability:
 * - Logs polling errors to console with backoff status
 * - Backoff resets on first successful poll
 */

// PUBLIC_INTERFACE
export function usePoller({ enabled, intervalMs, callback, maxBackoffMs = 30000 }) {
  /** React hook to invoke `callback` repeatedly while `enabled` is true.
   *  Backs off exponentially on consecutive errors to avoid flooding when backend is down.
   */
  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  useEffect(() => {
    if (!enabled) return undefined;
    if (!intervalMs || intervalMs < 1) return undefined;

    let cancelled = false;
    let consecutiveErrors = 0;

    const getNextDelay = () => {
      if (consecutiveErrors === 0) return intervalMs;
      // Exponential backoff: intervalMs * 2^(errors-1), capped at maxBackoffMs
      const backoff = Math.min(intervalMs * Math.pow(2, consecutiveErrors - 1), maxBackoffMs);
      return backoff;
    };

    const tick = async () => {
      try {
        await callbackRef.current?.();
        // Success: reset backoff
        consecutiveErrors = 0;
      } catch (e) {
        consecutiveErrors += 1;
        // Observability: don't crash UI from polling failures.
        // The calling component should surface errors in its own state when needed.
        const nextDelay = getNextDelay();
        // eslint-disable-next-line no-console
        console.error(
          `Polling callback error (attempt ${consecutiveErrors}, next retry in ${Math.round(nextDelay / 1000)}s):`,
          e
        );
      }
      if (cancelled) return;
      timerId = window.setTimeout(tick, getNextDelay());
    };

    // Start immediately
    let timerId = window.setTimeout(tick, 0);

    return () => {
      cancelled = true;
      if (timerId) window.clearTimeout(timerId);
    };
  }, [enabled, intervalMs, maxBackoffMs]);
}
