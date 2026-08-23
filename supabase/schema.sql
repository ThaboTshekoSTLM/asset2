-- ICT Asset Register production schema for Supabase/PostgreSQL.
-- Run this file in a new Supabase project's SQL editor.

create extension if not exists pgcrypto;

create type public.app_role as enum (
  'admin',
  'standard_user',
  'ict_technician',
  'viewer_auditor'
);

create type public.movement_type as enum (
  'new_allocation',
  'transfer',
  'return',
  'repair',
  'disposal'
);

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null check (length(trim(full_name)) > 0),
  username text not null unique check (username = lower(username)),
  role public.app_role not null default 'standard_user',
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.assets (
  id uuid primary key default gen_random_uuid(),
  device_description text not null,
  asset_barcode text not null unique check (asset_barcode = upper(asset_barcode)),
  serial_number text not null unique check (serial_number = upper(serial_number)),
  department text not null,
  section text not null default '',
  building text not null,
  office_number text not null,
  room_barcode text not null default '',
  current_owner text not null,
  previous_owner text not null default '',
  technician text not null,
  registered_at timestamptz not null default now(),
  moved_at timestamptz,
  movement_type public.movement_type not null default 'new_allocation',
  notes text not null default '',
  photo_path text,
  created_by uuid references public.profiles(id),
  updated_by uuid references public.profiles(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.asset_movements (
  id uuid primary key default gen_random_uuid(),
  asset_id uuid not null references public.assets(id) on delete restrict,
  previous_owner text not null default '',
  new_owner text not null,
  previous_location text not null default '',
  new_building text not null,
  new_office_number text not null,
  department text not null,
  section text not null default '',
  room_barcode text not null default '',
  movement_type public.movement_type not null,
  reason text not null,
  technician text not null,
  confirmation text not null,
  movement_date timestamptz not null default now(),
  created_by uuid not null references public.profiles(id)
);

create table public.audit_logs (
  id bigint generated always as identity primary key,
  actor_user_id uuid references public.profiles(id),
  action text not null,
  entity_type text not null,
  entity_id uuid,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index assets_department_idx on public.assets(department);
create index assets_building_idx on public.assets(building);
create index assets_current_owner_idx on public.assets(current_owner);
create index asset_movements_asset_date_idx on public.asset_movements(asset_id, movement_date desc);
create index audit_logs_created_at_idx on public.audit_logs(created_at desc);

create or replace function public.current_profile_role()
returns public.app_role
language sql
stable
security definer
set search_path = public
as $$
  select role from public.profiles where id = auth.uid() and active = true
$$;

create or replace function public.can_write_assets()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(public.current_profile_role() in ('admin', 'standard_user', 'ict_technician'), false)
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(public.current_profile_role() = 'admin', false)
$$;

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_touch_updated_at
before update on public.profiles
for each row execute function public.touch_updated_at();

create trigger assets_touch_updated_at
before update on public.assets
for each row execute function public.touch_updated_at();

create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, full_name, username)
  values (
    new.id,
    coalesce(nullif(trim(new.raw_user_meta_data ->> 'full_name'), ''), split_part(new.email, '@', 1)),
    lower(coalesce(nullif(trim(new.raw_user_meta_data ->> 'username'), ''), split_part(new.email, '@', 1)))
  );
  return new;
end;
$$;

create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_auth_user();

create or replace function public.audit_row_change()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  row_id uuid;
begin
  row_id := coalesce(new.id, old.id);
  insert into public.audit_logs (actor_user_id, action, entity_type, entity_id, details)
  values (
    auth.uid(),
    tg_op,
    tg_table_name,
    row_id,
    case when tg_op = 'DELETE' then to_jsonb(old) else to_jsonb(new) end
  );
  return coalesce(new, old);
end;
$$;

create trigger audit_assets
after insert or update or delete on public.assets
for each row execute function public.audit_row_change();

create trigger audit_movements
after insert or update or delete on public.asset_movements
for each row execute function public.audit_row_change();

-- Records a movement and updates the asset in one transaction.
create or replace function public.record_asset_movement(
  p_asset_id uuid,
  p_new_owner text,
  p_new_building text,
  p_new_office_number text,
  p_department text,
  p_section text,
  p_room_barcode text,
  p_movement_type public.movement_type,
  p_reason text,
  p_technician text,
  p_confirmation text
)
returns public.asset_movements
language plpgsql
security invoker
set search_path = public
as $$
declare
  existing_asset public.assets;
  created_movement public.asset_movements;
begin
  if not public.can_write_assets() then
    raise exception 'Insufficient permission';
  end if;

  select * into existing_asset from public.assets where id = p_asset_id for update;
  if not found then raise exception 'Asset not found'; end if;

  insert into public.asset_movements (
    asset_id, previous_owner, new_owner, previous_location,
    new_building, new_office_number, department, section, room_barcode,
    movement_type, reason, technician, confirmation, created_by
  ) values (
    existing_asset.id, existing_asset.current_owner, trim(p_new_owner),
    concat_ws(' / ', existing_asset.building, existing_asset.office_number, nullif(existing_asset.room_barcode, '')),
    trim(p_new_building), trim(p_new_office_number), trim(p_department), trim(p_section), upper(trim(p_room_barcode)),
    p_movement_type, trim(p_reason), trim(p_technician), trim(p_confirmation), auth.uid()
  ) returning * into created_movement;

  update public.assets set
    previous_owner = existing_asset.current_owner,
    current_owner = trim(p_new_owner),
    building = trim(p_new_building),
    office_number = trim(p_new_office_number),
    department = trim(p_department),
    section = trim(p_section),
    room_barcode = upper(trim(p_room_barcode)),
    movement_type = p_movement_type,
    technician = trim(p_technician),
    moved_at = created_movement.movement_date,
    updated_by = auth.uid()
  where id = existing_asset.id;

  return created_movement;
end;
$$;

alter table public.profiles enable row level security;
alter table public.assets enable row level security;
alter table public.asset_movements enable row level security;
alter table public.audit_logs enable row level security;

create policy "authenticated users read active profiles"
on public.profiles for select to authenticated
using (active = true or id = auth.uid() or public.is_admin());

create policy "admins update profiles"
on public.profiles for update to authenticated
using (public.is_admin()) with check (public.is_admin());

create policy "authenticated users read assets"
on public.assets for select to authenticated using (true);

create policy "writers insert assets"
on public.assets for insert to authenticated
with check (public.can_write_assets() and created_by = auth.uid());

create policy "writers update assets"
on public.assets for update to authenticated
using (public.can_write_assets())
with check (public.can_write_assets() and updated_by = auth.uid());

create policy "admins delete assets"
on public.assets for delete to authenticated using (public.is_admin());

create policy "authenticated users read movements"
on public.asset_movements for select to authenticated using (true);

create policy "writers insert movements"
on public.asset_movements for insert to authenticated
with check (public.can_write_assets() and created_by = auth.uid());

create policy "auditors and admins read audit logs"
on public.audit_logs for select to authenticated
using (public.current_profile_role() in ('admin', 'viewer_auditor'));

-- Asset photos are private and readable only by authenticated users.
insert into storage.buckets (id, name, public)
values ('asset-photos', 'asset-photos', false)
on conflict (id) do nothing;

create policy "authenticated users read asset photos"
on storage.objects for select to authenticated
using (bucket_id = 'asset-photos');

create policy "writers upload asset photos"
on storage.objects for insert to authenticated
with check (bucket_id = 'asset-photos' and public.can_write_assets());

create policy "writers update asset photos"
on storage.objects for update to authenticated
using (bucket_id = 'asset-photos' and public.can_write_assets())
with check (bucket_id = 'asset-photos' and public.can_write_assets());

create policy "admins delete asset photos"
on storage.objects for delete to authenticated
using (bucket_id = 'asset-photos' and public.is_admin());

-- Explicit Data API grants. Row-level security policies above still decide which
-- rows each authenticated user may access.
grant usage on schema public to authenticated;
grant select on public.profiles to authenticated;
grant update on public.profiles to authenticated;
grant select, insert, update, delete on public.assets to authenticated;
grant select, insert on public.asset_movements to authenticated;
grant select on public.audit_logs to authenticated;
grant usage, select on sequence public.audit_logs_id_seq to authenticated;
grant execute on function public.current_profile_role() to authenticated;
grant execute on function public.can_write_assets() to authenticated;
grant execute on function public.is_admin() to authenticated;
grant execute on function public.record_asset_movement(
  uuid, text, text, text, text, text, text, public.movement_type, text, text, text
) to authenticated;

