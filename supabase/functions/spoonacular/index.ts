// Authenticated Spoonacular proxy. The API key is a server secret (SPOONACULAR_API_KEY) and
// never ships in the Android app. Only the actions below are forwarded, with bounded inputs.
// Responses are passed through and not stored: Spoonacular's terms restrict caching.
import { createClient } from "jsr:@supabase/supabase-js@2";

import { type Body, route } from "./routes.ts";

const API = "https://api.spoonacular.com";

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  // Only signed-in app users may spend the project's Spoonacular quota.
  const token = req.headers.get("Authorization")?.replace(/^Bearer\s+/i, "") ??
    "";
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
  );
  const { data, error } = await supabase.auth.getUser(token);
  if (error || !data.user) return json({ error: "unauthorized" }, 401);

  const key = Deno.env.get("SPOONACULAR_API_KEY");
  if (!key) return json({ error: "not_configured" }, 500);

  let body: Body;
  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid_request" }, 400);
  }
  const target = route(body);
  if (!target) return json({ error: "invalid_request" }, 400);

  const [path, params] = target;
  const url = new URL(API + path);
  for (const [name, value] of Object.entries(params)) {
    url.searchParams.set(name, value);
  }
  const upstream = await fetch(url, { headers: { "x-api-key": key } });

  // 402 means the daily point quota is used up; other upstream failures are not detailed.
  if (upstream.status === 402) return json({ error: "quota_exceeded" }, 429);
  if (upstream.status === 404) return json({ error: "not_found" }, 404);
  if (!upstream.ok) return json({ error: "upstream_error" }, 502);
  // Bulk endpoints return an array; wrap it so every response is an object.
  const responseData = await upstream.json();
  return json(
    Array.isArray(responseData) ? { results: responseData } : responseData,
  );
});
