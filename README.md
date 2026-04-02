#  DailyContest-Notifier

위비티의 최신 공모전 정보를 자동으로 스크래핑하여 **GitHub Pages** 대시보드에 전시하고, **Discord**로 실시간 알림을 보내는 프로젝트입니다.

---

##  주요 기능

- **실시간 스크래핑**: JSoup을 사용하여 위비티의 최신 공모전 데이터 20개를 수집합니다.
- **웹 대시보드 제공**: GitHub Actions를 통해 자동 업데이트되는 깔끔한 공모전 리스트 페이지를 운영합니다.
- **텍스트 중심 최적화**: 이미지 로딩 문제를 해결하기 위해 가독성이 높은 텍스트 기반 카드를 사용하며, 제목이 길어도 잘리지 않도록 박스 크기를 유연하게 조정했습니다.
- **실시간 검색**: 대시보드 상단 검색창을 통해 키워드로 공모전을 즉시 필터링할 수 있습니다.
- **중복 방지 알림**: `sent.txt`를 활용해 이미 알림을 보낸 공모전은 제외하고 신규 정보만 Discord Webhook으로 전송합니다.

---

## ⚙️ 기술 스택

- **Language**: Java 17
- **Library**: JSoup (HTML 파싱)
- **Deployment**: GitHub Pages & GitHub Actions
- **Framework**: Bootstrap 5 (UI 디자인)
- **Communication**: Discord Webhook

---

## 파일 구조

```text
Contest-Hub/
├─ src/main/java/EventScraper.java  # 메인 스크래퍼 및 HTML 생성 코드
├─ index.html                       # 생성된 웹 대시보드 (GitHub Pages용)
├─ sent.txt                         # 이미 전송한 공모전 기록
├─ pom.xml                          # Maven 프로젝트 설정 및 의존성 관리
└─ README.md                        # 프로젝트 설명서
```
사용 방법
Discord 웹훅 설정

EventScraper.java 파일 내 DISCORD_WEBHOOK_URL 변수에 본인의 디스코드 웹훅 주소를 입력합니다.

로컬 실행

Bash
mvn clean compile
mvn exec:java -Dexec.mainClass="EventScraper"
자동 배포 (GitHub Actions)

코드를 GitHub 레포지토리에 push하면 설정된 Workflow에 따라 자동으로 스크래핑이 실행되고 index.html이 갱신되어 웹사이트에 반영됩니다.

결과 확인
웹 대시보드: https://hyun-jun-lee0811.github.io/DailyContest-Notifier/
