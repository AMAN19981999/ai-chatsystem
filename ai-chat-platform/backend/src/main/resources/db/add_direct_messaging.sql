create table if not exists public.direct_conversations (
    id uuid primary key default gen_random_uuid(),
    requester_id uuid not null references public.app_users(id) on delete cascade,
    recipient_id uuid not null references public.app_users(id) on delete cascade,
    status text not null check (status in ('PENDING', 'ACCEPTED', 'BLOCKED')),
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

create index if not exists idx_direct_conversations_requester_id on public.direct_conversations(requester_id);
create index if not exists idx_direct_conversations_recipient_id on public.direct_conversations(recipient_id);
create index if not exists idx_direct_messages_conversation_id_created_at
on public.direct_messages(conversation_id, created_at asc);

alter table public.direct_conversations enable row level security;
alter table public.direct_messages enable row level security;

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
