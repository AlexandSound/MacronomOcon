import { deleteAccount } from "./handler.ts";

function assert(
  condition: unknown,
  message = "Assertion failed",
): asserts condition {
  if (!condition) throw new Error(message);
}

Deno.test("requires POST and a Bearer token before calling Auth or admin", async () => {
  let called = false;
  const deps = {
    userId: async () => {
      called = true;
      return "user-a";
    },
    deleteUser: async () => {
      called = true;
      return true;
    },
  };
  assert(
    (await deleteAccount(new Request("https://example.com"), deps)).status ===
      405,
  );
  assert(
    (await deleteAccount(
      new Request("https://example.com", { method: "POST" }),
      deps,
    )).status === 401,
  );
  assert(!called);
});

Deno.test("invalid JWT cannot delete an account", async () => {
  let deleted = false;
  const response = await deleteAccount(
    new Request("https://example.com", {
      method: "POST",
      headers: { Authorization: "Bearer invalid" },
    }),
    {
      userId: async () => null,
      deleteUser: async () => {
        deleted = true;
        return true;
      },
    },
  );
  assert(response.status === 401);
  assert(!deleted);
});

Deno.test("ignores a supplied target and only deletes the verified caller", async () => {
  let deletedId = "";
  const response = await deleteAccount(
    new Request("https://example.com?userId=victim", {
      method: "POST",
      headers: { Authorization: "Bearer caller-jwt" },
      body: JSON.stringify({ userId: "victim" }),
    }),
    {
      userId: async (token) => token === "caller-jwt" ? "caller" : null,
      deleteUser: async (id) => {
        deletedId = id;
        return true;
      },
    },
  );
  assert(response.status === 200);
  assert(deletedId === "caller");
  assert((await response.json()).deleted === true);
});

Deno.test("reports deletion failures without leaking server details", async () => {
  const request = () =>
    new Request("https://example.com", {
      method: "POST",
      headers: { Authorization: "Bearer jwt" },
    });
  const failed = await deleteAccount(request(), {
    userId: async () => "caller",
    deleteUser: async () => false,
  });
  assert(failed.status === 500);
  const thrown = await deleteAccount(request(), {
    userId: async () => "caller",
    deleteUser: async () => {
      throw Error("secret");
    },
  });
  assert(thrown.status === 500);
  assert(!(await thrown.text()).includes("secret"));
});
