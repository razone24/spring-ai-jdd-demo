export type QueryFailure =
  | { kind: 'http'; status: number; message: string }
  | { kind: 'transport'; message: string };

export function describeFailure(failure: QueryFailure): string {
  return failure.kind === 'http' ? `HTTP ${failure.status}` : 'Backend unreachable';
}
