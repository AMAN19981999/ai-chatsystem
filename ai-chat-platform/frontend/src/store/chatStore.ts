import { create } from 'zustand';
import type { ChatMessage, ChatThread } from '../types/chat';

type ChatState = {
  activeChatId: string;
  threads: ChatThread[];
  messages: ChatMessage[];
  setMessages: (messages: ChatMessage[]) => void;
  addMessage: (message: ChatMessage) => void;
};

export const useChatStore = create<ChatState>((set) => ({
  activeChatId: 'default-chat',
  threads: [
    {
      id: 'default-chat',
      title: 'Product strategy',
      updatedAt: new Date().toISOString(),
    },
  ],
  messages: [],
  setMessages: (messages) => set({ messages }),
  addMessage: (message) => set((state) => ({ messages: [...state.messages, message] })),
}));
