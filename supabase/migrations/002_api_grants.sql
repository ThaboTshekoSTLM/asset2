-- Required because automatic table exposure is disabled for this project.
-- RLS policies remain the authorization boundary.
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
