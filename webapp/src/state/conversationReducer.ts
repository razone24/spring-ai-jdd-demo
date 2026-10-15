import type { Conversation, ConversationKey, Turn } from '../domain/conversation';
import type { QueryFailure } from '../domain/failure';
import type { QueryEnvelope } from '../domain/models';

export type ConversationState = {
  conversations: Record<ConversationKey, Conversation>;
  order: ConversationKey[];
  activeKey: ConversationKey | null;
  transportFailure: string | null;
};

export type ConversationAction =
  | { type: 'conversationCreated'; key: ConversationKey }
  | { type: 'conversationSelected'; key: ConversationKey }
  | { type: 'turnStarted'; key: ConversationKey; turnKey: string; prompt: string; startedAt: number }
  | { type: 'turnAnswered'; key: ConversationKey; turnKey: string; envelope: QueryEnvelope; latencyMs: number }
  | { type: 'turnFailed'; key: ConversationKey; turnKey: string; failure: QueryFailure }
  | { type: 'turnCancelled'; key: ConversationKey; turnKey: string };

export const initialConversationState: ConversationState = {
  conversations: {},
  order: [],
  activeKey: null,
  transportFailure: null,
};

export function seedConversationState(key: ConversationKey): ConversationState {
  return addConversation(initialConversationState, key);
}

export function createConversation(key: ConversationKey): ConversationAction {
  return { type: 'conversationCreated', key };
}

export function selectConversation(key: ConversationKey): ConversationAction {
  return { type: 'conversationSelected', key };
}

export function startTurn(key: ConversationKey, turnKey: string, prompt: string, startedAt: number): ConversationAction {
  return { type: 'turnStarted', key, turnKey, prompt, startedAt };
}

export function answerTurn(
  key: ConversationKey,
  turnKey: string,
  envelope: QueryEnvelope,
  latencyMs: number,
): ConversationAction {
  return { type: 'turnAnswered', key, turnKey, envelope, latencyMs };
}

export function failTurn(key: ConversationKey, turnKey: string, failure: QueryFailure): ConversationAction {
  return { type: 'turnFailed', key, turnKey, failure };
}

export function cancelTurn(key: ConversationKey, turnKey: string): ConversationAction {
  return { type: 'turnCancelled', key, turnKey };
}

export function reduceConversations(state: ConversationState, action: ConversationAction): ConversationState {
  switch (action.type) {
    case 'conversationCreated':
      return addConversation(state, action.key);
    case 'conversationSelected':
      return { ...state, activeKey: action.key };
    case 'turnStarted':
      return withConversation(clearTransportFailure(state), action.key, conversation => ({
        ...conversation,
        turns: [...conversation.turns, buildTurn(action.turnKey, action.prompt, action.startedAt)],
      }));
    case 'turnAnswered':
      return withConversation(clearTransportFailure(state), action.key, conversation => ({
        ...conversation,
        conversationId: action.envelope.conversationId || conversation.conversationId,
        turns: replaceTurn(conversation.turns, action.turnKey, {
          kind: 'answered',
          envelope: action.envelope,
          latencyMs: action.latencyMs,
        }),
      }));
    case 'turnFailed':
      return withConversation(recordTransportFailure(state, action.failure), action.key, conversation => ({
        ...conversation,
        turns: replaceTurn(conversation.turns, action.turnKey, { kind: 'failed', failure: action.failure }),
      }));
    case 'turnCancelled':
      return withConversation(state, action.key, conversation => ({
        ...conversation,
        turns: replaceTurn(conversation.turns, action.turnKey, { kind: 'cancelled' }),
      }));
  }
}

function addConversation(state: ConversationState, key: ConversationKey): ConversationState {
  return {
    ...state,
    conversations: { ...state.conversations, [key]: { key, conversationId: null, turns: [] } },
    order: [key, ...state.order],
    activeKey: key,
  };
}

function withConversation(
  state: ConversationState,
  key: ConversationKey,
  apply: (conversation: Conversation) => Conversation,
): ConversationState {
  const conversation = state.conversations[key];
  if (!conversation) {
    return state;
  }

  return { ...state, conversations: { ...state.conversations, [key]: apply(conversation) } };
}

function buildTurn(turnKey: string, prompt: string, startedAt: number): Turn {
  return { key: turnKey, prompt, state: { kind: 'pending', startedAt } };
}

function replaceTurn(turns: Turn[], turnKey: string, state: Turn['state']): Turn[] {
  return turns.map(turn => (turn.key === turnKey ? { ...turn, state } : turn));
}

function clearTransportFailure(state: ConversationState): ConversationState {
  return state.transportFailure ? { ...state, transportFailure: null } : state;
}

function recordTransportFailure(state: ConversationState, failure: QueryFailure): ConversationState {
  return failure.kind === 'transport' ? { ...state, transportFailure: failure.message } : state;
}
