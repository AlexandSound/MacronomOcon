begin;

-- Fields the app shows and filters on. Existing rows get neutral defaults.
-- Enum values match the Android Difficulty and MealType names.
alter table public.custom_recipes
  add column prep_time integer not null default 0 check (prep_time between 0 and 10080),
  add column cook_time integer not null default 0 check (cook_time between 0 and 10080),
  add column difficulty text not null default 'EASY'
    check (difficulty in ('EASY', 'MEDIUM', 'HARD')),
  add column meal_type text not null default 'DINNER'
    check (meal_type in ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')),
  add column cuisine text not null default '' check (length(cuisine) <= 100),
  add column tools_needed text[] not null default '{}';

-- Lists are read newest first.
drop index public.custom_recipes_owner;
create index custom_recipes_owner on public.custom_recipes(user_id, created_at desc);

commit;
