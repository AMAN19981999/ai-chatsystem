import { apiClient } from './apiClient';
import type {
  ChatMessage,
  AutoReplySettings,
  DirectConversation,
  DirectMessage,
  SendMessagePayload,
} from '../types/chat';

export const chatService = {
  async listMessages(chatId: string): Promise<ChatMessage[]> {
    const { data } = await apiClient.get<ChatMessage[]>(`/messages/${chatId}`);
    return data;
  },

  async requestAiReply(payload: SendMessagePayload): Promise<ChatMessage> {
    const { data } = await apiClient.post<ChatMessage>('/ai/reply', payload);
    return data;
  },

  async listDirectConversations(): Promise<DirectConversation[]> {
    const { data } = await apiClient.get<DirectConversation[]>('/direct/conversations');
    return data;
  },

  async createDirectRequest(username: string): Promise<DirectConversation> {
    const { data } = await apiClient.post<DirectConversation>('/direct/requests', { username });
    return data;
  },

  async acceptDirectRequest(conversationId: string): Promise<DirectConversation> {
    const { data } = await apiClient.post<DirectConversation>(
      `/direct/conversations/${conversationId}/accept`,
    );
    return data;
  },

  async listDirectMessages(conversationId: string): Promise<DirectMessage[]> {
    const { data } = await apiClient.get<DirectMessage[]>(
      `/direct/conversations/${conversationId}/messages`,
    );
    return data;
  },

  async sendDirectMessage(conversationId: string, content: string): Promise<DirectMessage> {
    const { data } = await apiClient.post<DirectMessage>(
      `/direct/conversations/${conversationId}/messages`,
      { content },
    );
    return data;
  },

  async getAutoReplySettings(): Promise<AutoReplySettings> {
    const { data } = await apiClient.get<AutoReplySettings>('/direct/auto-reply/settings');
    return data;
  },

  async updateAutoReplySettings(settings: AutoReplySettings): Promise<AutoReplySettings> {
    const { data } = await apiClient.put<AutoReplySettings>(
      '/direct/auto-reply/settings',
      settings,
    );
    return data;
  },
};
