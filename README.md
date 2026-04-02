# DailyContest-Notifier

한국산업인력공단_해외인턴 공모전 정보를 자동으로 스크래핑하여 **Discord**로 daily update 알림을 보내는 프로젝트입니다.  

---

## 📝 기능

- 한국산업인력공단 해외인턴 공모전 API를 이용해 최신 공모전 정보 가져오기  
- 중복 공모전 메시지 방지 (`sent.txt` 사용)  
- Discord Webhook을 통해 daily update 전송  
- Maven 기반 프로젝트로 JSoup, SnakeYAML 의존성 포함  

---

## ⚙️ 기술 스택

- Java 17  
- Maven  
- JSoup (HTML/XML 파싱)  
- SnakeYAML (config.yml 읽기)  
- HTTPURLConnection (API 요청 및 Discord Webhook 전송)  

---

## 📂 파일 구조

```text
Contest-Hub/
├─ src/main/java/EventScraper.java      # 메인 스크래퍼 코드
├─ config.yml                          # API 키 및 Discord Webhook 설정
├─ sent.txt                             # 이미 전송한 공모전 기록
├─ pom.xml                              # Maven 프로젝트 설정
└─ README.md                            # 프로젝트 설명
```

⚡ 사용 방법
1. config.yml 설정
```
api:
  worldjob:
    key: YOUR_API_KEY   # 한국산업인력공단 API 키
discord:
  webhook: YOUR_WEBHOOK_URL
```

2. Maven 빌드 및 실행
```
mvn clean compile
mvn exec:java -Dexec.mainClass="EventScraper"
```

3. 실행 시 동작
```
API에서 최신 공모전 정보를 가져옴
sent.txt에 없는 신규 공모전이면 Discord로 메시지 전송
메시지 전송 후 sent.txt에 기록
```
🔗 참고
```
한국산업인력공단 해외인턴 공모전 API
Discord Webhook Docs
```
