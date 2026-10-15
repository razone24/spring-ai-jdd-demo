import { createContext } from 'react';
import type { ConversationKey } from '../domain/conversation';
import type { ConversationState } from './conversationReducer';

export type ConversationContextValue = {
  state: ConversationState;
  startConversation: () => void;
  openConversation: (key: ConversationKey) => void;
  sendPrompt: (key: ConversationKey, prompt: string) => void;
  abortTurn: (turnKey: string) => void;
};

export const ConversationContext = createContext<ConversationContextValue | null>(null);
