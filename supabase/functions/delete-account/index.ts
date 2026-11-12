import { createClient } from "jsr:@supabase/supabase-js@2";
import { deleteAccount } from "./handler.ts";

Deno.serve((req) =>
  deleteAccount(req, {
    userId: async (token) => {
      const auth = createClient(
        Deno.env.get("SUPABASE_URL")!,
        Deno.env.get("SUPABASE_ANON_KEY")!,
        { auth: { persistSession: false, autoRefreshToken: false } },
      );
      // getUser contacts Auth to validate the JWT and check that the user still exists.
      const { data, error } = await auth.auth.getUser(token);
      return error ? null : data.user?.id ?? null;
    },
    deleteUser: async (id) => {
      // Separate admin client; never attach the caller's Authorization header to it.
      const admin = createClient(
        Deno.env.get("SUPABASE_URL")!,
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
        { auth: { persistSession: false, autoRefreshToken: false } },
      );
      const { error } = await admin.auth.admin.deleteUser(id);
      return !error;
    },
  })
);
