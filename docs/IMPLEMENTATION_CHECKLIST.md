# 구현 체크리스트

이 문서는 Episodive 앱의 기능 구현 상태를 추적하기 위한 체크리스트입니다.

## 온보딩

- ✅ 앱 소개
- ⬜ 권한 요청
- ✅ 선호 카테고리 선택 (2열 x행)
- ✅ 선호 팟캐스트 구독 선택 (vertical)
    - /recent/feeds (lang, cat)

## 메인 화면

Navigation 4개 탭:

- 홈
- 검색
- 라이브러리
- 클립

### 홈

- ✅ 최근 재생 에피소드 이어듣기 리스트 (horizontal)
- ✅ 선호 최근 팟캐스트 리스트 (horizontal)
    - /recent/feeds (lang, cat)
- ✅ 랜덤 에피소드 리스트 (vertical 6)
    - /episodes/random (lang, cat)
- ✅ 선호 인기 팟캐스트 리스트 (horizontal)
    - /podcasts/trending (lang, cat)
- ✅ 구독 팟캐스트 리스트 (horizontal)
- ✅ 지역 인기 팟캐스트 리스트 (horizontal)
    - /podcasts/trending (lang)
- ✅ 해외 인기 팟캐스트 리스트 (horizontal)
    - /podcasts/trending (lang)
- ✅ 라이브 에피소드 리스트 (vertical)
    - /episodes/live
- ✅ 채널 리스트 (horizontal)
    - db, /search/byterm
    - CNN, 뇌부자들, JTBC, BBC, ...

### 검색

- ✅ 검색창
    - /search/byterm (q, max)
    - /episodes/byfeedid (id, max)
    - 결과:
        - ✅ 검색 히스토리 (vertical)
        - ✅ 검색 팟캐스트 리스트 (vertical)
        - ✅ 검색 에피소드 리스트 (vertical)
- ⬜ 카테고리 리스트 (horizontal)
- ✅ 최근 에피소드 리스트 (horizontal)
    - /recent/episodes
    - 상세:
        - ⬜ 최근 팟캐스트 리스트 (horizontal)
            - /recent/feeds
        - ⬜ 최근 에피소드 리스트 (vertical)
            - /recent/episodes
- ⬜ 인기 팟캐스트 리스트 (vertical)
    - /podcasts/trending
    - 상세:
        - Global, Korean, Categories, ... chips

### 라이브러리

local db

#### 1. 메인

- ✅ 검색창
- ⬜ For you
- ✅ Recently listen
- ⬜ Saved
- ✅ Liked
- ✅ Followed

#### 2. 상세

- ✅ chips (For you, Recently listen, Saved, Liked, Followed)
- ⬜ For you, 랜덤 에피소드 리스트 (horizontal)
    - /episodes/random (lang, cat)
- ✅ Recently listen, 최근 재생 에피소드 리스트 (horizontal)
- ⬜ Saved, 저장된 팟캐스트 리스트 (horizontal)
- ✅ Liked, 좋아요 표시한 에피소드 리스트 (horizontal)
- ✅ Followed, 구독한 팟캐스트 리스트 (horizontal)

### 클립

- ✅ 구간 미리듣기
- ✅ 에피소드 좋아요
- ✅ 에피소드 재생
- ✅ 클립 자동 넘어가기

## 공통 화면

### 팟캐스트 상세

- ✅ image
- ✅ title
- ✅ description
- ✅ episodes

### 에피소드 플레이어

- ✅ image
- ✅ title
- ✅ description
- ✅ progress bar
- ✅ controls
- ✅ chapter
- ✅ transcript
- ✅ playlist
- ✅ playback speed
- ⬜ sleep timer
- ⬜ foreground service player