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
 * - Output: none
 * - Errors: callback errors are caught and logged to console (not thrown) to avoid unmount loops
 * - Side effects: sets/clears timers
 */

// PUBLIC_INTERFACE
export function usePoller({ enabled, intervalMs, callback }) {
  /** React hook to invoke `callback` repeatedly while `enabled` is true. */
  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  useEffect(() => {
    if (!enabled) return undefined;
    if (!intervalMs || intervalMs < 1) return undefined;

    let cancelled = false;

    const tick = async () => {
      try {
        await callbackRef.current?.();
      } catch (e) {
        // Observability: don't crash UI from polling failures.
        // The calling component should surface errors in its own state when needed.
        // eslint-disable-next-line no-console
        console.error('Polling callback error:', e);
      }
      if (cancelled) return;
      timerId = window.setTimeout(tick, intervalMs);
    };

    let timerId = window.setTimeout(tick, 0);

    return () => {
      cancelled = true;
      if (timerId) window.clearTimeout(timerId);
    };
  }, [enabled, intervalMs]);
}
