import { useEffect, useMemo } from 'react';
import { createChatSocket } from '../websocket/chatSocket';
import { useChatStore } from '../store/chatStore';

export const useChatSocket = (chatId: string) => {
  const addMessage = useChatStore((state) => state.addMessage);

  const socket = useMemo(() => createChatSocket(chatId, addMessage), [chatId, addMessage]);

  useEffect(() => {
    socket.connect();

    return () => {
      socket.disconnect();
    };
  }, [socket]);

  return socket;
};
