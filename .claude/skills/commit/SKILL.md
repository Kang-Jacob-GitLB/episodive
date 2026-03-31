---
name: commit
description: 변경 사항을 staging하고 커밋한다. 사용자가 "/commit"을 입력하거나 "커밋해줘", "변경사항 커밋", "commit the changes" 등을 요청할 때 사용한다.
disable-model-invocation: true
allowed-tools: Bash, Read
---

## 현재 상태

- 현재 브랜치: !`git branch --show-current`
- Git 상태: !`git status`
- 변경 사항 diff: !`git diff HEAD`
- 최근 커밋: !`git log --oneline -5`

## 작업 지침

위의 변경 사항을 분석하여 아래 절차를 순서대로 수행한다.

### 1. 보안 검토

커밋하기 전에 아래 패턴이 포함된 파일이 스테이징 대상에 포함되는지 확인한다:
- `.env`, `.env.*` (`.env.example` 제외)
- API 키, 비밀 키, 토큰, 패스워드가 하드코딩된 코드
- `google-services.json`, `keystore.*`, `*.jks`, `*.keystore`
- `local.properties` (SDK 경로 외 민감 정보 포함 시)

보안상 위험한 파일이 있으면 **해당 파일을 제외하고** 사용자에게 반드시 알린다.

### 2. 브랜치 처리

현재 브랜치가 `main` 또는 `master`이면, 변경 사항의 성격에 맞는 새 브랜치를 생성한다.

**브랜치 접두사 선택 기준:**
- `feat/` — 새 기능 추가
- `fix/` — 버그 수정
- `refactor/` — 리팩토링 (기능 변경 없음)
- `test/` — 테스트 추가/수정
- `docs/` — 문서 변경
- `chore/` — 빌드 설정, 의존성, 기타 잡무
- `ci/` — CI/CD 관련

**브랜치 이름 형식:** `{접두사}/{변경내용을-하이픈으로-이은-영문슬러그}`

예시:
- `feat/add-last-play`
- `fix/playback-list-crash`
- `refactor/split-player-module`

브랜치 생성 명령: `git checkout -b {브랜치명}`

### 3. 파일 스테이징

아래 파일 유형을 **제외**하고 모든 변경 파일을 스테이징한다:
- 보안 검토에서 식별된 민감 파일
- `*.class`, `*.apk`, `*.aab` 빌드 산출물

변경 파일 각각을 `git add {파일경로}` 로 명시적으로 스테이징한다. `git add -A` 나 `git add .` 는 사용하지 않는다.

### 4. 커밋 메시지 작성

여러 커밋을 나중에 squash하므로, 커밋 제목에 타입 접두사(`feat:`, `fix:` 등)를 붙이지 않는다.

**형식:**
```
{한글 제목}

{본문 (선택)}
```

**제목 규칙:**
- 한글로 작성 (기술 용어·고유명사는 영문 유지)
- 50자 이내
- 명령형 (~추가, ~수정, ~제거, ~개선)
- 마침표 없음

**본문 규칙 (생략 가능):**
- 변경이 단순하면 본문 생략
- 본문이 필요한 경우: 왜 변경했는지, 무엇이 달라졌는지 서술
- 제목과 한 줄 공백으로 구분

**커밋 명령:**
```bash
git commit -m "$(cat <<'EOF'
{제목}

{본문 (있는 경우만)}
EOF
)"
```

### 5. 완료 보고

커밋 완료 후 다음을 간략히 알린다:
- 생성된 브랜치 (main에서 분기한 경우)
- 커밋 메시지 요약
- 스테이징에서 제외된 파일 (있는 경우)
