# PRD: SVN MCP (Spring Boot / Java 17)

## 1. 개요

### 1.1 목적

Subversion(SVN) 저장소를 Model Context Protocol(MCP) 클라이언트(예: Cursor, Claude Desktop)에서 조회·비교·검색할 수 있도록, **Java 17 및 Spring Boot 기반 MCP 서버**를 제공한다. **AI 코딩 도구와의 MCP 연결 방식은 Streamable HTTP**를 사용한다. 초기 **읽기 중심 기능 범위**(저장소 탐색, 로그, diff 등)를 정할 때 [WebSVN](https://github.com/websvnphp/websvn) 같은 브라우저 SVN 뷰어를 **참고**했으며, 본 제품은 웹 UI를 재현하지 않고 MCP `tool`로 노출한다.

### 1.2 참고 자료

- SVN 저장소 연동: **SVNKit**(Subversion용 순수 Java 라이브러리). 구현은 Spring Boot 애플리케이션과 **MCP 서버 프로세스 내부의 SVN 접근 계층**에서 수행한다. (상세는 [3. 아키텍처 원칙](#3-아키텍처-원칙) 참고.)

### 1.3 제품 한 줄 정의

> “SVN 저장소를 Streamable HTTP MCP로 노출해, AI 에이전트가 `svn` CLI 없이 읽기·분석 작업을 수행할 수 있게 하는 게이트웨이.”

---

## 2. 범위

### 2.1 포함 (In Scope)

- MCP 서버 프로토콜(도구 등록, 호출, 오류 응답) 준수
- Spring Boot 애플리케이션으로 **MCP Streamable HTTP** 전송(스펙에 맞는 엔드포인트·세션) — AI 코딩 도구와의 **기본·권장** 연결 방식
- [4. 기능 요구사항](#4-기능-요구사항)에 정의된 SVN 읽기·분석 작업
- **SVNKit**을 통한 저장소 접근(네이티브 `svn` CLI 의존 없이 JVM 내에서 프로토콜·인증 처리; 지원 범위는 SVNKit·배포 JDK 정책에 따름)
- 다중 저장소 구성, 경로별 읽기 권한(선택적 `svnauthz` 등)

### 2.2 제외 또는 후순위 (선택)

- 브라우저용 SVN 뷰어 수준의 풍부한 웹 UI·템플릿 재현
- 서브버전 **쓰기** 작업(commit, propset 등) — 본 PRD는 **읽기 중심**으로 정의(별도 요청 시 확장)
- RSS를 외부 공개 피드로 호스팅하는 것(에이전트용으로는 “최근 변경 요약” 도구로 대체 가능)

---

## 3. 아키텍처 원칙

### 3.1 클라이언트 vs MCP 서버(SVN 접근)

| 구분 | 요구 |
|------|------|
| **AI 코딩 도구(IDE) 호스트** | **Subversion CLI 설치를 요구하지 않는다.** MCP 클라이언트는 Streamable HTTP로 본 MCP 서버만 연결하면 된다. |
| **MCP 서버(본 제품 `svn-mcp`)** | **SVN 저장소 접근은 SVNKit(Java)으로 수행한다.** 배포물(예: 실행 가능 JAR, 컨테이너 이미지)에 **Java 17 런타임과 SVNKit**이 포함되며, 이 MCP 프로세스가 설정된 URL로 SVN 서버에 붙는다. `https://` / `svn://` / `svn+ssh://` 등 프로토콜·인증은 **SVNKit이 지원하는 범위** 내에서 처리한다. |
| **운영 전제** | **사용자도, IDE 호스트도, 별도로 OS에 `svn` 명령을 깔아 두는 것을 요구하지 않는다.** 저장소 클라이언트 역할은 **SVNKit + JVM**으로 충족한다. |

### 3.2 MCP 전송

| 원칙 | 설명 |
|------|------|
| **Streamable HTTP** | AI 코딩 도구와의 MCP 통신은 **Streamable HTTP**로 수행한다. |

### 3.3 기타

| 원칙 | 설명 |
|------|------|
| **동작 의미 정렬** | 일반적인 SVN 클라이언트·CLI와 동일한 **의미**의 결과를 내도록 맞출 것; 내부 구현은 SVNKit API 사용. |
| **상태 비저장 우선** | 도구 호출 단위로 저장소 ID·리비전·경로를 인자로 받음(MCP 세션은 전송·인증·레이트리밋용). |
| **대용량 응답 제어** | diff/로그/파일 본문은 크기 상한, 페이지네이션, 요약 모드 제공. |
| **보안** | 저장소 자격 증명은 서버 측 환경 변수 또는 시크릿 저장소; 로그에 비밀번호·토큰 금지. |

---

## 4. 기능 요구사항

MCP `tool` 이름은 **snake_case**로 일관되게 정한다.

### 4.1 기능 범위 (요약)

| 영역 | 기대 동작 |
|------|-----------|
| 저장소 | 설정된 다중 저장소 나열; 필요 시 부모 SVN URL로 하위 엔트리 **라이브 탐색** |
| 탐색 | 리비전 기준 디렉터리·파일 목록(트리/플랫 등) |
| 파일 | 특정 리비전의 파일 내용(텍스트·바이너리), MIME·용량 상한 |
| 이력 | 경로별 커밋 로그, 단일 리비전 상세·변경 경로 |
| 비교 | 파일 두 리비전 간 unified diff, 단일 리비전 변경분(`svn diff -c`에 해당), 공백 무시 옵션 |
| blame | 줄별 리비전·작성자 |
| 보조 | 시간 구간을 리비전에 근사 매핑, 기간별 작성자 통계 등(에이전트 분석용) |
| 보안 | 서버 측 자격 증명; (선택) `authz` 경로별 읽기 제한 — **가드는 로드맵에서 구현** |

선택적 **구문 하이라이트**, **경로 간 diff**, **전체 검색**, **export/아카이브**, **RSS형 최근 활동 JSON** 등은 [7. 로드맵](#7-단계별-로드맵-제안)에서 후순위로 둔다.

### 4.2 구현 현황 (코드베이스 기준, 2026-03)

| 구분 | 설명 |
|------|------|
| **전송** | Spring Boot + MCP Java SDK, **Streamable HTTP**, 엔드포인트 경로 `/mcp`(기본값). |
| **SVN 접근** | **SVNKit** (`SvnRepositoryOperations` / `SvnKitRepositoryOperations`). 클라이언트·IDE에 `svn` CLI 불필요. |
| **설정** | `application.yml` 프리픽스 `io.github.jason07289.svn.mcp` — 저장소 목록, `authz_file`, `defaults`(로그 한도·파일 최대 바이트·통계용 리비전 상한). `authz_file`은 프로퍼티만 존재하며 **경로별 읽기 가드는 아직 미구현**. |

**등록된 MCP 도구(구현됨)**

아래 **프롬프트 예시**는 사용자가 에이전트에게 말하거나, 에이전트가 도구 선택 시 의도를 잡는 **자연어 참고**다. 실제 MCP 호출은 각 도구의 JSON 스키마 인자로 이루어진다.

| 도구명 | 역할 | 주요 인자·동작 | 프롬프트 예시 |
|--------|------|----------------|---------------|
| `list_repositories` | `application.yml`에 정의된 저장소 목록, 또는 부모 SVN URL의 **자식 엔트리** 1단계 나열(라이브 탐색) | 인자 없음 또는 `server_url`, 선택 `credential_repository_id` / `username`·`password`. 단일 저장소 루트면 `trunk` 등이 나올 수 있음. | 「설정에 등록된 SVN 저장소 id와 이름을 알려줘.」<br>「`svn://svn.example.com:3690/` 바로 아래에 어떤 저장소(또는 디렉터리)가 있는지 목록만 뽑아 줘.」 |
| `list_path` | 지정 리비전에서 디렉터리 **한 단계(tree)** 또는 경로 아래 **파일 나열(flat)** | `repository_id`, `path`(빈 문자열=루트), `revision`, `view_mode`(`tree`\|`flat`), `flat_max_depth`, `flat_max_entries`. `peg_revision`은 예약. | 「`my-repo` 루트에서 HEAD 기준으로 폴더만 한 단계 보여줘.」<br>「`trunk/src` 아래에 있는 파일을 flat으로, 깊이 3까지 최대 100개만.」 |
| `get_file` | 한 파일의 내용(텍스트 또는 Base64); 바이너리·MIME·용량 상한 | `repository_id`, `path`, 선택 `revision`(생략 시 HEAD). | 「`my-repo`의 `trunk/README.md` 내용을 HEAD에서 가져와.」<br>「리비전 1200에서 `conf/app.yml` 원문을 보여줘.」 |
| `get_log` | 경로에 영향을 준 커밋 로그(최신순), 변경 경로 포함 | `repository_id`, `path`, `limit`, `start_revision`/`end_revision`, `stop_on_copy`, 또는 `start_date`/`end_date`(ISO-8601), `author`, `author_match`(`exact`\|`contains`). | 「`trunk` 아래 최근 30개 커밋 로그만.」<br>「2025-01-01부터 2025-01-31까지 커밋한 것만, 작성자가 `kim`인 항목 위주로.」 |
| `get_revision` | **한 리비전**의 메타데이터와 변경된 경로 목록 | `repository_id`, `revision`. | 「리비전 4521에서 어떤 파일이 추가·수정·삭제됐는지 요약해 줘.」 |
| `diff_file` | **한 파일** 두 리비전 사이 unified diff | `repository_id`, `path`, `from_revision`, `to_revision`, `ignore_whitespace`. | 「`src/Main.java`가 리비전 100과 105 사이에 어떻게 바뀌었는지 diff로 보여줘.」<br>「공백 무시하고 같은 파일 diff.」 |
| `blame_file` | 줄마다 마지막 변경 리비전·작성자 | `repository_id`, `path`, 선택 `revision`(annotate 상한). | 「`lib/utils.py` blame을 HEAD 기준으로 줄 단위로.」 |
| `resolve_revision_range` | ISO 시각 구간을 **대략적인** 리비전 번호로 매핑(`getDatedRevision`) | `repository_id`, 선택 `path`, `start_inclusive`, `end_inclusive`(ISO-8601). 엄격한 시각 경계는 `get_log` 결과의 커밋 시각을 사용. | 「2025-03-01 00:00부터 2025-03-01 23:59:59까지의 커밋이 걸릴 만한 리비전 범위를 대략 알려줘.」 |
| `diff_revision` | **한 리비전**의 변경분(`svn diff -c REV`에 해당), 선택적 경로 접두사 | `repository_id`, `revision`, 선택 `path`(빈 문자열=전체 트리), `ignore_whitespace`. | 「리비전 500에서 들어온 변경 전체 diff를 보여줘.」<br>「`trunk/module/` 아래만 리비전 500의 diff.」 |
| `repository_author_stats` | 기간 내 **작성자별** 커밋 수·diff 가감 라인 합(상한 있음) | `repository_id`, `path_prefix`, 또는 `calendar_date`+`timezone`, 또는 `start_inclusive`/`end_inclusive`, `max_revisions_to_analyze`. | 「어제 하루 동안 저장소 전체에서 작성자별로 커밋이랑 변경 라인 양을 집계해 줘.」<br>「`trunk/app`만 보고 2025-03-01~2025-03-07 기간 통계.」 |

**아직 미구현([7. 로드맵](#7-단계별-로드맵-제안)과 동일하게 후순위)**

- `diff_paths`, `search`, `get_recent_activity`, `export_path`
- `authz` 기반 **도구 공통 권한 가드**(설정 키만 존재)
- Bugtraq 이슈 ID 추출(저장소별 `bugtraq.log_regex`는 프로퍼티에 존재, 도구 응답 연동은 미구현으로 둘 수 있음)
- (선택) `repo_last_activity`, 서버 측 구문 하이라이트

---

## 5. 비기능 요구사항

| ID | 항목 | 기준 |
|----|------|------|
| NFR-1 | **성능** | 단일 `get_log`/`diff` 호출에 타임아웃(설정 가능); 기본 limit 상한 |
| NFR-2 | **안정성** | SVNKit·네트워크 오류 시 안전한 오류 메시지(내부 스택 노출 최소화) |
| NFR-3 | **관측 가능성** | 구조화 로그(저장소 ID, 도구명, 소요 시간); 비밀번호/토큰 마스킹 |
| NFR-4 | **배포** | Spring Boot 실행 JAR(또는 컨테이너)로 **Streamable HTTP MCP** 엔드포인트 노출; 클라이언트는 별도 `svn` CLI 설치 불필요함을 문서에 명시 |
| NFR-5 | **테스트** | 핵심 도구·리졸버·SVNKit 통합에 대해 **JUnit**으로 단위·통합 테스트 존재(로컬 샘플 저장소 등). 지속 추가 권장. |

---

## 6. 설정 모델 (초안)

### 6.1 요구사항

- **Spring Boot `application.yml`**(및 `application-{profile}.yml`)로 **SVN 서버·저장소 기본 정보**를 선언할 수 있어야 한다. 여기에 포함되는 항목(예시)은 다음과 같다.
  - 저장소 **식별자**(`id`), **표시 이름**, **SVN 루트 URL**(`root_url` 등)
  - **접속 자격 증명**(사용자명·비밀번호 등 SVNKit이 지원하는 형태) — 가능하면 **환경 변수·시크릿 참조**로만 주입하고, 평문을 저장소에 커밋하지 않도록 한다.
  - (선택) 그룹, `authz` 파일 경로, Bugtraq 정규식, 응답 한도(`defaults`: `log_limit_max`, `file_content_max_bytes`, `max_revisions_for_stats` 등) 등
- 동일 내용은 **환경 변수**로 덮어쓰거나, 필요 시 **별도 YAML 경로**를 가리키는 방식으로도 로드할 수 있게 한다(Spring Boot 외부 설정 관례).
- **SVN 접근은 SVNKit 전제**이므로 `SVN_BIN` 같은 CLI 경로는 본 제품의 기본 설계에 포함하지 않는다(필요 시 레거시·진단용 옵션으로만 검토).

### 6.2 예시 (`application.yml`)

프리픽스·프로퍼티 이름은 구현 시 `@ConfigurationProperties`와 맞춘다. 아래는 구조만 나타낸 예시다.

```yaml
# application.yml (예시)
io:
  github:
    jason07289:
      svn:
        mcp:
          repositories:
            - id: myproj
              name: My Project
              root_url: https://svn.example.com/myproj
              group: products
              credentials:
                username: ${SVN_MYPROJ_USER:}
                password: ${SVN_MYPROJ_PASSWORD:}
              bugtraq:
                log_regex: "ISSUE-(\\d+)"
          authz_file: /path/to/authz
          defaults:
            log_limit_max: 500
            file_content_max_bytes: 2_000_000
            max_revisions_for_stats: 500
```

`max_revisions_for_stats`는 `repository_author_stats` 등에서 diff 기반 통계를 계산할 때 분석할 리비전 수 상한으로 사용한다.

---

## 7. 단계별 로드맵 (제안)

| Phase | 내용 |
|-------|------|
| **MVP (달성)** | `list_repositories`(설정 + 선택적 `server_url` 라이브 탐색), `list_path`, `get_file`, `get_log`(날짜·작성자 필터 포함), `get_revision`, `diff_file`, `blame_file`, `resolve_revision_range`, `diff_revision`, `repository_author_stats`, SVNKit 기반 오류 처리(`SvnAccessException` 등) |
| **1.1** | `diff_paths`, `search`(제한적), **`authz` 가드 구현**, 응답 크기 제한·관측 로그 강화, (선택) `repo_last_activity` |
| **1.2** | `export_path`, Bugtraq 추출(도구 응답 연동), `get_recent_activity` |
| **2.0** | 선택적 구문 하이라이트, RSS XML, 고급 검색(인덱스/외부 도구 연동 검토) |

---

## 8. 성공 지표

- **읽기·분석 흐름**을 MCP 도구만으로 재현 가능할 것(에이전트 데모 시나리오). **현재** [4.2절 구현 현황](#42-구현-현황-코드베이스-기준-2026-03)에 열거된 도구로 브라우징·로그·리비전·파일·diff·blame·기간 통계·(선택) 서버 URL 탐색까지 검증 가능.
- **IDE/에이전트 호스트에 Subversion CLI를 설치하지 않아도** Streamable HTTP로 MCP만 연결해 동일 기능을 쓸 수 있을 것.
- MCP 서버가 네트워크·자격 증명이 허용되는 한 **SVN 서버에 직접 접근(SVNKit)** 해 도구가 성공할 것.
- 권한이 없는 경로는 **일관되게 거부**될 것.
- 대용량 diff/로그에서도 **서버·클라이언트가 멈추지 않도록** 기본 한도가 동작할 것.

---

## 9. 용어

| 용어 | 설명 |
|------|------|
| **Peg revision** | 경로 이동(rename) 이력이 있을 때 기준이 되는 리비전 |
| **MCP tool** | MCP 스펙에 따라 노출되는 callable 기능 단위 |
| **Streamable HTTP** | MCP 원격 연결용 전송 방식(본 PRD에서 AI 코딩 도구 연결의 표준) |
| **SVNKit** | Subversion과 연동하기 위한 순수 Java 오픈 소스 라이브러리; 본 제품의 저장소 접근 계층으로 사용 |

---

## 10. 오픈 이슈 (결정 필요)

1. **SVNKit 버전·지원 프로토콜**: `svn+ssh`·클라이언트 인증서 등 운영 환경에서 필요한 연결 방식이 SVNKit·선택한 트랜스포트에서 모두 가능한지 검증.  
2. 바이너리/대용량 파일: Base64 크기 한도 및 “다운로드 URL만 반환” 정책.  
3. 검색: 전수 스캔 허용 여부(서버 부하 정책).  
4. 쓰기 작업 범위를 이후 스프린트에 포함할지 여부.  

---

*문서 버전: 0.7 — 4.2절 구현 도구 표에 주요 인자·프롬프트 예시(자연어) 열 추가*
