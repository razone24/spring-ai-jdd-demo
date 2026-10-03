import { useContext } from 'react';
import type { Conversation } from '../domain/conversation';
import { ConversationContext } from './ConversationContext';
import type { ConversationContextValue } from './ConversationContext';

const MISSING_PROVIDER_MESSAGE = 'useConversations must be used inside a ConversationProvider.';

export function useConversations(): ConversationContextValue {
  const value = useContext(ConversationContext);
  if (!value) {
    throw new Error(MISSING_PROVIDER_MESSAGE);
  }

  return value;
}

export function useOrderedConversations(): Conversation[] {
  const { state } = useConversations();

  return state.order.flatMap(key => {
    const conversation = state.conversations[key];

    return conversation ? [conversation] : [];
  });
}

export function useActiveConversation(): Conversation | null {
  const { state } = useConversations();

  return state.activeKey ? (state.conversations[state.activeKey] ?? null) : null;
}
