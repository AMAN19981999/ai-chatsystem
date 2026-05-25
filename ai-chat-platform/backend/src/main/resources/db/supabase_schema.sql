-- Supabase PostgreSQL starter schema for ai-chat-platform.
-- Run this in Supabase SQL Editor before pointing the backend at Supabase DB.

create extension if not exists pgcrypto;

create table if not exists public.app_users (
    id uuid primary key default gen_random_uuid(),
    supabase_auth_user_id uuid unique,
    username text not null unique,
    email text not null unique,
    display_name text not null,
    avatar_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.chat_threads (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references public.app_users(id) on delete cascade,
    title text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.messages (
    id uuid primary key default gen_random_uuid(),
    chat_id uuid not null references public.chat_threads(id) on delete cascade,
    sender_id uuid references public.app_users(id) on delete set null,
    role text not null check (role in ('USER', 'ASSISTANT', 'SYSTEM')),
    content text not null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create table if not exists public.direct_conversations (
    id uuid primary key default gen_random_uuid(),
    requester_id uuid not null references public.app_users(id) on delete cascade,
    recipient_id uuid not null references public.app_users(id) on delete cascade,
    status text not null check (status in ('PENDING', 'ACCEPTED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint direct_conversations_no_self_request check (requester_id <> recipient_id)
);

create unique index if not exists idx_direct_conversations_user_pair
on public.direct_conversations (
    least(requester_id, recipient_id),
    greatest(requester_id, recipient_id)
);

create table if not exists public.direct_messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid not null references public.direct_conversations(id) on delete cascade,
    sender_id uuid not null references public.app_users(id) on delete cascade,
    content text not null,
    ai_generated boolean not null default false,
    created_at timestamptz not null default now()
);

create table if not exists public.auto_reply_settings (
    user_id uuid primary key references public.app_users(id) on delete cascade,
    enabled boolean not null default false,
    delay_minutes integer not null default 5 check (delay_minutes between 1 and 1440),
    instructions text not null default '',
    updated_at timestamptz not null default now()
);

create index if not exists idx_chat_threads_owner_id on public.chat_threads(owner_id);
create index if not exists idx_chat_threads_updated_at on public.chat_threads(updated_at desc);
create index if not exists idx_app_users_username on public.app_users(lower(username));
create unique index if not exists idx_app_users_username_lower_unique on public.app_users(lower(username));
create unique index if not exists idx_app_users_email_lower_unique on public.app_users(lower(email));
create index if not exists idx_messages_chat_id_created_at on public.messages(chat_id, created_at asc);
create index if not exists idx_messages_role on public.messages(role);
create index if not exists idx_direct_conversations_requester_id on public.direct_conversations(requester_id);
create index if not exists idx_direct_conversations_recipient_id on public.direct_conversations(recipient_id);
create index if not exists idx_direct_messages_conversation_id_created_at on public.direct_messages(conversation_id, created_at asc);

alter table public.app_users enable row level security;
alter table public.chat_threads enable row level security;
alter table public.messages enable row level security;
alter table public.direct_conversations enable row level security;
alter table public.direct_messages enable row level security;
alter table public.auto_reply_settings enable row level security;

-- App users can read and update their own profile when Supabase Auth is wired in.
create policy "Users can read own profile"
on public.app_users
for select
using (supabase_auth_user_id = auth.uid());

create policy "Users can update own profile"
on public.app_users
for update
using (supabase_auth_user_id = auth.uid())
with check (supabase_auth_user_id = auth.uid());

-- Chat access is scoped to the authenticated owner.
create policy "Users can read own chats"
on public.chat_threads
for select
using (
    owner_id in (
        select id from public.app_users where supabase_auth_user_id = auth.uid()
    )
);

create policy "Users can manage own chats"
on public.chat_threads
for all
using (
    owner_id in (
        select id from public.app_users where supabase_auth_user_id = auth.uid()
    )
)
with check (
    owner_id in (
        select id from public.app_users where supabase_auth_user_id = auth.uid()
    )
);

-- Message access follows chat ownership.
create policy "Users can read messages in own chats"
on public.messages
for select
using (
    chat_id in (
        select chat_threads.id
        from public.chat_threads
        join public.app_users on app_users.id = chat_threads.owner_id
        where app_users.supabase_auth_user_id = auth.uid()
    )
);

create policy "Users can manage messages in own chats"
on public.messages
for all
using (
    chat_id in (
        select chat_threads.id
        from public.chat_threads
        join public.app_users on app_users.id = chat_threads.owner_id
        where app_users.supabase_auth_user_id = auth.uid()
    )
)
with check (
    chat_id in (
        select chat_threads.id
        from public.chat_threads
        join public.app_users on app_users.id = chat_threads.owner_id
        where app_users.supabase_auth_user_id = auth.uid()
    )
);

create policy "Users can read direct conversations"
on public.direct_conversations
for select
using (
    requester_id in (select id from public.app_users where supabase_auth_user_id = auth.uid())
    or recipient_id in (select id from public.app_users where supabase_auth_user_id = auth.uid())
);

create policy "Users can create direct conversations"
on public.direct_conversations
for insert
with check (
    requester_id in (select id from public.app_users where supabase_auth_user_id = auth.uid())
);

create policy "Recipients can update direct conversations"
on public.direct_conversations
for update
using (
    recipient_id in (select id from public.app_users where supabase_auth_user_id = auth.uid())
)
with check (
    recipient_id in (select id from public.app_users where supabase_auth_user_id = auth.uid())
);

create policy "Users can read direct messages"
on public.direct_messages
for select
using (
    conversation_id in (
        select direct_conversations.id
        from public.direct_conversations
        join public.app_users on app_users.id in (
            direct_conversations.requester_id,
            direct_conversations.recipient_id
        )
        where app_users.supabase_auth_user_id = auth.uid()
    )
);

create policy "Users can create direct messages"
on public.direct_messages
for insert
with check (
    sender_id in (select id from public.app_users where supabase_auth_user_id = auth.uid())
    and conversation_id in (
        select direct_conversations.id
        from public.direct_conversations
        join public.app_users on app_users.id in (
            direct_conversations.requester_id,
            direct_conversations.recipient_id
        )
        where app_users.supabase_auth_user_id = auth.uid()
          and direct_conversations.status = 'ACCEPTED'
    )
);

create policy "Users can read own auto reply settings"
on public.auto_reply_settings
for select
using (
    user_id in (select id from public.app_users where supabase_auth_user_id = auth.uid())
);

create policy "Users can manage own auto reply settings"
on public.auto_reply_settings
for all
using (
    user_id in (select id from public.app_users where supabase_auth_user_id = auth.uid())
)
with check (
    user_id in (select id from public.app_users where supabase_auth_user_id = auth.uid())
);

-- Seed data for development inspection.
insert into public.app_users (id, username, email, display_name)
values ('00000000-0000-0000-0000-000000000001', 'demo', 'demo@aichat.local', 'Demo User')
on conflict (email) do nothing;

insert into public.chat_threads (id, owner_id, title)
values (
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000001',
    'Product strategy'
)
on conflict (id) do nothing;

insert into public.messages (id, chat_id, sender_id, role, content)
values (
    '00000000-0000-0000-0000-000000001001',
    '00000000-0000-0000-0000-000000000101',
    null,
    'ASSISTANT',
    'Welcome. Ask me anything and I will route the reply through the AI service boundary.'
)
on conflict (id) do nothing;
