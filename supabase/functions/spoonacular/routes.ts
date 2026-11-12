const DIETS = new Set([
  "gluten free",
  "ketogenic",
  "vegetarian",
  "lacto-vegetarian",
  "ovo-vegetarian",
  "vegan",
  "pescetarian",
  "paleo",
  "primal",
  "low fodmap",
  "whole30",
]);
const INTOLERANCES = new Set([
  "dairy",
  "egg",
  "gluten",
  "grain",
  "peanut",
  "seafood",
  "sesame",
  "shellfish",
  "soy",
  "sulfite",
  "tree nut",
  "wheat",
]);
const CUISINES = new Set([
  "african",
  "asian",
  "american",
  "british",
  "cajun",
  "caribbean",
  "chinese",
  "eastern european",
  "european",
  "french",
  "german",
  "greek",
  "indian",
  "irish",
  "italian",
  "japanese",
  "jewish",
  "korean",
  "latin american",
  "mediterranean",
  "mexican",
  "middle eastern",
  "nordic",
  "southern",
  "spanish",
  "thai",
  "vietnamese",
]);

function preferenceParams(body: Body): Record<string, string> | null {
  const params: Record<string, string> = {};
  for (
    const [name, allowed] of [
      ["diet", DIETS],
      ["intolerances", INTOLERANCES],
      ["cuisine", CUISINES],
    ] as const
  ) {
    const value = body[name];
    if (value === undefined || value === "") continue;
    if (typeof value !== "string" || value.length > 500) return null;
    const values = value.split(",").map((part) => part.trim().toLowerCase());
    if (values.length > 30 || !values.every((part) => allowed.has(part))) {
      return null;
    }
    params[name] = [...new Set(values)].sort().join(",");
  }
  return params;
}

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

export type Body = {
  action?: string;
  query?: string;
  ingredients?: string[];
  id?: string;
  ids?: string[];
  upc?: string;
  diet?: string;
  intolerances?: string;
  cuisine?: string;
};

const text = (value: unknown) =>
  typeof value === "string" && value.trim().length > 0 &&
    value.length <= MAX_TEXT
    ? value.trim()
    : null;

/** Maps a request to a Spoonacular path and query, or null if it is not allowed. */
export function route(body: Body): [string, Record<string, string>] | null {
  if (!body || typeof body !== "object" || Array.isArray(body)) return null;
  switch (body.action) {
    case "search": {
      const query = text(body.query);
      const preferences = preferenceParams(body);
      return query && preferences
        ? ["/recipes/complexSearch", {
          ...RECIPE_DETAILS,
          ...preferences,
          query,
        }]
        : null;
    }
    case "findByIngredients": {
      const names = Array.isArray(body.ingredients)
        ? body.ingredients.map(text)
        : [];
      if (
        names.length === 0 || names.length > MAX_INGREDIENTS ||
        names.includes(null)
      ) return null;
      const preferences = preferenceParams(body);
      if (!preferences) return null;
      return ["/recipes/complexSearch", {
        ...RECIPE_DETAILS,
        ...preferences,
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
      if (
        !ids.every((id) => typeof id === "string" && /^[0-9]{1,10}$/.test(id))
      ) return null;
      return ["/recipes/informationBulk", {
        ids: ids.join(","),
        includeNutrition: "false",
      }];
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
      return /^[0-9]{8,14}$/.test(body.upc ?? "")
        ? [`/food/products/upc/${body.upc}`, {}]
        : null;
    default:
      return null;
  }
}
