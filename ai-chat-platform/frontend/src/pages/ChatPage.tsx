import { FormEvent, useEffect, useState, useRef } from 'react';
import { Check, SendHorizontal, UserPlus } from 'lucide-react';
import { chatService } from '../services/chatService';
import { useAuthStore } from '../store/authStore';
import type { AutoReplySettings, DirectConversation, DirectMessage } from '../types/chat';
import { createDirectSocket } from '../websocket/chatSocket';

export const ChatPage = () => {
  const token = useAuthStore((state) => state.token);
  const [conversations, setConversations] = useState<DirectConversation[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<DirectMessage[]>([]);
  const [socket, setSocket] = useState<ReturnType<typeof createDirectSocket> | null>(null);
  const [username, setUsername] = useState('');
  const [draft, setDraft] = useState('');
  const [notice, setNotice] = useState('');
  
  // Real-time tracking states
  const [onlineUsers, setOnlineUsers] = useState<Set<string>>(new Set());
  const [typingStatus, setTypingStatus] = useState<Record<string, Record<string, boolean>>>({});
  const [isTyping, setIsTyping] = useState(false);
  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const [autoReplySettings, setAutoReplySettings] = useState<AutoReplySettings>({
    enabled: false,
    delayMinutes: 5,
    instructions: '',
  });

  useEffect(() => {
    void chatService.listDirectConversations().then((items) => {
      setConversations(items);
      setActiveConversationId((current) => current ?? items[0]?.id ?? null);

      // Extract initial online users
      const initialOnline = new Set<string>();
      items.forEach((c) => {
        if (c.otherUserOnline) {
          initialOnline.add(c.otherUsername.toLowerCase());
        }
      });
      setOnlineUsers(initialOnline);
    });
    void chatService.getAutoReplySettings().then(setAutoReplySettings);
  }, []);

  useEffect(() => {
    if (!activeConversationId) {
      setMessages([]);
      return;
    }

    const activeConversation = conversations.find((item) => item.id === activeConversationId);
    if (activeConversation?.status !== 'ACCEPTED') {
      setMessages([]);
      return;
    }

    void chatService.listDirectMessages(activeConversationId).then(setMessages);
  }, [activeConversationId, conversations]);

  useEffect(() => {
    if (!token) {
      return;
    }

    // Connect to specific conversation channel if accepted, else a general fallback
    const activeConv = conversations.find((item) => item.id === activeConversationId);
    const targetConversationId = (activeConv?.status === 'ACCEPTED') ? activeConversationId : 'global';

    const nextSocket = createDirectSocket(
      targetConversationId || 'global',
      token,
      (message) => {
        setMessages((items) => {
          if (items.some((item) => item.id === message.id)) {
            return items;
          }
          return [...items, message];
        });
      },
      (typingEvent) => {
        if (targetConversationId && targetConversationId !== 'global') {
          setTypingStatus((prev) => ({
            ...prev,
            [targetConversationId]: {
              ...prev[targetConversationId],
              [typingEvent.username]: typingEvent.typing,
            },
          }));
        }
      },
      (presenceEvent) => {
        setOnlineUsers((prev) => {
          const next = new Set(prev);
          if (presenceEvent.online) {
            next.add(presenceEvent.username.toLowerCase());
          } else {
            next.delete(presenceEvent.username.toLowerCase());
          }
          return next;
        });
      }
    );
    nextSocket.connect();
    setSocket(nextSocket);

    return () => {
      nextSocket.disconnect();
      setSocket(null);
    };
  }, [activeConversationId, conversations, token]);

  const activeConversation = conversations.find((item) => item.id === activeConversationId) ?? null;

  const upsertConversation = (conversation: DirectConversation) => {
    setConversations((items) => {
      const exists = items.some((item) => item.id === conversation.id);
      if (!exists) {
        return [conversation, ...items];
      }

      return items.map((item) => (item.id === conversation.id ? conversation : item));
    });
  };

  const handleRequest = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const requestedUsername = username.trim();
    if (!requestedUsername) {
      return;
    }

    const conversation = await chatService.createDirectRequest(requestedUsername);
    upsertConversation(conversation);
    setActiveConversationId(conversation.id);
    setUsername('');
    setNotice(`Request ready for @${conversation.otherUsername}.`);
  };

  const handleAccept = async (conversationId: string) => {
    const conversation = await chatService.acceptDirectRequest(conversationId);
    upsertConversation(conversation);
    setActiveConversationId(conversation.id);
    setNotice(`You can now message @${conversation.otherUsername}.`);
  };

  const saveAutoReplySettings = async (settings: AutoReplySettings) => {
    setAutoReplySettings(settings);
    const saved = await chatService.updateAutoReplySettings(settings);
    setAutoReplySettings(saved);
    setNotice(saved.enabled ? `AI auto-reply after ${saved.delayMinutes} min.` : 'AI auto-reply off.');
  };

  const handleDraftChange = (text: string) => {
    setDraft(text);
    if (!socket || !activeConversation || activeConversation.status !== 'ACCEPTED') {
      return;
    }

    if (!isTyping) {
      setIsTyping(true);
      socket.sendTyping(true);
    }

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    typingTimeoutRef.current = setTimeout(() => {
      setIsTyping(false);
      socket.sendTyping(false);
    }, 3000);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const content = draft.trim();
    if (!content) {
      return;
    }

    if (!activeConversation || activeConversation.status !== 'ACCEPTED') {
      setNotice('Accept the request before sending messages.');
      return;
    }

    // Reset typing indicator immediately on send
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
    if (isTyping && socket) {
      socket.sendTyping(false);
    }
    setIsTyping(false);

    setDraft('');
    if (socket) {
      socket.sendMessage(content);
      return;
    }

    const message = await chatService.sendDirectMessage(activeConversation.id, content);
    setMessages((items) => [...items, message]);
  };

  return (
    <main className="grid h-[calc(100vh-3.5rem)] grid-cols-[320px_1fr] overflow-hidden">
      <aside className="border-r border-stone-200 bg-white p-4">
        <form onSubmit={handleRequest} className="flex gap-2">
          <input
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            className="h-10 min-w-0 flex-1 rounded-md border border-stone-300 px-3 text-sm outline-none focus:border-accent"
            placeholder="Username"
          />
          <button
            type="submit"
            className="inline-flex h-10 w-10 items-center justify-center rounded-md bg-ink text-white hover:bg-stone-800"
            title="Send request"
          >
            <UserPlus className="h-4 w-4" />
          </button>
        </form>

        {notice ? <p className="mt-3 text-sm text-teal-800">{notice}</p> : null}

        <section className="mt-5 border-t border-stone-200 pt-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-ink">AI auto-reply</h2>
              <p className="text-xs text-stone-500">Replies only if you do not.</p>
            </div>
            <input
              type="checkbox"
              checked={autoReplySettings.enabled}
              onChange={(event) =>
                void saveAutoReplySettings({
                  ...autoReplySettings,
                  enabled: event.target.checked,
                })
              }
              className="h-4 w-4 accent-teal-700"
            />
          </div>

          <label className="mt-3 block text-xs font-medium text-stone-600">
            Minutes
            <input
              type="number"
              min={1}
              max={1440}
              value={autoReplySettings.delayMinutes}
              onChange={(event) =>
                setAutoReplySettings({
                  ...autoReplySettings,
                  delayMinutes: Number(event.target.value),
                })
              }
              onBlur={() => void saveAutoReplySettings(autoReplySettings)}
              className="mt-1 h-9 w-full rounded-md border border-stone-300 px-3 text-sm outline-none focus:border-accent"
            />
          </label>

          <label className="mt-3 block text-xs font-medium text-stone-600">
            Style
            <textarea
              value={autoReplySettings.instructions}
              onChange={(event) =>
                setAutoReplySettings({
                  ...autoReplySettings,
                  instructions: event.target.value,
                })
              }
              onBlur={() => void saveAutoReplySettings(autoReplySettings)}
              className="mt-1 min-h-20 w-full resize-none rounded-md border border-stone-300 px-3 py-2 text-sm outline-none focus:border-accent"
              placeholder="Short, friendly, same language"
            />
          </label>
        </section>

        <nav className="mt-4 space-y-2">
          {conversations.map((conversation) => (
            <button
              key={conversation.id}
              onClick={() => setActiveConversationId(conversation.id)}
              className={`w-full rounded-md px-3 py-2 text-left text-sm font-medium ${
                conversation.id === activeConversationId ? 'bg-stone-100 text-ink' : 'text-stone-700'
              } hover:bg-stone-100`}
            >
              <div className="flex items-center justify-between gap-2">
                <span className="block truncate font-semibold">@{conversation.otherUsername}</span>
                {onlineUsers.has(conversation.otherUsername.toLowerCase()) && (
                  <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse" title="Online"></span>
                )}
              </div>
              <span className="mt-1 block text-xs font-normal text-stone-500">
                {conversation.status === 'ACCEPTED'
                  ? conversation.otherDisplayName
                  : conversation.incomingRequest
                    ? 'Request received'
                    : 'Request sent'}
              </span>
            </button>
          ))}
        </nav>
      </aside>

      <section className="flex min-w-0 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-stone-200 bg-white px-6">
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-semibold text-ink">
                {activeConversation ? `@${activeConversation.otherUsername}` : 'Messages'}
              </h1>
              {activeConversation && onlineUsers.has(activeConversation.otherUsername.toLowerCase()) && (
                <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-medium text-emerald-700 animate-pulse">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                  online
                </span>
              )}
            </div>
            <p className="text-sm text-stone-500">
              {activeConversation
                ? activeConversation.status === 'ACCEPTED'
                  ? activeConversation.otherDisplayName
                  : 'Waiting for request acceptance'
                : 'Send a request with a username'}
            </p>
          </div>

          {activeConversation?.incomingRequest ? (
            <button
              onClick={() => void handleAccept(activeConversation.id)}
              className="inline-flex h-10 items-center gap-2 rounded-md bg-accent px-4 text-sm font-semibold text-white hover:bg-teal-800"
            >
              <Check className="h-4 w-4" />
              Accept
            </button>
          ) : null}
        </header>

        <div className="flex-1 overflow-y-auto p-6">
          <div className="mx-auto flex max-w-3xl flex-col gap-4">
            {!activeConversation ? (
              <p className="text-center text-sm text-stone-500">Choose a conversation or send a request.</p>
            ) : activeConversation.status !== 'ACCEPTED' ? (
              <p className="text-center text-sm text-stone-500">
                Messages unlock when the request is accepted.
              </p>
            ) : (
              <>
                {messages.map((message) => {
                  const isMine = message.senderUsername !== activeConversation.otherUsername;

                  return (
                    <article
                      key={message.id}
                      className={
                        isMine
                          ? 'ml-auto max-w-[80%] rounded-lg bg-accent px-4 py-3 text-white'
                          : 'mr-auto max-w-[80%] rounded-lg border border-stone-200 bg-white px-4 py-3 text-ink'
                      }
                    >
                      <p className="mb-1 text-xs font-semibold opacity-75">
                        @{message.senderUsername}
                        {message.aiGenerated ? ' · AI' : ''}
                      </p>
                      <p className="whitespace-pre-wrap text-sm leading-6">{message.content}</p>
                    </article>
                  );
                })}

                {/* Real-time Typing Indicator */}
                {activeConversationId && typingStatus[activeConversationId]?.[activeConversation.otherUsername] ? (
                  <div className="mr-auto flex items-center gap-2 rounded-lg border border-stone-100 bg-stone-50 px-4 py-2 text-xs text-stone-500 italic">
                    <span className="flex gap-1">
                      <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-stone-400 [animation-delay:-0.3s]"></span>
                      <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-stone-400 [animation-delay:-0.15s]"></span>
                      <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-stone-400"></span>
                    </span>
                    @{activeConversation.otherUsername} is typing...
                  </div>
                ) : null}
              </>
            )}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="border-t border-stone-200 bg-white p-4">
          <div className="mx-auto flex max-w-3xl gap-3">
            <input
              value={draft}
              onChange={(event) => handleDraftChange(event.target.value)}
              className="h-11 flex-1 rounded-md border border-stone-300 px-4 outline-none focus:border-accent"
              disabled={!activeConversation || activeConversation.status !== 'ACCEPTED'}
              placeholder={
                activeConversation?.status === 'ACCEPTED'
                  ? `Message @${activeConversation.otherUsername}`
                  : 'Select an accepted conversation'
              }
            />
            <button
              type="submit"
              disabled={!activeConversation || activeConversation.status !== 'ACCEPTED'}
              className="inline-flex h-11 items-center gap-2 rounded-md bg-accent px-4 text-sm font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-stone-300"
            >
              <SendHorizontal className="h-4 w-4" />
              Send
            </button>
          </div>
        </form>
      </section>
    </main>
  );
};
