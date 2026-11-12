type Dependencies = {
  userId: (token: string) => Promise<string | null>;
  deleteUser: (id: string) => Promise<boolean>;
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
    },
  });
}

/** The target always comes from verified Auth data, never from request JSON or URL parameters. */
export async function deleteAccount(
  req: Request,
  deps: Dependencies,
): Promise<Response> {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const authorization = req.headers.get("Authorization") ?? "";
  const token = /^Bearer\s+(\S+)$/i.exec(authorization)?.[1];
  if (!token) return json({ error: "unauthorized" }, 401);
  try {
    const id = await deps.userId(token);
    if (!id) return json({ error: "unauthorized" }, 401);
    if (!await deps.deleteUser(id)) {
      return json({ error: "deletion_failed" }, 500);
    }
    return json({ deleted: true });
  } catch {
    // Never return upstream exception details or secrets to the caller.
    return json({ error: "deletion_failed" }, 500);
  }
}
