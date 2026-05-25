-- Run this if you already created app_users before username existed.

alter table public.app_users
add column if not exists username text;

update public.app_users
set username = lower(regexp_replace(split_part(email, '@', 1), '[^a-zA-Z0-9_]', '_', 'g'))
where username is null;

alter table public.app_users
alter column username set not null;

create unique index if not exists idx_app_users_username_unique
on public.app_users(lower(username));
