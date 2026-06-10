# SVN MCP 서버

**SVN MCP**는 **Subversion(SVN)** 저장소를 AI 코딩 어시스턴트(예: Cursor, Claude Desktop)가 쓸 수 있도록 [Model Context Protocol(MCP)](https://modelcontextprotocol.io/)로 노출하는 서버입니다. 클라이언트는 **Streamable HTTP**로 연결하고, SVN 접근은 JVM 안의 **SVNKit**으로 처리합니다. 클라이언트·IDE 호스트에는 **`svn` CLI 설치가 필요 없습니다.**

> *WebSVN이 브라우저에 주던 읽기 흐름을, Streamable HTTP로 호출 가능한 MCP 도구로 제공합니다.*

[English README](README.md) · 상세 요구: [PRD.md](PRD.md) · 코드 구조: [ARCHITECTURE.md](ARCHITECTURE.md) · 시나리오: [기간별 생산량](docs/scenarios/productivity-by-period.md)

---

## SVN MCP를 쓰는 이유

기존 SVN 탐색([WebSVN](https://github.com/websvnphp/websvn) 등)은 브라우저 전제에 가깝습니다. **SVN MCP**는 같은 *종류*의 조회·비교·검색을 MCP 생태계로 가져와, 에이전트가 저장소 목록·경로·파일·이력·diff·검색 등을 **MCP 연결 하나**로 다루게 합니다.

---

## 기능 요약 (현재 구현 MCP 도구)

아래는 `McpServerConfiguration`에 등록된 **`tool`** 이름과 JSON 스키마상 **필수(`required`) 파라미터**입니다. 생략 가능한 인자는 각 도구 설명에 따릅니다.

| 영역 | 도구 | 필수 파라미터 | 동작 요약 |
|------|------|---------------|-----------|
| **저장소** | `list_repositories` | 없음 | 설정된 저장소 ID·이름·루트 URL·그룹 등(자격 증명 미포함). |
| **탐색** | `list_path` | `repository_id` | 리비전에서 디렉터리 한 단계(`view_mode=tree`) 또는 경로 하위 파일 나열(`flat`); 선택: `path`(기본 루트), `revision`, `peg_revision`, `view_mode`, `flat_max_depth`, `flat_max_entries`. |
| **파일** | `get_file` | `repository_id`, `path` | 파일 내용(텍스트 또는 Base64), MIME·텍스트 여부 등; 선택: `revision`, `peg_revision`. |
| **로그** | `get_log` | `repository_id` | 경로에 영향 준 커밋 로그; 선택: `path`, `limit`, `start_revision`/`end_revision`, `stop_on_copy`, `start_date`/`end_date`(ISO-8601), `author`, `author_match`(`exact`\|`contains`). |
| **리비전** | `get_revision` | `repository_id`, `revision` | 단일 리비전 메타데이터·변경 경로. |
| **Diff (파일)** | `diff_file` | `repository_id`, `path`, `from_revision`, `to_revision` | 두 리비전 간 unified diff; 선택: `ignore_whitespace`. |
| **Diff (리비전)** | `diff_revision` | `repository_id`, `revision` | `svn diff -c REV`에 해당; 선택: `path`(접두), `ignore_whitespace`. |
| **Blame** | `blame_file` | `repository_id`, `path` | 줄 단위 blame; 선택: `revision`(annotate 상한, 생략 시 HEAD). |
| **기간→리비전** | `resolve_revision_range` | `repository_id`, `start_inclusive`, `end_inclusive` | 시간 구간을 리비전 범위로 근사 매핑; 선택: `path`. |
| **통계** | `repository_author_stats` | `repository_id` | 기간별 작성자 커밋·diff 라인 집계. 시간 창은 **`calendar_date`**(+선택 `timezone`) **또는** **`start_inclusive`·`end_inclusive`**(ISO-8601) 중 하나로 지정; 선택: `path_prefix`, `max_revisions_to_analyze`. |
| **검색** | `search_in_path` | `repository_id`, `path`, `keyword` | 경로 하위 파일 내용에서 키워드(예: 테이블명)를 찾아 매칭 파일과 **마지막 작성자**를 반환. 모든 파일 읽기를 **단일 SVN 세션**으로 처리해 N+1 연결 비용을 회피. 선택: `revision`, `file_extensions`(쉼표 구분, 기본 `java`), `case_sensitive`(기본 false), `max_files_to_scan`(기본 200, 최대 500), `max_matches`(기본 50, 최대 200). |

PRD·로드맵에만 있는 **`get_recent_activity`**, **`export_path`**, **`diff_paths`**, 선택 **`authz`** 등은 아직 MCP 도구로 노출되지 않습니다. 자격 증명은 서버 설정(env·시크릿)에만 두고 응답에 포함하지 않습니다.

**포함:** MCP 프로토콜, Streamable HTTP, 위 읽기 중심 SVN 작업, 다중 저장소, 선택적 경로 권한.

**제외·후순위:** WebSVN PHP UI 재현, **쓰기**(commit 등), 외부 공개 RSS 피드 호스팅(에이전트는 `get_recent_activity` 등으로 대체).

---

## `search_in_path` 사용법

특정 테이블·키워드에 접근하는 파일을 찾고 **마지막 작업자**까지 한 번에 확인할 때 사용합니다. 내부적으로 경로 하위를 BFS로 훑은 뒤, 매칭된 파일마다 마지막 커밋 로그를 조회합니다. 이 모든 파일 I/O가 **하나의 SVN 세션** 안에서 수행되므로, `list_path` + `get_file`을 파일마다 호출하는 방식의 N+1 연결 비용이 발생하지 않습니다.

### 파라미터

| 파라미터 | 필수 | 기본값 | 설명 |
|----------|------|--------|------|
| `repository_id` | ✅ | — | 설정된 저장소 ID |
| `path` | ✅ | — | 탐색 시작 경로 (예: `trunk/com/example/service`) |
| `keyword` | ✅ | — | 파일 내용에서 찾을 키워드 (테이블명, SQL 조각 등) |
| `revision` | — | HEAD | 조회할 리비전 |
| `file_extensions` | — | `java` | 스캔 대상 확장자(쉼표 구분, 예: `java,xml`) |
| `case_sensitive` | — | `false` | 대소문자 구분 여부 |
| `max_files_to_scan` | — | `200` (최대 500) | 내용을 읽을 파일 수 상한 |
| `max_matches` | — | `50` (최대 200) | 수집할 매칭 결과 수 상한 |

### 응답 형태

매칭된 파일마다 `modulePath`(상위 디렉터리), `filePath`(전체 경로), `fileName`, `lastAuthor`, `lastRevision`, `matchCount`(키워드 등장 라인 수), `matchedLines`(샘플 최대 5줄)를 반환합니다.

### 권장 호출 패턴

저장소 전체를 훑을 때는 `list_path`로 모듈 목록을 먼저 얻은 뒤, 모듈별로 `search_in_path`를 호출하면 진행 상황을 단계적으로 보여주고 에러도 모듈 단위로 격리됩니다.

```text
# 1) 모듈 목록
list_path(repository_id="my-repo", path="trunk", view_mode="tree")

# 2) 모듈별 검색 (MyBatis mapper까지 보려면 xml 포함)
search_in_path(repository_id="my-repo", path="trunk/module-a", keyword="TB_ORDERS", file_extensions="java,xml")
search_in_path(repository_id="my-repo", path="trunk/module-b", keyword="TB_ORDERS", file_extensions="java,xml")
```

LLM이 응답을 받아 다음과 같이 `모듈경로 | 파일 | 마지막작업자` 형식으로 정리할 수 있습니다.

```text
trunk/module-a/src/main/java/com/example/service | OrderService.java | dev_kim
trunk/module-a/src/main/java/com/example/dao     | OrderDao.java     | dev_lee
trunk/module-b/src/main/resources/mapper         | OrderMapper.xml   | dev_park
```

> 참고: `path="trunk"`처럼 넓은 범위를 한 번에 검색하면 BFS 깊이 기본값(6)과 `max_files_to_scan`(기본 200) 제한에 걸릴 수 있습니다. 규모가 크면 모듈 단위로 나눠 호출하거나 한도를 높이세요.

---

## 기술 스택

| 구분 | 선택 |
|------|------|
| 런타임 | Java **17** |
| 앱 | **Spring Boot** |
| MCP 전송 | **Streamable HTTP** |
| SVN | **SVNKit**(순수 Java, 프로토콜은 SVNKit 지원 범위) |

---

## 로드맵 (요약)

| 단계 | 내용 |
|------|------|
| **MVP** | `list_repositories`, `list_path`, `get_file`, `get_log`, `get_revision`, `diff_file`, `blame_file`, 기본 오류 |
| **1.1** | `diff_paths`, 제한적 `search`, authz 가드, 응답 한도 강화 |
| **1.2** | `export_path`, Bugtraq 추출, `get_recent_activity` |
| **2.0** | 선택 구문 하이라이트, RSS XML, 고급 검색(검토) |

---

## 빌드·실행

```bash
./gradlew build
./gradlew bootRun
```

- **MCP 엔드포인트(Streamable HTTP):** `http://localhost:8765/mcp` (기본 포트 **8765**, `application.yml`의 `server.port`).

### Docker

저장소 루트에서 이미지를 빌드하고 컨테이너를 띄웁니다. 호스트·컨테이너 모두 **8765**를 사용합니다.

```bash
docker compose up --build
```

- **MCP URL:** `http://localhost:8765/mcp`
- 포트를 바꾸려면 `docker-compose.yml`의 `ports`를 `"원하는호스트포트:8765"`처럼 조정하고, 컨테이너 쪽 앱 포트는 `SERVER_PORT` 또는 `application.yml`로 맞춥니다.
- **설정:** `src/main/resources/application.yml`, 프로퍼티 접두사 `io.github.jason07289.svn.mcp`(YAML에서는 `io` → `github` → `jason07289` → … 중첩). 데모 자격 증명은 `SVN_DEMO_USER`, `SVN_DEMO_PASSWORD`로 덮어쓸 수 있습니다.
- **현재 구현:** 상단 기능 요약 표에 나열한 MCP 도구(SVNKit 기반 읽기 전용).

---

## 패키지

애플리케이션 기본 패키지: **`io.github.jason07289.svn.mcp`**.
