begin;

-- Comment photos. Public read: anyone with an image's URL can view it, which matches comments
-- being visible to every user. The app uploads JPEGs; PNG and WebP are allowed for other clients.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('comment-images', 'comment-images', true, 5242880,
        array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

-- Signed-in users write only inside their own folder: <user_id>/<file>.
-- Select is needed by the Storage API for deletes; public URLs bypass these policies.
create policy comment_images_select_own on storage.objects for select to authenticated
  using (bucket_id = 'comment-images'
         and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy comment_images_insert_own on storage.objects for insert to authenticated
  with check (bucket_id = 'comment-images'
              and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy comment_images_delete_own on storage.objects for delete to authenticated
  using (bucket_id = 'comment-images'
         and (storage.foldername(name))[1] = (select auth.uid())::text);

commit;
