import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { ChatMessage, DirectMessage, SendMessagePayload } from '../types/chat';
import { env } from '../utils/env';

export const createChatSocket = (chatId: string, onMessage: (message: ChatMessage) => void) => {
  const client = new Client({
    webSocketFactory: () => new SockJS(env.wsUrl),
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/chats/${chatId}`, (frame) => {
        onMessage(JSON.parse(frame.body) as ChatMessage);
      });
    },
  });

  return {
    connect: () => client.activate(),
    disconnect: () => client.deactivate(),
    sendMessage: (payload: SendMessagePayload) => {
      client.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(payload),
      });
    },
  };
};

export const createDirectSocket = (
  conversationId: string,
  token: string,
  onMessage: (message: DirectMessage) => void,
  onTyping?: (typing: { username: string; typing: boolean }) => void,
  onPresence?: (presence: { username: string; online: boolean }) => void,
) => {
  const client = new Client({
    webSocketFactory: () => new SockJS(env.wsUrl),
    reconnectDelay: 5000,
    onConnect: () => {
      // 1. Register presence immediately upon connection
      client.publish({
        destination: '/app/presence.register',
        body: JSON.stringify({ token }),
      });

      // 2. Subscribe to general presence updates
      if (onPresence) {
        client.subscribe('/topic/presence', (frame) => {
          onPresence(JSON.parse(frame.body) as { username: string; online: boolean });
        });
      }

      // 3. Subscribe to active conversation messages
      if (conversationId && conversationId !== 'global') {
        client.subscribe(`/topic/direct/${conversationId}`, (frame) => {
          onMessage(JSON.parse(frame.body) as DirectMessage);
        });
      }

      // 4. Subscribe to active conversation typing indicators
      if (conversationId && conversationId !== 'global' && onTyping) {
        client.subscribe(`/topic/direct/${conversationId}/typing`, (frame) => {
          onTyping(JSON.parse(frame.body) as { username: string; typing: boolean });
        });
      }
    },
  });

  return {
    connect: () => client.activate(),
    disconnect: () => client.deactivate(),
    sendMessage: (content: string) => {
      client.publish({
        destination: '/app/direct.send',
        body: JSON.stringify({ conversationId, content, token }),
      });
    },
    sendTyping: (typing: boolean) => {
      client.publish({
        destination: '/app/direct.typing',
        body: JSON.stringify({ conversationId, typing, token }),
      });
    },
  };
};
