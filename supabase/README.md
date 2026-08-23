# Shared backend setup

This folder contains the production database foundation for the ICT Asset Register.

## Create the backend

1. Create a Supabase project for the municipality.
2. Open **SQL Editor**, paste `schema.sql`, and run it once.
3. In **Authentication > URL Configuration**, add the deployed web URL.
4. Create the first user in **Authentication > Users**. Set user metadata:

   ```json
   { "full_name": "ICT Administrator", "username": "admin" }
   ```

5. In **Table Editor > profiles**, change that first user's role to `admin`.
6. Copy `web/config.example.js` to `web/config.js` and add the project's URL and anon key.

Do not expose the Supabase service-role key in the web or Android apps. Administrative
user creation should later be implemented in a server-side Edge Function.

## Deployment modes

- Empty `web/config.js`: the existing local demonstration data is used.
- Configured `web/config.js`: the next integration stage will use Supabase Auth and the
  shared tables. The schema and security policies are ready for that stage.

Before production launch, disable open public signup unless the municipality explicitly
wants self-registration. Users should normally be created by an administrator.
