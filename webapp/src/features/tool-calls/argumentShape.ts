export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function countArguments(value: unknown): number {
  return isRecord(value) ? Object.keys(value).length : 1;
}
