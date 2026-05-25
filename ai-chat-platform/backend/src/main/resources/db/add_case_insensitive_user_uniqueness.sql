create unique index if not exists idx_app_users_username_lower_unique
on public.app_users(lower(username));

create unique index if not exists idx_app_users_email_lower_unique
on public.app_users(lower(email));
