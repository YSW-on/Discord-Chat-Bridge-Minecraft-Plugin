package org.blog.discordChatBridge;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBotManager {
    public static JDA start(String token, MinecraftChatListener listener) throws Exception {
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.playing("Minecraft server"))
                .addEventListeners(listener)
                .build()
                .awaitReady();
        listener.setJda(jda);
        return jda;
    }
}
