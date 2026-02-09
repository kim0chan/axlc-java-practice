# AXLC Java 실습 환경 세팅 가이드

이 가이드는 AXLC Java 실습을 위한 개발 환경 구축 방법을 안내합니다.

---

## 1. 준비 사항

- **JDK 21 이상**: 최신 Java 기능을 활용하므로 JDK 21 이상의 설치가 필수적입니다.
    - 추천: [Azul Zulu JDK](https://www.azul.com/downloads/?package=jdk#zulu) 또는 [Amazon Corretto](https://aws.amazon.com/corretto/)
- **Git**: 소스 코드 클론 및 버전 관리를 위해 필요합니다.
- **IDE (선택)**:
    - **IntelliJ IDEA (추천)**: Gradle 지원 및 Java 개발에 최적화되어 있습니다.
    - **VS Code**: `Extension Pack for Java` 설치가 필요합니다.
- **Bruno (선택)**:
    - Postman과 같은 API 테스팅 툴입니다.

---

## 2. 프로젝트 클론 및 설정

### 저장소 클론
```bash
git clone https://github.com/kim0chan/axlc-java-practice.git
cd axlc-java-practice
```

### 환경 변수 설정 (.env)
실습 코드에서 OpenAI API Key를 안전하게 로드하기 위해 `.env` 파일 설정이 필요합니다.  
(실습을 위한 API Key는 크루 진행 시 전달 드리겠습니다.)
1. 프로젝트 루트에 있는 `.env.example` 파일을 복사하여 `.env` 파일을 생성합니다.
2. 생성한 `.env` 파일을 열고 `your_key_here` 부분을 실제 OpenAI API Key로 교체합니다.

```text
# .env 파일 예시
OPENAI_API_KEY=sk-proj-....
```

---

## 3. 실행 방법

### IDE를 이용한 실행
IDE를 사용한다면 각 파일(`Step0StatelessChat.java` 등)에서 `main` 메서드 옆의 재생 버튼(▶️)을 눌러 바로 실행할 수 있습니다. 단, IDE 설정에서 **Annotation Processing**이 활성화되어 있는지 확인해주세요!
### Gradle을 이용한 실행
터미널에서 아래 명령어를 통해 각 Step의 실습 코드를 실행할 수 있습니다.

**Windows (PowerShell/CMD):**
> [!TIP]
> Windows 터미널에서 한글이 깨질 경우 실행 전 `chcp 65001` 명령어를 입력하세요.
```powershell
chcp 65001
# Step 0: Stateless Chat 실행
.\gradlew.bat run --args="step0"

# Step 1: Context Chat 실행
.\gradlew.bat run --args="step1"
```

**macOS/Linux:**
```bash
chmod +x gradlew
# Step 0: Stateless Chat 실행
./gradlew run --args="step0"

# Step 1: Context Chat 실행
./gradlew run --args="step1"
```

---

## 4. 트러블슈팅 (Troubleshooting)

- **인코딩 문제**: 한글이 깨져 보인다면 터미널의 인코딩이 UTF-8인지 확인하세요.
    - Windows: `chcp 65001` 실행
- **Gradle 빌드 에러**: `./gradlew clean build` 명령어를 통해 캐시를 삭제하고 다시 빌드해보세요.
- **API 호출 실패**:
    - `.env` 파일에 API Key가 올바르게 입력되었는지, 인터넷 연결이 정상인지 확인하세요.

---

## 5. API 테스트 (Bruno 활용)

코드 작성 전, LLM API가 정상적으로 동작하는지 확인하기 위해 [Bruno](https://www.usebruno.com/)를 활용할 수 있습니다. 프로젝트 루트에 설정 파일(`gpt-bruno-collection.json`)이 포함되어 있습니다.

### 브루노 설정 방법
1. **Bruno 설치**: [공식 홈페이지](https://www.usebruno.com/downloads)에서 OS에 맞는 버전을 설치합니다.
2. **컬렉션 불러오기**: Bruno 앱에서 `... > Import Collection > Bruno Collection`을 클릭하고 프로젝트의 `gpt-bruno-collection.json` 파일을 선택합니다.
    - 이후 뜨는 모달 창에서 Location은 bruno collection이 저장될 위치를 선택하시면 됩니다.
3. **API Key 설정**:
    - 불러온 'Create Completion' POST 요청을 선택합니다.
    - Headers 탭을 선택하고, `Authorization` value의 `your-key-here` 부분을 안내 받은 API Key로 변경합니다.
1. **요청 테스트**: `->` (Ctrl + Enter) 버튼을 눌러 응답이 오는지 확인합니다.

---

**즐거운 코딩 되세요!**