import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { HttpQueryClient } from './api/HttpQueryClient';
import { App } from './app/App';
import { ConversationProvider } from './state/ConversationProvider';
import './app/global.css';

const client = new HttpQueryClient();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConversationProvider client={client}>
      <App />
    </ConversationProvider>
  </StrictMode>,
);
