import type { QueryFailure } from '../domain/failure';
import type { QueryEnvelope } from '../domain/models';

export type QueryRequest = {
  prompt: string;
  conversationId: string | null;
  signal: AbortSignal;
};

export interface QueryClient {
  send(request: QueryRequest): Promise<QueryEnvelope>;
}

export class QueryError extends Error {
  readonly failure: QueryFailure;

  constructor(failure: QueryFailure) {
    super(failure.message);
    this.name = 'QueryError';
    this.failure = failure;
  }
}
