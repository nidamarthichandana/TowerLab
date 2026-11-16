/**
 * Mock API router for dev mode.
 * Intercepts calls from client.ts and returns fixture JSON.
 *
 * REPLACE sample-simulation-result.json with the real fixture
 * when the engine owner provides it.
 */
import level1Json from './level1.json';
import catalogJson from './catalog.json';
import simResultJson from './sample-simulation-result.json';
import batchResultJson from './sample-batch-result.json';
import type { SimulationRequest, BatchRequest } from '../../types';

const delay = (ms = 120) => new Promise(r => setTimeout(r, ms));

export const mockRouter = {
  async get<T>(path: string): Promise<T> {
    await delay();
    if (path === '/catalog') return catalogJson as unknown as T;
    if (path.startsWith('/levels/')) return level1Json as unknown as T;
    throw new Error(`[mockRouter] No GET handler for ${path}`);
  },

  async post<TBody, TResponse>(path: string, _body: TBody): Promise<TResponse> {
    await delay(400); // simulate latency
    if (path === '/simulations/play') {
      return simResultJson as unknown as TResponse;
    }
    if (path === '/simulations/batch') {
      return batchResultJson as unknown as TResponse;
    }
    throw new Error(`[mockRouter] No POST handler for ${path}`);
  },
};
