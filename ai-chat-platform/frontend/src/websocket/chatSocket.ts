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
) => {
  const client = new Client({
    webSocketFactory: () => new SockJS(env.wsUrl),
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/direct/${conversationId}`, (frame) => {
        onMessage(JSON.parse(frame.body) as DirectMessage);
      });
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
  };
};
