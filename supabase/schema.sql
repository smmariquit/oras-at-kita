-- Oras at Kita backend. Each phone signs in anonymously, so no names are stored.

create table public.groups (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  code text not null unique check (code = lower(code)),
  created_at timestamptz not null default now()
);

create table public.members (
  user_id uuid primary key references auth.users on delete cascade,
  group_id uuid not null references public.groups on delete cascade,
  member_no int not null check (member_no between 1 and 999),
  joined_at timestamptz not null default now(),
  unique (group_id, member_no)
);
create index members_group_idx on public.members (group_id);

create table public.entries (
  id text primary key,
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  cat text not null check (cat in ('bahay', 'alaga', 'babuyan', 'kapehan', 'samahan', 'pahinga')),
  day date not null,
  mins int not null check (mins between 1 and 960),
  started_at timestamptz,
  created_at timestamptz not null default now()
);
create index entries_user_day_idx on public.entries (user_id, day);

alter table public.groups enable row level security;
alter table public.members enable row level security;
alter table public.entries enable row level security;

-- Groups are read only through the functions below, so codes stay private.

create policy "read own membership" on public.members
  for select to authenticated using (user_id = (select auth.uid()));
create policy "leave group" on public.members
  for delete to authenticated using (user_id = (select auth.uid()));

create policy "read own entries" on public.entries
  for select to authenticated using (user_id = (select auth.uid()));
create policy "add own entries" on public.entries
  for insert to authenticated with check (user_id = (select auth.uid()));
create policy "update own entries" on public.entries
  for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "delete own entries" on public.entries
  for delete to authenticated using (user_id = (select auth.uid()));

-- Join a group by its code with a member number. Returns the group name.
create function public.join_group(p_code text, p_member_no int)
returns text
language plpgsql security definer set search_path = ''
as $$
declare
  g public.groups;
begin
  if auth.uid() is null then
    raise exception 'not signed in' using errcode = '28000';
  end if;
  select * into g from public.groups where code = lower(trim(p_code));
  if not found then
    raise exception 'unknown group code' using errcode = 'P0002';
  end if;
  insert into public.members (user_id, group_id, member_no)
  values (auth.uid(), g.id, p_member_no)
  on conflict (user_id) do update set group_id = excluded.group_id, member_no = excluded.member_no, joined_at = now();
  return g.name;
end;
$$;

-- Totals for the caller's group over a date range. Only sums leave the database.
create function public.group_week(p_from date, p_to date)
returns json
language sql stable security definer set search_path = ''
as $$
  with me as (
    select group_id from public.members where user_id = auth.uid()
  ),
  rows as (
    select e.cat, e.mins, e.user_id
    from public.entries e
    join public.members m on m.user_id = e.user_id
    where m.group_id = (select group_id from me) and e.day between p_from and p_to
  )
  select json_build_object(
    'name', (select g.name from public.groups g where g.id = (select group_id from me)),
    'members', (select count(*) from public.members m where m.group_id = (select group_id from me)),
    'reporting', (select count(distinct user_id) from rows),
    'cats', coalesce((select json_object_agg(cat, total) from (select cat, sum(mins) as total from rows group by cat) t), '{}'::json)
  )
  where exists (select 1 from me);
$$;

revoke execute on function public.join_group(text, int) from public, anon;
revoke execute on function public.group_week(date, date) from public, anon;
grant execute on function public.join_group(text, int) to authenticated;
grant execute on function public.group_week(date, date) to authenticated;
