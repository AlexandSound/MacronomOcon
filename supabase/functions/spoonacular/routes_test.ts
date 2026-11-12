import { type Body, route } from "./routes.ts";

function assert(
  condition: unknown,
  message = "Assertion failed",
): asserts condition {
  if (!condition) throw new Error(message);
}

Deno.test("search and pantry recommendations forward all dietary filters", () => {
  for (
    const body of [
      { action: "search", query: "pasta" },
      { action: "findByIngredients", ingredients: ["rice"] },
    ]
  ) {
    const result = route({
      ...body,
      diet: "vegan,gluten free",
      intolerances: "peanut,soy",
      cuisine: "Italian",
    });
    assert(result?.[0] === "/recipes/complexSearch");
    assert(result[1].diet === "gluten free,vegan");
    assert(result[1].intolerances === "peanut,soy");
    assert(result[1].cuisine === "italian");
    assert(result[1].number === "10");
  }
});

Deno.test("rejects malformed or unsupported filters rather than silently dropping restrictions", () => {
  for (
    const filter of [
      { diet: "vegan|vegetarian" },
      { intolerances: "unknown" },
      { cuisine: "unknown" },
      { diet: ["vegan"] },
      { cuisine: "a".repeat(501) },
    ]
  ) {
    assert(
      route({ action: "search", query: "pasta", ...filter } as Body) === null,
    );
  }
  assert(route(null as unknown as Body) === null);
  assert(route([] as unknown as Body) === null);
});

Deno.test("existing unfiltered searches and ingredient bounds still work", () => {
  const plain = route({ action: "search", query: "rice" });
  assert(plain?.[1].query === "rice");
  assert(plain[1].diet === undefined);
  assert(route({ action: "findByIngredients", ingredients: [] }) === null);
  assert(
    route({
      action: "findByIngredients",
      ingredients: Array(21).fill("rice"),
    }) === null,
  );
  assert(
    route({ action: "recipes", ids: ["123"] })?.[0] ===
      "/recipes/informationBulk",
  );
  assert(
    route({ action: "ingredients", query: "rice" })?.[1].cuisine === undefined,
  );
});
