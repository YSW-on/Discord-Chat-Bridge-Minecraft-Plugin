# Third-Party Notices

This project uses third-party software. Each third-party component remains subject to its own license and copyright notices.

## JDA (Java Discord API)

- **Version:** 6.7.0
- **Maven coordinates:** `net.dv8tion:JDA:6.7.0`
- **Project:** https://github.com/discord-jda/JDA
- **License:** Apache License, Version 2.0
- **License text:** https://www.apache.org/licenses/LICENSE-2.0

JDA is used to connect the Minecraft server plugin to Discord.

## JDA Dependencies

JDA uses additional third-party libraries. These dependencies may be downloaded when the plugin starts. Each dependency has its own applicable license and copyright notices, which may differ from JDA's Apache License, Version 2.0.

For the dependency list and associated license information, consult the published JDA 6.7.0 dependency metadata and the respective libraries' license files:

- https://central.sonatype.com/artifact/net.dv8tion/JDA/6.7.0

## Paper API

- **Dependency:** `io.papermc.paper:paper-api`
- **Target version:** Paper 26.3
- **Project:** https://papermc.io/
- **Source:** https://github.com/PaperMC/Paper

The Paper API is a compile-time dependency. It is provided by the Paper server at runtime and is not bundled in the plugin JAR.

## Distribution

The source repository and the plugin JAR do not bundle JDA or its transitive dependencies. The plugin declares JDA as a runtime library in `plugin.yml`, allowing the server to download the required libraries separately.

This document identifies known direct third-party components; it is not an exhaustive inventory of every transitive dependency or its license. If you redistribute third-party libraries separately or bundle them in a modified distribution, review and comply with the license and notice requirements of each included component.

The DiscordChatBridge project's own source code is licensed under the [MIT License](LICENSE).
