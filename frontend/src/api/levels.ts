import { apiGet } from './client';
import type { LevelConfig } from '../types';

export async function getLevel(id: string): Promise<LevelConfig> {
  return apiGet<LevelConfig>(`/levels/${id}`);
}
