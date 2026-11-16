import { apiGet } from './client';
import type { CatalogResult } from '../types';

export async function getCatalog(): Promise<CatalogResult> {
  return apiGet<CatalogResult>('/catalog');
}
