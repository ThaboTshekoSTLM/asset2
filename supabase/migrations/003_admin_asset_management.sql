-- Administrators may correct or archive assets while movement and audit history
-- remains intact. Archived assets are hidden by application queries.
alter table public.assets
  add column if not exists deleted_at timestamptz,
  add column if not exists deleted_by uuid references public.profiles(id);

drop policy if exists "writers update assets" on public.assets;
drop policy if exists "admins update assets" on public.assets;
drop policy if exists "authenticated users read assets" on public.assets;

create policy "authenticated users read assets"
on public.assets for select to authenticated
using (deleted_at is null);

create policy "admins update assets"
on public.assets for update to authenticated
using (public.is_admin())
with check (public.is_admin() and updated_by = auth.uid());
