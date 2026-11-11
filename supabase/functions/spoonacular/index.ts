// Authenticated Spoonacular proxy. The API key is a server secret (SPOONACULAR_API_KEY) and
// never ships in the Android app. Only the actions below are forwarded, with bounded inputs.
// Responses are passed through and not stored: Spoonacular's terms restrict caching.
import { createClient } from "jsr:@supabase/supabase-js@2";

const API = "https://api.spoonacular.com";
const MAX_RESULTS = 10;
const MAX_INGREDIENTS = 20;
const MAX_BULK_IDS = 20;
const MAX_TEXT = 100;

// One Spoonacular call per request; complexSearch also returns ingredients and instructions.
const RECIPE_DETAILS = {
  addRecipeInformation: "true",
  fillIngredients: "true",
  instructionsRequired: "true",
  number: String(MAX_RESULTS),
};

type Body = {
  action?: string;
  query?: string;
  ingredients?: string[];
  id?: string;
  ids?: string[];
  upc?: string;
};

const text = (value: unknown) =>
  typeof value === "string" && value.trim().length > 0 && value.length <= MAX_TEXT
    ? value.trim()
    : null;

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** Maps a request to a Spoonacular path and query, or null if it is not allowed. */
function route(body: Body): [string, Record<string, string>] | null {
  switch (body.action) {
    case "search": {
      const query = text(body.query);
      return query ? ["/recipes/complexSearch", { ...RECIPE_DETAILS, query }] : null;
    }
    case "findByIngredients": {
      const names = Array.isArray(body.ingredients) ? body.ingredients.map(text) : [];
      if (names.length === 0 || names.length > MAX_INGREDIENTS || names.includes(null)) return null;
      return ["/recipes/complexSearch", {
        ...RECIPE_DETAILS,
        includeIngredients: names.join(","),
        sort: "max-used-ingredients",
      }];
    }
    case "recipe":
      return /^[0-9]{1,10}$/.test(body.id ?? "")
        ? [`/recipes/${body.id}/information`, { includeNutrition: "false" }]
        : null;
    case "recipes": {
      // informationBulk: one call for several saved recipes.
      const ids = Array.isArray(body.ids) ? body.ids : [];
      if (ids.length === 0 || ids.length > MAX_BULK_IDS) return null;
      if (!ids.every((id) => typeof id === "string" && /^[0-9]{1,10}$/.test(id))) return null;
      return ["/recipes/informationBulk", { ids: ids.join(","), includeNutrition: "false" }];
    }
    case "ingredients": {
      const query = text(body.query);
      return query
        ? ["/food/ingredients/search", {
          query,
          number: String(MAX_RESULTS),
          metaInformation: "true",
        }]
        : null;
    }
    case "upc":
      return /^[0-9]{8,14}$/.test(body.upc ?? "") ? [`/food/products/upc/${body.upc}`, {}] : null;
    default:
      return null;
  }
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  // Only signed-in app users may spend the project's Spoonacular quota.
  const token = req.headers.get("Authorization")?.replace(/^Bearer\s+/i, "") ?? "";
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
  );
  const { data: authData, error } = await supabase.auth.getUser(token);
  if (error || !authData.user) return json({ error: "unauthorized" }, 401);

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
  for (const [name, value] of Object.entries(params)) url.searchParams.set(name, value);
  const upstream = await fetch(url, { headers: { "x-api-key": key } });

  // 402 means the daily point quota is used up; other upstream failures are not detailed.
  if (upstream.status === 402) return json({ error: "quota_exceeded" }, 429);
  if (upstream.status === 404) return json({ error: "not_found" }, 404);
  if (!upstream.ok) return json({ error: "upstream_error" }, 502);
  // Bulk endpoints return an array; wrap it so every response is an object.
  const upstreamData = await upstream.json();
  return json(Array.isArray(upstreamData) ? { results: upstreamData } : upstreamData);
});
