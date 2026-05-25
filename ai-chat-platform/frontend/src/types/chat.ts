export type ChatMessage = {
  id: string;
  chatId: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  createdAt: string;
};

export type ChatThread = {
  id: string;
  title: string;
  updatedAt: string;
};

export type SendMessagePayload = {
  chatId: string;
  content: string;
};

export type DirectConversation = {
  id: string;
  status: 'PENDING' | 'ACCEPTED';
  otherUserId: string;
  otherUsername: string;
  otherDisplayName: string;
  incomingRequest: boolean;
  updatedAt: string;
};

export type DirectMessage = {
  id: string;
  conversationId: string;
  senderId: string;
  senderUsername: string;
  content: string;
  aiGenerated: boolean;
  createdAt: string;
};

export type AutoReplySettings = {
  enabled: boolean;
  delayMinutes: number;
  instructions: string;
};
