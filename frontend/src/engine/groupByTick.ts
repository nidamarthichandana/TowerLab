import type { GameEvent } from '../types';

/**
 * Groups events by tick in a single O(n) pass.
 * Call this once after the simulation result arrives.
 */
export function groupByTick(events: GameEvent[]): Map<number, GameEvent[]> {
  const map = new Map<number, GameEvent[]>();
  for (const ev of events) {
    let bucket = map.get(ev.tick);
    if (!bucket) {
      bucket = [];
      map.set(ev.tick, bucket);
    }
    bucket.push(ev);
  }
  return map;
}

/** Sorted array of unique ticks present in the event stream. */
export function sortedTicks(events: GameEvent[]): number[] {
  return [...new Set(events.map(e => e.tick))].sort((a, b) => a - b);
}
