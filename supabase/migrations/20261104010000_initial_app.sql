begin;

create table public.profiles (
  user_id uuid primary key default auth.uid() references auth.users(id) on delete cascade,
  display_name text not null default '' check (length(display_name) <= 100),
  dietary_restrictions text[] not null default '{}',
  preferred_cuisines text[] not null default '{}',
  cooking_methods text[] not null default '{}',
  notifications_enabled boolean not null default true
);

-- User-entered pantry data. Do not persist restricted Spoonacular product payloads.
create table public.pantry_items (
  pantry_item_id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  ingredient_id text not null check (length(trim(ingredient_id)) > 0),
  name text not null check (length(trim(name)) between 1 and 200),
  barcode text not null default '' check (barcode = '' or barcode ~ '^[0-9]{8,14}$'),
  category text not null default '',
  quantity numeric not null check (quantity >= 0 and quantity < 'Infinity'::numeric),
  unit text not null check (length(trim(unit)) between 1 and 30),
  expiration_date date not null,
  created_at timestamptz not null default now()
);
create index pantry_items_owner_expiration on public.pantry_items(user_id, expiration_date);

create table public.recipe_bookmarks (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  recipe_id text not null check (length(trim(recipe_id)) > 0),
  title text not null default '',
  image_url text not null default '',
  favorite boolean not null default false,
  blocked boolean not null default false,
  primary key (user_id, recipe_id)
);

-- Only recipes authored by the user, not copies of provider responses.
create table public.custom_recipes (
  recipe_id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  title text not null check (length(trim(title)) between 1 and 200),
  ingredients jsonb not null default '[]' check (jsonb_typeof(ingredients) = 'array'),
  instructions text[] not null default '{}',
  created_at timestamptz not null default now()
);
create index custom_recipes_owner on public.custom_recipes(user_id);

alter table public.profiles enable row level security;
alter table public.pantry_items enable row level security;
alter table public.recipe_bookmarks enable row level security;
alter table public.custom_recipes enable row level security;

create policy own_profile on public.profiles for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy own_pantry on public.pantry_items for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy own_bookmarks on public.recipe_bookmarks for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy own_custom_recipes on public.custom_recipes for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

revoke all on public.profiles, public.pantry_items, public.recipe_bookmarks, public.custom_recipes from anon;
grant select, insert, update, delete on public.profiles, public.pantry_items,
  public.recipe_bookmarks, public.custom_recipes to authenticated;

commit;
