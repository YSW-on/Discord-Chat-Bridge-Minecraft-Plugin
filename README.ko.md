[English](README.md) | [한국어](README.ko.md)

## 개요

마인크래프트 서버와 디스코드 채널 사이의 채팅을 양방향으로 전달하는 플러그인입니다. 플레이어가 게임 안에서 자신의 디스코드 계정을 연동하면, 연동한 플레이어의 채팅만 전달됩니다. [JDA](https://github.com/discord-jda/JDA)(Apache-2.0)와 [Paper(26.3)](https://papermc.io)를 사용했습니다.

## 설치 및 설정

1. [Discord Developer Portal](https://discord.com/developers/applications)을 열고 **New Application**을 누릅니다.
2. 왼쪽의 **Bot** 탭에서 봇 토큰을 발급받거나 확인합니다. 토큰은 한 번만 표시되므로 안전한 곳에 저장해 두십시오. 만약 토큰이 유출되거나 잊어버린 경우 **Reset Token**을 눌러 토큰을 재생성하십시오.

**주의: 봇 토큰은 비공개로 유지하고, 다른 사람에게 알려 주거나 절대 공유하지 마십시오.**

3. **Bot** 탭에서 **Privileged Gateway Intents**까지 내려가 **Message Content Intent**를 켜고 저장합니다. 켜지 않으면 플러그인이 디스코드 메시지를 읽지 못해 시작에 실패합니다.
4. **OAuth2** > **URL Generator**에서 `bot` 범위를 선택하고 **View Channels**와 **Send Messages** 권한을 선택합니다. 생성된 URL을 열어 봇을 원하는 디스코드 서버에 추가합니다.
5. 봇이 채팅 전달에 쓸 채널을 볼 수 있고 그 채널에 메시지를 보낼 수 있는지 확인합니다.
6. 내려받은 플러그인 JAR 파일을 서버의 `plugins` 폴더에 넣고 서버를 시작합니다.
7. 플러그인이 처음 실행될 때 서버가 필요한 라이브러리(JDA 등)를 내려받습니다. 토큰이 비어 있기 때문에 오류를 출력한 뒤 비활성화됩니다.
8. 서버를 종료한 뒤 봇이 추가된 디스코드 서버의 ID와 채팅 채널의 ID를 우클릭하여 복사한 뒤 복사해 둔 토큰, 디스코드 서버 ID, 채팅 채널 ID를 생성된 `plugins/DiscordChatBridge/config.yml`의 다음 줄에 붙여넣습니다. ID를 복사하는 버튼이 표시되지 않는 경우 **사용자 설정** > **고급**에서 **개발자 모드**를 활성화하십시오.
    ```yaml
    discord:
      token: ''
      guild-id: ''
      chat-channel-id: ''
    ```

9. 서버를 다시 시작합니다. 플러그인이 활성화되면 콘솔에 `DiscordChatBridge enabled`가 출력되고, 봇이 채팅 채널에 `Minecraft server is now online` 메시지를 보냅니다.

## 소스에서 빌드

빌드 시에는 JDK 25가 설치되어 있어야 합니다. 만약 컴파일에 상위 버전의 JDK를 사용하고 싶은 경우 `build.gradle.kts`를 수정하십시오.
다음 명령어를 사용해 빌드합니다.
```
./gradlew build
```

Windows PowerShell에서는 `.\gradlew build`를 사용하십시오.
플러그인 JAR 파일은 `build/libs/`에 만들어지며, 이 프로젝트의 코드만 들어 있습니다.

## 테스트 서버 실행

다음 명령어를 사용해 테스트 서버를 실행합니다.
```
./gradlew runServer
```

Windows PowerShell에서는 `.\gradlew runServer`를 사용하십시오.
Paper를 내려받고 `run/` 폴더에서 플러그인과 함께 테스트 서버를 실행합니다. 서버에 2GB의 메모리를 할당하도록 설정되어 있으며 이를 수정하고 싶은 경우 `build.gradle.kts`를 수정하십시오.
```kotlin
runServer {
    minecraftVersion("26.3")
    jvmArgs("-Xms2G", "-Xmx2G")
}
```

## 사용 방법

1. 플레이어가 `/link <디스코드 사용자 ID>`를 입력하면 봇이 그 디스코드 사용자에게 DM으로 인증번호를 보냅니다.
2. 플레이어가 게임에서 `/verify <인증번호>`를 입력하면 연동이 `config.yml`에 저장됩니다.
3. 이후 플레이어의 마인크래프트 채팅이 설정한 디스코드 채널에 올라가고, 그 디스코드 사용자가 채널에 쓴 메시지가 게임에 표시됩니다. 연동한 디스코드 사용자의 메시지는 그 플레이어가 마인크래프트 서버에 접속해 있지 않아도 게임에 표시됩니다.
4. 연동된 플레이어가 게임에서 `/unlink`를 입력하면 연동이 해제되어 메시지가 다른 쪽으로 전달되지 않습니다.

## 작동 방식

1. 시작될 때 `config.yml`이 없으면 이를 폴더 안에 생성합니다. 이후 `guild-id`, `chat-channel-id`, `token` 값을 읽어 변수에 담습니다.
2. `config.yml`의 `Verified_user` 섹션에서 디스코드 ID와 마인크래프트 UUID를 가져와 `linked_list`에 담습니다.
3. 이후 디스코드 봇을 시작합니다. 조건을 만족하지 않으면 아래의 오류가 발생하며 플러그인이 종료됩니다.

    | 오류 | 원인 |
    | --- | --- |
    | `Fix the entries above in config.yml and restart the server.` | `config.yml`의 `Verified_user` 섹션의 값이 잘못됨 |
    | 플러그인이 시작될 때 intents가 허용되지 않았다는 오류 | 개발자 포털에서 `Message Content Intent`가 활성화되지 않음 |
    | 첫 시작 직후의 `Failed to start DiscordChatBridge: ...` | 토큰이 비어 있거나 잘못됨 |
    | JDA 클래스를 찾을 수 없다는 오류나 라이브러리 다운로드 오류 | 방화벽이나 프록시 또는 요청 제한에 의해 서버가 라이브러리 저장소에 접속하지 못한 경우 |
    | `Couldn't find the Discord server. Check discord.guild-id in config.yml.` | 서버 ID가 잘못되었거나 봇이 그 서버에 추가되지 않음 |
    | `Couldn't find the Discord text channel. Check discord.chat-channel-id in config.yml.` | 채팅 채널 ID가 잘못되었거나 봇이 그 채널을 볼 수 없음 |
    | `The Discord text channel does not belong to the configured server.` | 설정한 채팅 채널이 설정한 서버에 속하지 않음 |


4. 이하의 명령어를 플레이어가 아닌 명령어 실행 주체(서버 콘솔 등)가 실행하지 못하도록 합니다.
5. 플레이어가 `/link` 명령어를 실행하면 플레이어의 UUID를 가져오며 조건에 맞지 않으면 다음 메시지를 출력하며 return합니다.

    | 오류 | 원인 |
    | --- | --- |
    | `/link <Your Discord user id>` | 명령어 뒤에 디스코드 ID를 입력하지 않음 |
    | `Your account is already linked.` | 해당 UUID가 `linked_list`에 있음(이미 연동된 경우) |

6. 인증번호 6자리를 생성합니다.
7. 입력받은 디스코드 ID를 가진 사용자에게 DM으로 인증번호를 보내며 플레이어 UUID를 키(key)로 사용하여 디스코드 ID, 디스코드 사용자명, 인증번호를 담은 `PendingVerification`을 `pendingVerification_list`에 담습니다. 조건을 만족하지 않으면 아래의 오류가 발생합니다.

    | 오류 | 원인 |
    | --- | --- |
    | `Couldn't find that Discord user. Check the user ID and try again.` | 입력한 디스코드 ID가 잘못됨 |
    | `Couldn't send the code. Make sure the account is in the Discord server and allows DMs from server members.` | 사용자의 DM 수신 설정 등으로 인해 대상 사용자에게 DM을 전송할 수 없음 |
    | `Something went wrong. Try again later.` | 이외의 오류(요청 제한, 서버 장애, 시간 초과 등)가 발생한 경우 |

8. 플레이어가 `/verify` 명령어를 실행하면 플레이어의 UUID를 가져옵니다. 해당 UUID에 대응하는 인증 요청이 존재하는지 확인하고, 입력한 인증번호가 발급된 인증번호와 일치하는지 검사합니다. 인증에 성공하면 `config.yml`의 `Verified_user` 섹션에 연동 정보를 저장하고, `linked_list`에 UUID와 디스코드 ID를 추가한 뒤 `pendingVerification_list` 에서 인증 요청을 제거합니다. 조건에 맞지 않으면 다음 메시지를 출력하며 return합니다.

    | 오류 | 원인 |
    | --- | --- |
    | `/verify <Your verification code>` | 명령어 뒤에 인증번호를 입력하지 않음 |
    | `Your account is already linked.` | 해당 UUID가 `linked_list`에 있음(이미 연동된 경우) |
    | `No pending request. Start with /link <Discord user ID>.` | 해당 UUID가 `pendingVerification_list`에 없음(`/link`를 통해 인증번호를 받지 않음) |
    | `That code doesn't match. Check your Discord DMs and try again.` | 입력한 인증번호가 일치하지 않음 |

    연동된 플레이어가 마인크래프트 서버 내에서 메시지를 전송하면 디스코드 서버 내의 채팅 채널에 그대로 전달되며, 디스코드 서버 내에서 메시지를 전송하면 마인크래프트 서버에 전달됩니다.

9. 플레이어가 `/unlink` 명령어를 실행하면 플레이어의 UUID를 가져와 해당 플레이어의 연동 정보를 `linked_list`에서 제거하며 `config.yml`의 `Verified_user` 섹션에서 제거합니다. 해당 UUID가 이미 `linked_list`에 없다면 메시지를 출력하며 return합니다.

## 주의사항

- 관리자 권한(op)이 없는 플레이어도 명령어를 사용할 수 있습니다.
- 누구든지 봇이 있는 서버에 함께 있는 임의의 디스코드 사용자의 ID를 복사하여 DM으로 인증번호를 보내게 할 수 있습니다.
- 인증번호는 만료되지 않으며 인증 시도 횟수에도 제한이 없으므로 무차별 대입에 취약할 수 있으며 여러 마인크래프트 계정이 동일한 디스코드 계정에 연동될 수 있습니다.
- 위와 같은 문제를 방지하려면 신뢰할 수 있는 환경에서 사용하십시오.
- 메시지는 그대로 전달됩니다. 예를 들어 마인크래프트 서버에서 입력한 `@everyone`이나 `@here`는 봇에게 Mention Everyone 권한이 있으면 대상에게 알림이 갑니다. 디스코드 서버에서 `§`를 입력하면 색상 코드가 적용됩니다.

- [Paper 26.3 #239](https://fill-ui.papermc.io/projects/paper/version/26.3?build=239)에서 테스트하였으며 다른 버전의 Paper에서 작동하지 않을 수 있습니다.
- Paper가 아닌 다른 서버 구동기(Spigot, Sponge 등)에서 작동하지 않습니다.
- 서버가 필요한 라이브러리를 내려받을 수 있어야 합니다. Paper는 기본적으로 Google이 운영하는 Maven Central 미러(`maven-central.storage-download.googleapis.com`)를 사용하므로, 방화벽이 있다면 이 주소를 허용하십시오. 인터넷이 차단된 환경에서는 사용할 수 없습니다. 자세한 내용은 [Paper 공식 문서](https://docs.papermc.io/paper/reference/system-properties/#orgbukkitpluginjavalibraryloadercentralurl)를 참고하십시오.

- `config.yml`은 서버를 닫은 상태에서 수정해야 합니다.

## 라이선스

- 이 프로젝트는 [MIT 라이선스](LICENSE)로 배포됩니다.
- 이 저장소와 플러그인 JAR에는 JDA와 그 의존 라이브러리가 포함되어 있지 않습니다. 서버가 시작할 때 각 라이브러리를 별도로 내려받으며 [자체 라이선스](THIRD_PARTY_NOTICES.md)를 따릅니다. 