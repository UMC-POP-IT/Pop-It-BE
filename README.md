# 🎪 POP-IT Backend

> 비어있는 공간과 브랜드의 상상력이 만나는 곳  
> 단기 상업 공간 대관 플랫폼 **POP-IT**의 백엔드 레포지토리입니다.

<br>

## 🛠 Tech Stack

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![AWS](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)

<br>

## 📁 Package Structure

```
com.popIt
├── domain
│   ├── contract
│   ├── escrow
│   ├── facility
│   ├── pass
│   ├── payment
│   ├── reservation
│   ├── space
│   ├── terms
│   ├── user
│   ├── user_activity
│   ├── user_agreement
│   └── wishlist
└── global
    ├── config
    ├── exception
    └── 
```

<br>

## 🌿 Branch Strategy

```
main        ← 배포용 (직접 push 금지)
└── develop ← 개발 통합 브랜치
    └── feat/{이슈번호}-{기능명}
```

| 브랜치 | 설명 |
|--------|------|
| `main` | 프로덕션 배포 브랜치 |
| `develop` | 개발 통합 브랜치, PR 머지 대상 |
| `feat/{n}-{기능}` | 기능 개발 브랜치 |
| `fix/{n}-{내용}` | 버그 수정 브랜치 |

<br>

## ✍️ Commit Convention

```
[type(이름(닉네임)): 수정 내용 ]

```

| Type | 설명 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변화 없는 코드 개선 |
| `perf` | 성능 개선 (쿼리 최적화, 동시성 개선 등) |
| `docs` | 문서 수정 |
| `test` | 테스트 코드 |
| `chore` | 빌드/설정 변경 |
| `style` | 코드 포맷 변경 |
| `ci` | CI/CD 설정 (GitHub Actions 등) |


<br>

## 📋 Issue Template

<details>
<summary>✨ Feature</summary>

```markdown
---
name: "✨ Feature"
about: '새로운 기능 추가'
title: "[Feature] 이슈 제목"
labels: enhancement
assignees: ''
---

## 📄 설명
설명을 작성해주세요.

## ✅ TO_DO
- [ ] 해야할 일 1
- [ ] 해야할 일 2

## 🔔 기타
기타 사항을 작성해주세요.
```

</details>

<details>
<summary>⚒️ Refactor</summary>

```markdown
---
name: "⚒️ Refactor"
about: '리팩토링'
title: "[Refactor] 리팩토링 제목"
labels: ''
assignees: ''
---

## 이슈 제목
`[영역/모듈] - [리팩토링 내용 요약]`

## 리팩토링 대상
- **대상 코드/모듈:**
- **관련 기능/도메인:**
- **현재 코드 위치:**

## 리팩토링 필요성 및 배경

## 현재 문제점

## 예상되는 개선 효과

## 제안하는 리팩토링 방안

## 고려사항 및 잠재적 위험

## 테스트 계획

## 관련 자료 / 참고 링크
```

</details>

<details>
<summary>🐛 Bug</summary>

```markdown
---
name: "🐛 Bug"
about: '버그 제보'
title: "[Bug] 버그 제목"
labels: bug
assignees: ''
---

## 버그 설명

## 재현 방법
1. '...'으로 이동
2. '...'을 클릭
3. 오류 확인

## 예상 동작

## 스크린샷

## 환경
- OS:
- 브라우저:
- 버전:

## 추가 정보
```

</details>

<details>
<summary>🚀 Deploy</summary>

```markdown
---
name: "🚀 Deploy"
about: '배포 관련 작업'
title: "[Deploy] 배포 제목"
labels: ''
assignees: ''
---

## 배포 설명

## 우선순위
- [ ] High (즉시 해결 필요)
- [ ] Medium (다음 배포 전 해결)
- [ ] Low (장기적으로 해결)

## 발생 환경 및 정보
- **배포 환경:**
- **배포 버전:**
- **배포 브랜치:**
- **배포 시작 시간:**
- **배포 시도자:**

## 예상 동작
```

</details>

<br>

## 👥 Contributors

| 이름 | 역할                | GitHub |
|------|-------------------|--------|
| 최서연 | 결제, 활동기록(AI추천)    | |
| 김유진 | 계약, 본인인증          | |
| 김하림 | 공간                | |
| 송시찬 | 예약, 에스크로          | |
| 이권형 | 사용자(로그인/회원가입), 약관 | |
