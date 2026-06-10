# SVN MCP Server

**SVN MCP** is a [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server that exposes **Subversion (SVN)** repositories to AI coding assistants (for example Cursor or Claude Desktop). Clients connect over **Streamable HTTP**; the server talks to SVN with **SVNKit** inside the JVM—**no `svn` CLI** is required on the client or IDE host.

> *WebSVN-style read workflows, callable as MCP tools over Streamable HTTP.*

[한국어 문서](README_kr.md) · Full requirements: [PRD.md](PRD.md) · Code layout: [ARCHITECTURE.md](ARCHITECTURE.md)

---

## Why SVN MCP?

Traditional SVN browsing (for example [WebSVN](https://github.com/websvnphp/websvn)) assumes a browser. **SVN MCP** brings the same *kinds* of operations into the MCP ecosystem so agents can list repos, browse paths, read files, inspect history, diff, and search—using a single MCP connection instead of shelling out to `svn`.

---

## Current MCP Tools

Capabilities are aligned with WebSVN-style flows and exposed as MCP **`tool`** calls using stable `snake_case` names.

| Area | Tools / behavior |
|------|------------------|
| **Repositories** | `list_repositories` — configured repo IDs, names, root URLs, groups. |
| **Browse** | `list_path` — directory entries at a revision (or peg); tree or flat view with limits. |
| **Files** | `get_file` — file contents at a revision (text or Base64); metadata such as `mime_type`, `is_text`, `encoding_hint`. |
| **History** | `get_log` — commit log for a path with limits and revision range; `changed_paths` per revision. |
| **Revision** | `get_revision` — one revision’s metadata and changed paths (add/modify/delete). |
| **Diff** | `diff_file` — compare two revisions of a file. `diff_revision` — one revision’s unified diff or changed paths only. |
| **Blame** | `blame_file` — per-line revision, author, and content. |
| **Stats** | `resolve_revision_range`, `repository_author_stats` — map dates to revisions and aggregate author activity. |
| **Search** | `search_in_path` — search file contents under a path for a keyword (e.g. a table name) and return matching files with their last author. All file reads share a single SVN session to avoid N+1 connection overhead. Required: `repository_id`, `path`, `keyword`; optional: `revision`, `file_extensions` (comma-separated, default `java`), `case_sensitive`, `max_files_to_scan`, `max_matches`. |

**In scope:** MCP protocol, Streamable HTTP transport, read-heavy SVN operations above, multi-repo config, optional path-level authz.

**Out of scope (for now):** Recreating WebSVN’s PHP UI; **write** operations (commit, propset, …); hosting a public RSS feed (agents use `get_recent_activity` instead).

---

## Stack

| Layer | Choice |
|-------|--------|
| Runtime | Java **17** |
| App framework | **Spring Boot** |
| MCP transport | **Streamable HTTP** (default for AI tools) |
| SVN access | **SVNKit** (pure Java; protocols per SVNKit support) |

---

## Roadmap (high level)

| Phase | Focus |
|-------|--------|
| **MVP** | `list_repositories`, `list_path`, `get_file`, `get_log`, `get_revision`, `diff_file`, `blame_file`, basic errors |
| **1.1** | `diff_paths`, parent SVN URL live discovery, authz guard, tighter response limits |
| **1.2** | `export_path`, Bugtraq extraction, `get_recent_activity` |
| **2.0** | Optional syntax highlight, RSS XML, advanced search (TBD) |

---

## Build and run

```bash
./gradlew build
./gradlew bootRun
```

- **MCP endpoint (Streamable HTTP):** `http://localhost:8765/mcp` (default port **8765**, `server.port` in `application.yml`).

### Docker

From the repository root:

```bash
docker compose up --build
```

- **MCP URL:** `http://localhost:8765/mcp`
- Host and container both listen on **8765** (`docker-compose.yml` `ports` and `SERVER_PORT`).

To use another host port, change `ports` to e.g. `"9000:8765"` and keep the container app on **8765** unless you also override `SERVER_PORT` / `application.yml`.
- **Configuration:** `src/main/resources/application.yml`, prefix `io.github.jason07289.svn.mcp` (nested under `io.github.jason07289.svn.mcp` in YAML). Demo credentials can be overridden with `SVN_DEMO_USER` and `SVN_DEMO_PASSWORD`.
- **Implemented today:** `list_repositories` (config only), `list_path`, `get_file`, `get_log`, `get_revision`, `diff_file`, `diff_revision`, `blame_file`, `resolve_revision_range`, `repository_author_stats`, `search_in_path` (SVNKit against each repo `root_url`; credentials stay server-side and are never returned in tool output).

---

## Using `search_in_path`

Use this to find which files reference a given table/keyword and who last touched each one. It walks the files under `path` (BFS), then looks up the last commit for each matching file. All of this file I/O happens inside a **single SVN session**, so it avoids the N+1 connection cost of calling `list_path` + `get_file` per file.

### Parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `repository_id` | yes | — | Configured repository id |
| `path` | yes | — | Base path to search under (e.g. `trunk/com/example/service`) |
| `keyword` | yes | — | Keyword to find in file contents (table name, SQL fragment, …) |
| `revision` | no | HEAD | Revision to read |
| `file_extensions` | no | `java` | Comma-separated extensions to scan (e.g. `java,xml`) |
| `case_sensitive` | no | `false` | Case-sensitive matching |
| `max_files_to_scan` | no | `200` (max 500) | Cap on files whose content is read |
| `max_matches` | no | `50` (max 200) | Stop after this many matches |

### Response

Each match returns `modulePath`, `filePath`, `fileName`, `lastAuthor`, `lastRevision`, `matchCount` (lines containing the keyword), and `matchedLines` (up to 5 sample lines).

### Recommended pattern

To sweep an entire repo, list modules first with `list_path`, then call `search_in_path` per module so progress is incremental and errors are isolated per module.

```text
# 1) list modules
list_path(repository_id="my-repo", path="trunk", view_mode="tree")

# 2) search per module (include xml to catch MyBatis mappers)
search_in_path(repository_id="my-repo", path="trunk/module-a", keyword="TB_ORDERS", file_extensions="java,xml")
search_in_path(repository_id="my-repo", path="trunk/module-b", keyword="TB_ORDERS", file_extensions="java,xml")
```

The LLM can then format the results as `modulePath | file | lastAuthor`:

```text
trunk/module-a/src/main/java/com/example/service | OrderService.java | dev_kim
trunk/module-a/src/main/java/com/example/dao     | OrderDao.java     | dev_lee
trunk/module-b/src/main/resources/mapper         | OrderMapper.xml   | dev_park
```

> Note: searching a wide scope like `path="trunk"` in one call may hit the default BFS depth (6) and `max_files_to_scan` (200) limits. For large trees, split calls per module or raise the limits.

---

## Package name

Application base package: **`io.github.jason07289.svn.mcp`**.
