begin;

-- Store user content and recipe references, never provider recipe payloads.
create table public.comments (
  comment_id uuid primary key default gen_random_uuid(),
  recipe_id text not null check (length(trim(recipe_id)) > 0),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  author_name text not null default '',
  text text not null default '',
  image_url text check (image_url is null or length(trim(image_url)) > 0),
  created_at timestamptz not null default now(),
  constraint comment_has_content check (text ~ '\S' or image_url is not null)
);
create index comments_recipe_created on public.comments(recipe_id, created_at, comment_id);
create index comments_author on public.comments(user_id);

create table public.comment_likes (
  comment_id uuid not null references public.comments(comment_id) on delete cascade,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  primary key (comment_id, user_id)
);
create index comment_likes_user on public.comment_likes(user_id);

-- Snapshot the authenticated author's public display name at publication. Invoker
-- security keeps profile reads subject to RLS; never use an email as a fallback.
create function public.set_comment_author_name()
returns trigger language plpgsql security invoker set search_path = '' as $$
begin
  new.author_name := coalesce(
    (select nullif(trim(display_name), '') from public.profiles where user_id = auth.uid()),
    ''
  );
  return new;
end;
$$;
revoke all on function public.set_comment_author_name() from public;
create trigger set_comment_author_name before insert on public.comments
  for each row execute function public.set_comment_author_name();

alter table public.comments enable row level security;
alter table public.comment_likes enable row level security;

create policy read_comments on public.comments for select to authenticated using (true);
create policy publish_own_comment on public.comments for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy delete_own_comment on public.comments for delete to authenticated
  using ((select auth.uid()) = user_id);
create policy read_comment_likes on public.comment_likes for select to authenticated using (true);
create policy like_another_users_comment on public.comment_likes for insert to authenticated
  with check (
    (select auth.uid()) = user_id
    and exists (
      select 1 from public.comments c
      where c.comment_id = comment_likes.comment_id and c.user_id <> (select auth.uid())
    )
  );

-- No edits or unlikes in this feature. Server owns IDs, author names and timestamps.
revoke all on public.comments, public.comment_likes from public, anon, authenticated;
grant select, delete on public.comments to authenticated;
grant insert (recipe_id, user_id, text, image_url) on public.comments to authenticated;
grant select on public.comment_likes to authenticated;
grant insert (comment_id, user_id) on public.comment_likes to authenticated;

commit;
