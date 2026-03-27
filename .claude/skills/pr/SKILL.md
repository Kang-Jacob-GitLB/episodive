---
name: pr
description: 현재 브랜치를 push하고 PR을 생성한다. 사용자가 "/pr"을 입력하거나 "PR 만들어줘", "풀리퀘스트 생성", "PR 올려줘" 등을 요청할 때 사용한다.
disable-model-invocation: true
allowed-tools: Bash(git status:*), Bash(git log:*), Bash(git branch:*), Bash(git diff:*), Bash(git push:*), Bash(gh pr create:*), Bash(gh pr view:*), Bash(gh pr edit:*)
---

## 현재 상태

- 현재 브랜치: !`git branch --show-current`
- main과의 커밋 목록: !`git log main..HEAD --oneline`
- main과의 변경 diff: !`git diff main..HEAD`
- 이미 PR 존재 여부: !`gh pr view 2>&1 || echo "PR 없음"`

## 작업 지침

### 1. 사전 확인

- 현재 브랜치가 `main` 또는 `master`이면 중단하고 사용자에게 알린다.
- main과의 커밋이 없으면 중단하고 사용자에게 알린다.
- 이미 PR이 열려 있으면 **업데이트 모드**로 전환한다 (아래 6단계 참고).

### 2. Push

```bash
git push -u origin HEAD
```

### 3. PR 제목 작성

브랜치 이름과 커밋 목록을 바탕으로 PR 제목을 작성한다.

**형식:** `{타입}: {한글 제목}`

**타입 선택 기준:**
- `feat` — 새 기능 추가
- `fix` — 버그 수정
- `refactor` — 리팩토링 (기능 변경 없음)
- `test` — 테스트 추가/수정
- `docs` — 문서 변경
- `chore` — 빌드 설정, 의존성, 기타 잡무
- `ci` — CI/CD 관련

**제목 규칙:**
- 한글로 작성 (기술 용어·고유명사는 영문 유지)
- 70자 이내
- 명령형 (~추가, ~수정, ~제거, ~개선)
- 마침표 없음

### 4. PR 본문 작성

커밋 목록과 diff를 분석해 본문을 작성한다.

**형식:**
```markdown
## 변경 사항
- {변경 내용 항목1}
- {변경 내용 항목2}

## 테스트
{테스트 방법 또는 확인 사항. 해당 없으면 생략}
```

- 한글로 작성 (기술 용어·고유명사는 영문 유지)
- 변경이 단순하면 본문 전체 생략 가능

### 5. PR 생성

타입에 따라 라벨을 매핑한다:
- `feat` → `feature`
- `fix` → `bugfix`
- `refactor` → `refactoring`
- `test` → `test`
- `docs` → `documentation`
- `chore` → `chore`
- `ci` → `ci/cd`

```bash
gh pr create --title "{타입}: {제목}" --assignee "alsrb968" --label "{라벨}" --body "$(cat <<'EOF'
{본문}
EOF
)"
```

### 6. PR 업데이트 (이미 PR이 열려 있는 경우)

push 후 전체 커밋 목록과 diff를 재분석하여 제목과 본문을 업데이트한다.

```bash
gh pr edit --title "{타입}: {제목}" --body "$(cat <<'EOF'
{본문}
EOF
)"
```

### 7. 완료 보고

PR URL을 출력한다.
