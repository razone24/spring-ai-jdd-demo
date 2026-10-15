import { useCallback, useMemo, useReducer, useRef } from 'react';
import type { ReactNode } from 'react';
import { QueryError } from '../api/QueryClient';
import type { QueryClient } from '../api/QueryClient';
import type { ConversationKey } from '../domain/conversation';
import type { QueryFailure } from '../domain/failure';
import { ConversationContext } from './ConversationContext';
import type { ConversationContextValue } from './ConversationContext';
import type { ConversationState } from './conversationReducer';
import {
  answerTurn,
  cancelTurn,
  createConversation,
  failTurn,
  reduceConversations,
  seedConversationState,
  selectConversation,
  startTurn,
} from './conversationReducer';

const UNEXPECTED_FAILURE_MESSAGE = 'The request failed before a response could be read.';

function buildSeededState(): ConversationState {
  return seedConversationState(crypto.randomUUID());
}

function toFailure(error: unknown): QueryFailure {
  return error instanceof QueryError ? error.failure : { kind: 'transport', message: UNEXPECTED_FAILURE_MESSAGE };
}

export function ConversationProvider({ client, children }: { client: QueryClient; children: ReactNode }) {
  const [state, dispatch] = useReducer(reduceConversations, null, buildSeededState);
  const stateRef = useRef(state);
  const controllers = useRef(new Map<string, AbortController>());
  stateRef.current = state;

  const startConversation = useCallback(() => {
    dispatch(createConversation(crypto.randomUUID()));
  }, []);

  const openConversation = useCallback((key: ConversationKey) => {
    dispatch(selectConversation(key));
  }, []);

  const sendPrompt = useCallback(
    (key: ConversationKey, prompt: string) => {
      const turnKey = crypto.randomUUID();
      const controller = new AbortController();
      const conversationId = stateRef.current.conversations[key]?.conversationId ?? null;
      const startedAt = performance.now();

      controllers.current.set(turnKey, controller);
      dispatch(startTurn(key, turnKey, prompt, Date.now()));

      client
        .send({ prompt, conversationId, signal: controller.signal })
        .then(envelope => dispatch(answerTurn(key, turnKey, envelope, performance.now() - startedAt)))
        .catch((error: unknown) => {
          dispatch(controller.signal.aborted ? cancelTurn(key, turnKey) : failTurn(key, turnKey, toFailure(error)));
        })
        .finally(() => controllers.current.delete(turnKey));
    },
    [client],
  );

  const abortTurn = useCallback((turnKey: string) => {
    controllers.current.get(turnKey)?.abort();
  }, []);

  const value = useMemo<ConversationContextValue>(
    () => ({ state, startConversation, openConversation, sendPrompt, abortTurn }),
    [state, startConversation, openConversation, sendPrompt, abortTurn],
  );

  return <ConversationContext value={value}>{children}</ConversationContext>;
}
