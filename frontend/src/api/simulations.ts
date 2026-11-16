import { apiPost } from './client';
import type { SimulationRequest, SimulationResult, BatchRequest, BatchResult } from '../types';

/** POST /api/simulations/play */
export async function runSimulation(req: SimulationRequest): Promise<SimulationResult> {
  return apiPost<SimulationRequest, SimulationResult>('/simulations/play', req);
}

/** POST /api/simulations/batch */
export async function runBatch(req: BatchRequest): Promise<BatchResult> {
  return apiPost<BatchRequest, BatchResult>('/simulations/batch', req);
}
