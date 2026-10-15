import type { QueryFailure } from './failure';
import type { QueryEnvelope } from './models';

const TITLE_MAX_LENGTH = 40;
const ELLIPSIS = '…';

export const UNTITLED_CONVERSATION = 'New conversation';

export type ConversationKey = string;

export type TurnState =
  | { kind: 'pending'; startedAt: number }
  | { kind: 'answered'; envelope: QueryEnvelope; latencyMs: number }
  | { kind: 'failed'; failure: QueryFailure }
  | { kind: 'cancelled' };

export type Turn = {
  key: string;
  prompt: string;
  state: TurnState;
};

export type Conversation = {
  key: ConversationKey;
  conversationId: string | null;
  turns: Turn[];
};

export function buildTitle(conversation: Conversation): string {
  const first = conversation.turns[0];
  if (!first) {
    return UNTITLED_CONVERSATION;
  }
  const prompt = first.prompt.trim();

  return prompt.length > TITLE_MAX_LENGTH ? `${prompt.slice(0, TITLE_MAX_LENGTH)}${ELLIPSIS}` : prompt;
}

export function isPending(conversation: Conversation): boolean {
  return conversation.turns.some(turn => turn.state.kind === 'pending');
}
