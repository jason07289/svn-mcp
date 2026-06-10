# Architecture: io.github.jason07289.svn.mcp

본 문서는 코드베이스의 **패키지 루트·계층 분리·의존 규칙**을 정의한다. 구현 세부(클래스명·엔드포인트 경로)는 진행에 따라 조정할 수 있으나, **SVN 연동**과 **MCP(Streamable HTTP) 연동**의 패키지 경계는 유지하는 것을 원칙으로 한다.

---

## 1. 기본 패키지 루트

모든 애플리케이션 코드의 기본 네임스페이스는 다음과 같다.

```text
io.github.jason07289.svn.mcp
```

루트가 이미 `…mcp`로 끝나므로, 하위에 **`io.github.jason07289.svn.mcp.mcp`** 와 같이 `mcp`를 한 번 더 두지 않는다. MCP 전송·tool 계층은 **`transport`**, **`tool`**(및 필요 시 **`protocol`**) 등으로 나눈다.

---

## 2. 상위 구조 (한 눈에)

| 영역 | 패키지 | 책임 |
|------|--------|------|
| 애플리케이션 부트스트랩 | `io.github.jason07289.svn.mcp` | `@SpringBootApplication`, `main` |
| 설정 | `io.github.jason07289.svn.mcp.config` | Spring `@Configuration`, `@ConfigurationProperties`, 프로파일 |
| **SVN 실제 연동** | `io.github.jason07289.svn.mcp.svn` | SVNKit 기반 저장소 접근, 인증·연결, 도메인/유스케이스에 맞는 API |
| **MCP 연동** | `io.github.jason07289.svn.mcp.transport`, `io.github.jason07289.svn.mcp.tool` | Streamable HTTP, MCP 세션/전송, tool 등록·호출 라우팅 |

선택적으로 공통 타입만 분리할 때는 아래를 둘 수 있다(필요 시 도입).

| 영역 | 패키지 | 책임 |
|------|--------|------|
| 공통 | `io.github.jason07289.svn.mcp.common` | 여러 계층에서 쓰는 예외·유틸·소량 공유 타입(과도한 “공통” 축적은 지양) |

---

## 3. 패키지 트리 (초안)

의미 단위로 하위 패키지를 나눈다. 이름은 구현 시 클래스 배치에 맞게 미세 조정 가능하다.

```text
io.github.jason07289.svn.mcp
├── SvnMcpApplication.java           # Spring Boot 진입점
│
├── config
│   ├── …                             # 빈 조립, Properties, 보안·타임아웃 등
│   └── …
│
├── svn                               # ★ Subversion 연동 (SVNKit)
│   ├── api                           # transport/tool이 호출하는 **포트** (인터페이스)
│   │   └── …                         # 예: RepositoryClient, LogQuery, DiffRequest 등
│   ├── model                         # (선택) 저장소 도메인 값 객체, 경로·리비전 표현
│   ├── service                       # (선택) api 구현을 조합하는 애플리케이션 서비스
│   └── svnkit                        # SVNKit 전용 구현·어댑터
│       └── …                         # 인증, WC 없는 원격 접근, SVNKit 예외 → 도메인 예외 매핑
│
├── transport                         # ★ MCP Streamable HTTP (엔드포인트, 스트리밍·세션)
│   └── …
├── protocol                          # (선택) MCP 메시지 직렬화·라우팅 헬퍼
└── tool                              # ★ tool 이름 ↔ svn.api 호출 매핑, 요청/응답 DTO
    └── …
```

### 3.1 `io.github.jason07289.svn.mcp.svn` (SVN 실제 연동)

- **역할**: SVN 서버와의 통신, 인증, 리비전·경로·로그·diff·blame 등 **저장소 작업의 실체**.
- **기술**: SVNKit을 **이 패키지 트리 안**에서 캡슐화한다(특히 `svn.svnkit`).
- **대외 계약**: `svn.api`에 두는 인터페이스는 **MCP에 특화되지 않게** 유지한다. 동일 API를 나중에 REST·배치 등이 재사용할 수 있게 한다.

### 3.2 `io.github.jason07289.svn.mcp.transport` · `io.github.jason07289.svn.mcp.tool` (MCP 연동)

- **역할**: AI 도구와의 **Streamable HTTP** MCP 전송, tool 목록·호출 처리.
- **경계**: 저장소 I/O는 직접 하지 않고, **`io.github.jason07289.svn.mcp.svn.api`** 를 호출한다.

### 3.3 `io.github.jason07289.svn.mcp` · `io.github.jason07289.svn.mcp.config`

- **역할**: 애플리케이션 기동, 전역 설정, 빈 등록(필요 시 `svn` 구현체를 `api`에 연결).
- **저장소·SVN 서버 설정**: 루트 URL·계정 등 **SVN 접속에 필요한 기본 정보**는 Spring Boot **`application.yml`**(프로파일·환경 변수 포함) 등 외부 설정으로 두고, `@ConfigurationProperties` 등으로 이 패키지에서 바인딩한다. 필드·키 정의는 [PRD.md](./PRD.md)의 설정 모델과 맞춘다.
- **원칙**: 도메인 로직은 `svn`·`transport`·`tool`에 두고, `config`는 **연결과 바인딩** 위주로 유지한다.

---

## 4. 모듈(Gradle/Maven)과의 대응 (선택)

초기에는 **단일 모듈**(단일 `jar`) 안에 위 패키지를 모두 두어도 된다. 경계가 커지면 예를 들어 다음처럼 분리할 수 있다.

| 모듈 | 포함 패키지 (예시) |
|------|---------------------|
| `svn-mcp-svn` | `io.github.jason07289.svn.mcp.svn` |
| `svn-mcp-mcp` | `io.github.jason07289.svn.mcp.transport`, `io.github.jason07289.svn.mcp.tool`, (선택) `io.github.jason07289.svn.mcp.protocol` |
| `svn-mcp-app` | `io.github.jason07289.svn.mcp`, `io.github.jason07289.svn.mcp.config` + Spring Boot 플러그인 |

의존 방향: **`app` → `svn-mcp-mcp`, `svn-mcp-svn`**, **`transport`/`tool` → `svn`(api 중심)**. `svn`은 `transport`·`tool`을 **참조하지 않는다.**

---

## 5. 의존 규칙 (필수)

1. **`io.github.jason07289.svn.mcp.transport` / `io.github.jason07289.svn.mcp.tool` → `io.github.jason07289.svn.mcp.svn.api` (및 필요 시 `svn.model`)**  
   MCP 계층은 SVN 구현 세부(SVNKit 타입)에 직접 의존하지 않는 것을 권장한다.

2. **`io.github.jason07289.svn.mcp.svn.svnkit` → SVNKit**  
   SVNKit 타입은 가능한 한 `svnkit` 패키지 내부로 가둔다.

3. **`io.github.jason07289.svn.mcp.svn` ↛ `io.github.jason07289.svn.mcp.transport` · `io.github.jason07289.svn.mcp.tool`**  
   SVN 계층은 MCP·HTTP에 대해 알지 않는다.

4. **`config`**  
   모든 계층의 빈을 조립할 수 있으나, **비즈니스 규칙**은 `svn` / `transport` / `tool`에 둔다.

---

## 6. PRD와의 정렬

기능·비기능 요구는 [PRD.md](./PRD.md)를 따른다. 본 문서는 **코드 구조**만 규정하며, MCP tool 이름·엔드포인트 경로는 구현 단계에서 PRD의 도구 매핑과 함께 확정한다.

---

*문서 버전: 1.2 — 패키지 루트 `io.github.jason07289.svn.mcp`, `svn` / `transport`·`tool` 분리; `config` ↔ `application.yml` 저장소 바인딩*
