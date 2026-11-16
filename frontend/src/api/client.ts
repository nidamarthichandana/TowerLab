/**
 * API base client.
 *
 * In dev, set VITE_MOCK_API=true (or leave the env var unset in dev)
 * and all calls are intercepted by src/api/mocks/index.ts.
 * In production, calls go to /api (proxied by Vite to the backend).
 */

import { mockRouter } from './mocks';

const MOCK_MODE = import.meta.env.VITE_MOCK_API === 'true'
  || import.meta.env.DEV && import.meta.env.VITE_MOCK_API !== 'false';

/** Base GET request. */
export async function apiGet<T>(path: string): Promise<T> {
  if (MOCK_MODE) {
    return mockRouter.get<T>(path);
  }
  const res = await fetch(`/api${path}`);
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError(res.status, body.error ?? res.statusText, body.message);
  }
  return res.json() as Promise<T>;
}

/** Base POST request. */
export async function apiPost<TBody, TResponse>(
  path: string,
  body: TBody,
): Promise<TResponse> {
  if (MOCK_MODE) {
    return mockRouter.post<TBody, TResponse>(path, body);
  }
  const res = await fetch(`/api${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const errBody = await res.json().catch(() => ({}));
    throw new ApiError(res.status, errBody.error ?? res.statusText, errBody.message);
  }
  return res.json() as Promise<TResponse>;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    public readonly detail?: string,
  ) {
    super(detail ?? code);
    this.name = 'ApiError';
  }
}
