create table if not exists public.auto_reply_settings (
    user_id uuid primary key references public.app_users(id) on delete cascade,
    enabled boolean not null default false,
    delay_minutes integer not null default 5 check (delay_minutes between 1 and 1440),
    instructions text not null default '',
    updated_at timestamptz not null default now()
);

alter table public.direct_messages
add column if not exists ai_generated boolean not null default false;

alter table public.auto_reply_settings enable row level security;

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
