---
name: cleanup
description: PR 머지 후 main으로 돌아가 pull, fetch --prune하고 삭제된 remote 브랜치의 로컬 브랜치를 정리한다. 사용자가 "/cleanup"을 입력하거나 "머지 후 정리", "브랜치 정리해줘" 등을 요청할 때 사용한다.
disable-model-invocation: true
allowed-tools: Bash
---

## 현재 상태

- 현재 브랜치: !`git branch --show-current`
- 워킹 트리 상태: !`git status --short`
- 로컬 브랜치 목록: !`git branch`
- Remote에서 삭제된 브랜치: !`git fetch --prune --dry-run 2>&1`

## 작업 지침

아래 절차를 순서대로 수행한다.

### 1. 사전 확인

`git status --short` 결과가 비어 있지 않으면 (미커밋 변경 또는 untracked 파일 존재) **중단**하고 사용자에게 아래 중 하나를 선택하도록 안내한다:

- **커밋**: 변경 사항을 커밋한 뒤 다시 실행
- **스태시**: `git stash`로 임시 저장한 뒤 다시 실행
- **취소**: cleanup을 중단하고 현재 작업 유지

### 2. main으로 이동

```bash
git checkout main
```

### 2. 최신 상태 동기화

```bash
git pull
git fetch --prune
```

### 3. 머지된 로컬 브랜치 삭제

remote에서 사라진 브랜치 중 main·master를 제외한 로컬 브랜치를 삭제한다.

```bash
GONE=$(git branch -vv | grep ': gone]' | awk '{print $1}' | grep -v '^main$\|^master$')
[ -n "$GONE" ] && echo "$GONE" | xargs git branch -D
```

squash merge 방식을 사용하므로 `-D`로 강제 삭제한다. remote가 `gone`인 브랜치는 이미 PR 머지 후 삭제된 것이 확실하다.

### 4. 완료 보고

- 현재 브랜치 (main)
- 삭제된 로컬 브랜치 목록
- 삭제되지 않은 브랜치 (있는 경우)
