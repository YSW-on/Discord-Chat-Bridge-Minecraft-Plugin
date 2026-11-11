[English](README.md) | [한국어](README.ko.md)

## Overview

This plugin relays chat bidirectionally between a Minecraft server and a Discord channel. When players link their Discord accounts in-game, only the chat messages of linked players are relayed. It uses [JDA](https://github.com/discord-jda/JDA)(Apache-2.0) and [Paper(26.3)](https://papermc.io).

## Installation and Configuration

1. Open the [Discord Developer Portal](https://discord.com/developers/applications) and click **New Application**.
2. In the **Bot** tab on the left, generate or check the bot token. The token is shown only once, so store it in a safe place. If the token is leaked or forgotten, click **Reset Token** to regenerate it.

**Warning: Keep your bot token private. Never disclose or share it with anyone.**

3. In the **Bot** tab, scroll down to **Privileged Gateway Intents**, enable **Message Content Intent**, and save. If it is not enabled, the plugin will fail to start because it cannot read Discord messages.
4. Under **OAuth2** > **URL Generator**, select the `bot` scope and the **View Channels** and **Send Messages** permissions. Open the generated URL to add the bot to the desired Discord server.
5. Make sure the bot can view the channel used for chat relaying and send messages to that channel.
6. Place the downloaded plugin JAR file in the server's `plugins` folder and start the server.
7. When the plugin runs for the first time, the server downloads the required libraries (including JDA). Because the token is empty, the plugin displays an error and then disables itself.
8. Shut down the server, then right-click and copy the ID of the Discord server to which the bot was added and the ID of the chat channel. Paste the copied token, Discord server ID, and chat channel ID into the following lines of the generated `plugins/DiscordChatBridge/config.yml`. If the option to copy IDs does not appear, enable **Developer Mode** under **User Settings** > **Advanced**.
    ```yaml
    discord:
      token: ''
      guild-id: ''
      chat-channel-id: ''
    ```

9. Restart the server. When the plugin is enabled, `DiscordChatBridge enabled` is printed to the console, and the bot sends `Minecraft server is now online` to the chat channel.

## Building from Source

JDK 25 must be installed to build the project. If you want to use a higher version of the JDK for compilation, modify `build.gradle.kts`.
Build using the following command.
```
./gradlew build
```

On Windows PowerShell, use `.\gradlew build`.
The plugin JAR file is created in `build/libs/` and contains only this project's code.

## Running the Test Server

Run the test server using the following command.
```
./gradlew runServer
```

On Windows PowerShell, use `.\gradlew runServer`.
This downloads Paper and runs the test server together with the plugin in the `run/` folder. It is configured to allocate 2GB of memory to the server. If you want to change this, modify `build.gradle.kts`.
```kotlin
runServer {
    minecraftVersion("26.3")
    jvmArgs("-Xms2G", "-Xmx2G")
}
```

## How to Use

1. When a player enters `/link <Discord user ID>`, the bot sends a verification code to that Discord user via DM.
2. When the player enters `/verify <verification code>` in-game, the link is saved in `config.yml`.
3. After that, the player's Minecraft chat messages appear in the configured Discord channel, and messages written by that Discord user in the channel appear in-game. Messages from a linked Discord user are displayed in-game even when the linked player is not connected to the Minecraft server.
4. When a linked player enters `/unlink` in-game, the account link is removed and messages are no longer relayed between the two sides.

## How It Works

1. On startup, if `config.yml` does not exist, the plugin creates it in the folder. It then reads the `guild-id`, `chat-channel-id`, and `token` values into variables.
2. It retrieves the Discord IDs and Minecraft UUIDs from the `Verified_user` section of `config.yml` and stores them in `linked_list`.
3. It then starts the Discord bot. If the conditions are not met, the following errors occur and the plugin shuts down.

    | Error | Cause |
    | --- | --- |
    | `Fix the entries above in config.yml and restart the server.` | Incorrect values in the `Verified_user` section of `config.yml` |
    | An error stating that intents are not allowed when the plugin starts | `Message Content Intent` is not enabled in the Developer Portal |
    | `Failed to start DiscordChatBridge: ...` immediately after the first startup | The token is empty or incorrect |
    | An error indicating that JDA classes cannot be found, or a library download error | The server cannot connect to the library repository due to a firewall, proxy, or rate limit |
    | `Couldn't find the Discord server. Check discord.guild-id in config.yml.` | The server ID is incorrect, or the bot has not been added to that server |
    | `Couldn't find the Discord text channel. Check discord.chat-channel-id in config.yml.` | The chat channel ID is incorrect, or the bot cannot view the channel |
    | `The Discord text channel does not belong to the configured server.` | The configured chat channel does not belong to the configured server |


4. It prevents command senders other than players (such as the server console) from executing the commands below.
5. When a player executes the `/link` command, it retrieves the player's UUID. If the conditions are not met, it displays the following messages and returns.

    | Error | Cause |
    | --- | --- |
    | `/link <Your Discord user id>` | No Discord ID was entered after the command |
    | `Your account is already linked.` | The UUID is in `linked_list` (already linked) |

6. It generates a six-digit verification code.
7. It sends the verification code via DM to the user with the provided Discord ID, and stores a `PendingVerification` containing the Discord ID, Discord username, and verification code in `pendingVerification_list`, using the player's UUID as the key. If the conditions are not met, the following errors occur.

    | Error | Cause |
    | --- | --- |
    | `Couldn't find that Discord user. Check the user ID and try again.` | The entered Discord ID is incorrect |
    | `Couldn't send the code. Make sure the account is in the Discord server and allows DMs from server members.` | A DM cannot be sent to the target user due to their DM reception settings or other reasons |
    | `Something went wrong. Try again later.` | Another error occurred (rate limit, server outage, timeout, etc.) |

8. When a player executes the `/verify` command, it retrieves the player's UUID. It checks whether a verification request corresponding to that UUID exists and whether the entered verification code matches the issued code. If verification succeeds, it saves the link information in the `Verified_user` section of `config.yml`, adds the UUID and Discord ID to `linked_list`, and then removes the verification request from `pendingVerification_list`. If the conditions are not met, it displays the following messages and returns.

    | Error | Cause |
    | --- | --- |
    | `/verify <Your verification code>` | No verification code was entered after the command |
    | `Your account is already linked.` | The UUID is in `linked_list` (already linked) |
    | `No pending request. Start with /link <Discord user ID>.` | The UUID is not in `pendingVerification_list` (a verification code was not received through `/link`) |
    | `That code doesn't match. Check your Discord DMs and try again.` | The entered verification code does not match |

    When a linked player sends a message on the Minecraft server, it is relayed as-is to the chat channel on the Discord server, and when a message is sent on the Discord server, it is relayed to the Minecraft server.

9. When a player executes the `/unlink` command, it retrieves the player's UUID and removes that player's link information from `linked_list` and from the `Verified_user` section of `config.yml`. If the UUID is not in `linked_list`, it displays a message and returns.

## Notes

- Players without operator privileges (op) can also use the commands.
- Anyone can copy the ID of any Discord user who shares the server with the bot and cause a verification code to be sent to that user's DMs.
- Verification codes do not expire, and there is no limit on verification attempts, so the system may be vulnerable to brute-force attacks, and multiple Minecraft accounts can be linked to the same Discord account.
- To prevent the problems above, use the plugin in a trusted environment.
- Messages are relayed as-is. For example, `@everyone` or `@here` entered on the Minecraft server will notify the recipients if the bot has the Mention Everyone permission. If `§` is entered on the Discord server, color codes are applied.

- Tested on [Paper 26.3 #239](https://fill-ui.papermc.io/projects/paper/version/26.3?build=239); it may not work on other versions of Paper.
- It does not work on other server software (such as Spigot or Sponge) that is not Paper.
- The server must be able to download the required libraries. Paper uses Google's Maven Central mirror (`maven-central.storage-download.googleapis.com`) by default, so if there is a firewall, allow access to this address. It cannot be used in an environment without internet access. For more information, see the [official Paper documentation](https://docs.papermc.io/paper/reference/system-properties/#orgbukkitpluginjavalibraryloadercentralurl).

- `config.yml` must be modified while the server is shut down.

## License

- This project is distributed under the [MIT License](LICENSE).
- JDA and its dependent libraries are not included in this repository or the plugin JAR. Each library is downloaded separately when the server starts and is subject to its [own license](THIRD_PARTY_NOTICES.md).
