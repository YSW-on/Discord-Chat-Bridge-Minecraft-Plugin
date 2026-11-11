package org.blog.discordChatBridge;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public class MinecraftChatListener extends ListenerAdapter implements Listener {
    private volatile JDA jda;
    private final String channelId;
    private final Map<UUID, String> linked_list;
    private final JavaPlugin plugin;

    public MinecraftChatListener(String channelId, Map<UUID, String> linked_list, JavaPlugin plugin) {
        this.channelId = channelId;
        this.linked_list = linked_list;
        this.plugin = plugin;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!linked_list.containsKey(player.getUniqueId())) return;

        if (jda == null) return;
        MessageChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        channel.sendMessage("<" + player.getName() + "> " + message).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;
        if (!event.getChannel().getId().equals(channelId)) return;
        if (!linked_list.containsValue(event.getAuthor().getId())) return;

        String content = event.getMessage().getContentDisplay();
        if (content.isBlank()) return;

        Member member = event.getMember();
        String discordName = (member != null) ? member.getEffectiveName() : event.getAuthor().getName();
        String line = "<" + discordName + "> " + content;
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.getServer().broadcast(Component.text(line))
        );
    }
}