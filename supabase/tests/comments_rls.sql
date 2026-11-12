-- Run on a disposable Supabase database after migrations, as postgres:
-- psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/comments_rls.sql
-- No pgTAP dependency. Fixtures and helper are rolled back even after success.
begin;

create function pg_temp.expect_error(statement text, expected_state text)
returns void language plpgsql as $$
begin
  begin
    execute statement;
  exception when others then
    if sqlstate = expected_state then return; end if;
    raise;
  end;
  raise exception 'Expected SQLSTATE %, but statement succeeded: %', expected_state, statement;
end;
$$;

insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'comment-author@example.test'),
  ('10000000-0000-0000-0000-000000000002', 'comment-reader@example.test');
insert into public.profiles(user_id, display_name)
values ('10000000-0000-0000-0000-000000000001', 'Author');

set local role authenticated;
select set_config('request.jwt.claim.sub', '10000000-0000-0000-0000-000000000001', true);
insert into public.comments(recipe_id, text) values ('comment-rls-test', 'Hello');
insert into public.comments(recipe_id, text, image_url)
values ('comment-image-test', '', 'https://example.test/photo.jpg');

select pg_temp.expect_error(
  $sql$insert into public.comments(recipe_id, text) values ('empty', E' \n\t')$sql$, '23514'
);
select pg_temp.expect_error(
  $sql$insert into public.comments(recipe_id, text, user_id)
       values ('spoof', 'Hello', '10000000-0000-0000-0000-000000000002')$sql$, '42501'
);
select pg_temp.expect_error(
  $sql$insert into public.comments(recipe_id, text, author_name)
       values ('spoof-name', 'Hello', 'Someone else')$sql$, '42501'
);
select pg_temp.expect_error(
  $sql$insert into public.comment_likes(comment_id)
       select comment_id from public.comments where recipe_id = 'comment-rls-test'$sql$, '42501'
);

select set_config('request.jwt.claim.sub', '10000000-0000-0000-0000-000000000002', true);
do $$
begin
  if (select count(*) from public.comments where recipe_id = 'comment-rls-test'
      and author_name = 'Author') <> 1 then
    raise exception 'Another user must see the comment and server author name';
  end if;
end;
$$;
delete from public.comments where recipe_id = 'comment-rls-test';
do $$
begin
  if (select count(*) from public.comments where recipe_id = 'comment-rls-test') <> 1 then
    raise exception 'Non-author deleted comment';
  end if;
end;
$$;
select pg_temp.expect_error(
  $sql$update public.comments set text = 'Edited' where recipe_id = 'comment-rls-test'$sql$, '42501'
);
select pg_temp.expect_error(
  $sql$insert into public.comment_likes(comment_id, user_id)
       select comment_id, '10000000-0000-0000-0000-000000000001'::uuid
       from public.comments where recipe_id = 'comment-rls-test'$sql$, '42501'
);
insert into public.comment_likes(comment_id)
select comment_id from public.comments where recipe_id = 'comment-rls-test';
select pg_temp.expect_error(
  $sql$insert into public.comment_likes(comment_id)
       select comment_id from public.comments where recipe_id = 'comment-rls-test'$sql$, '23505'
);
select pg_temp.expect_error(
  $sql$insert into public.comment_likes(comment_id)
       values ('20000000-0000-0000-0000-000000000001')$sql$, '42501'
);

select set_config('request.jwt.claim.sub', '10000000-0000-0000-0000-000000000001', true);
do $$
begin
  if (select count(*) from public.comment_likes l join public.comments c using (comment_id)
      where c.recipe_id = 'comment-rls-test') <> 1 then
    raise exception 'Author must see the other user like';
  end if;
end;
$$;
delete from public.comments where recipe_id in ('comment-rls-test', 'comment-image-test');
reset role;
do $$
begin
  if exists (select 1 from public.comments where recipe_id in ('comment-rls-test', 'comment-image-test'))
     or exists (select 1 from public.comment_likes
                where user_id = '10000000-0000-0000-0000-000000000002') then
    raise exception 'Author delete must cascade to likes';
  end if;
end;
$$;

set local role anon;
select set_config('request.jwt.claim.sub', '', true);
select pg_temp.expect_error('select * from public.comments', '42501');
select pg_temp.expect_error('select * from public.comment_likes', '42501');
select pg_temp.expect_error(
  $sql$insert into public.comments(recipe_id, text) values ('anonymous', 'Hello')$sql$, '42501'
);
reset role;
rollback;
