# alchery-explorer

A read-only web UI over the [alchery](https://github.com/andysmith-ai/alchery) API.

The explorer is a pure client: it talks to alchery only over its public HTTP API,
never its database or internals. It lets you ingest a url, add a note, browse the
graph, and search it by meaning or by text.

## Run

alchery must be running (its API defaults to `http://127.0.0.1:8080`). Then:

```sh
clj -M:run     # serves the explorer (defaults to http://127.0.0.1:8091)
```

Configuration (environment):

- `ALCHERY_API_URL` — the alchery API base (default `http://127.0.0.1:8080`)
- `ALCHERY_EXPLORER_PORT` — the port to serve on (default `8091`)
