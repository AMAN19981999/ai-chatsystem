import { FormEvent, useEffect, useState } from 'react';
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
  const [autoReplySettings, setAutoReplySettings] = useState<AutoReplySettings>({
    enabled: false,
    delayMinutes: 5,
    instructions: '',
  });

  useEffect(() => {
    void chatService.listDirectConversations().then((items) => {
      setConversations(items);
      setActiveConversationId((current) => current ?? items[0]?.id ?? null);
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
    if (!token || !activeConversationId) {
      return;
    }

    const activeConversation = conversations.find((item) => item.id === activeConversationId);
    if (activeConversation?.status !== 'ACCEPTED') {
      return;
    }

    const nextSocket = createDirectSocket(activeConversationId, token, (message) => {
      setMessages((items) => {
        if (items.some((item) => item.id === message.id)) {
          return items;
        }

        return [...items, message];
      });
    });
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
              <span className="block truncate">@{conversation.otherUsername}</span>
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
            <h1 className="text-base font-semibold text-ink">
              {activeConversation ? `@${activeConversation.otherUsername}` : 'Messages'}
            </h1>
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
              messages.map((message) => {
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
              })
            )}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="border-t border-stone-200 bg-white p-4">
          <div className="mx-auto flex max-w-3xl gap-3">
            <input
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
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
