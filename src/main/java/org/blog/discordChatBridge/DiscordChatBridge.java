package org.blog.discordChatBridge;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class DiscordChatBridge extends JavaPlugin {

    private JDA jda;
    private final Map<UUID, String> linked_list = new ConcurrentHashMap<>();
    private final Map<UUID, PendingVerification> pendingVerification_list = new ConcurrentHashMap<>();

    private static class PendingVerification {
        final String discordId;
        final String code;
        final String discordName;
        PendingVerification(String discordId, String discordName, String code) {
            this.discordId = discordId;
            this.discordName = discordName;
            this.code = code;
        }
    }

    private void sendLinkError(Player player, Throwable error) {
        ErrorResponse response = (error instanceof ErrorResponseException ere) ? ere.getErrorResponse() : null;
        if (response == ErrorResponse.UNKNOWN_USER || error instanceof IllegalArgumentException) {
            player.sendMessage("Couldn't find that Discord user. Check the user ID and try again.");
        } else if (response == ErrorResponse.CANNOT_SEND_TO_USER) {
            player.sendMessage("Couldn't send the code. Make sure the account is in the Discord server and allows DMs from server members.");
        } else {
            player.sendMessage("Something went wrong. Try again later.");
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String guildId = getConfig().getString("discord.guild-id");
        String channelId = getConfig().getString("discord.chat-channel-id");
        String token = getConfig().getString("discord.token");

        try {
            ConfigurationSection section = getConfig().getConfigurationSection("Verified_user");
            if (section != null) {
                boolean configValid = true;
                for (String uuidKey : section.getKeys(false)) {
                    String discordId = section.getString(uuidKey + ".discord_id");
                    if (discordId == null || discordId.isBlank()) {
                        configValid = false;
                        continue;
                    }
                    try {
                        UUID uuid = UUID.fromString(uuidKey);
                        linked_list.put(uuid, discordId);
                    } catch (IllegalArgumentException e) {
                        configValid = false;
                    }
                }
                if (!configValid) {
                    getLogger().severe("Fix the entries above in config.yml and restart the server.");
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
            }

            MinecraftChatListener listener = new MinecraftChatListener(channelId, linked_list, this);
            jda = DiscordBotManager.start(token, listener);

            if (jda.getGuildById(guildId) == null) {
                getLogger().severe("Couldn't find the Discord server. Check discord.guild-id in config.yml.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                getLogger().severe("Couldn't find the Discord text channel. Check discord.chat-channel-id in config.yml.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            if (!channel.getGuild().getId().equals(guildId)) {
                getLogger().severe("The Discord text channel does not belong to the configured server.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            getServer().getPluginManager().registerEvents(listener, this);
            channel.sendMessage("Minecraft server is now online").queue();
            getLogger().info("DiscordChatBridge enabled");

        } catch (Exception e) {
            getLogger().severe("Failed to start DiscordChatBridge: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
                    jda.shutdownNow();
                    jda.awaitShutdown(Duration.ofSeconds(5));
                }
            } catch (InterruptedException e) {
                jda.shutdownNow();
                Thread.currentThread().interrupt();
            }
            getLogger().info("Discord bot shut down");
        }
        getLogger().info("DiscordChatBridge disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        switch (command.getName()) {
            case "link" -> linkCommand(player, args);
            case "verify" -> verifyCommand(player, args);
            case "unlink" -> unlinkCommand(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void linkCommand(Player player, String[] args) {
        UUID McId = player.getUniqueId();

        if (args.length < 1) {
            player.sendMessage("/link <Your Discord user id>");
            return;
        }
        if (linked_list.containsKey(McId)) {
            player.sendMessage("Your account is already linked.");
            return;
        }

        String DcId = args[0];
        String verificationCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        try {
            jda.retrieveUserById(DcId)
                    .flatMap(user -> user.openPrivateChannel()
                            .flatMap(channel -> channel.sendMessage("Your verification code: " + verificationCode))
                            .map(message -> user))
                    .queue(
                            user -> {
                                pendingVerification_list.put(McId, new PendingVerification(DcId, user.getName(), verificationCode));
                                Bukkit.getScheduler().runTask(this, () ->
                                        player.sendMessage("Verification code sent to your Discord DMs.\nEnter it with /verify <code> to finish linking.")
                                );
                            },
                            error -> Bukkit.getScheduler().runTask(this, () -> sendLinkError(player, error))
                    );
        } catch (Exception e) {
            sendLinkError(player, e);
        }
    }

    private void verifyCommand(Player player, String[] args) {
        UUID McId = player.getUniqueId();

        if (args.length < 1) {
            player.sendMessage("/verify <Your verification code>");
            return;
        }
        if (linked_list.containsKey(McId)) {
            player.sendMessage("Your account is already linked.");
            return;
        }

        PendingVerification pending = pendingVerification_list.get(McId);
        if (pending == null) {
            player.sendMessage("No pending request. Start with /link <Discord user ID>.");
            return;
        }
        if (!pending.code.equals(args[0])) {
            player.sendMessage("That code doesn't match. Check your Discord DMs and try again.");
            return;
        }

        String McName = player.getName();
        String DcId = pending.discordId;
        String DcName = pending.discordName;
        getConfig().set("Verified_user." + McId + ".discord_id", DcId);
        saveConfig();
        linked_list.put(McId, DcId);
        pendingVerification_list.remove(McId);
        player.sendMessage(
                "----------------------------\n"
                        + "Account linked\n"
                        + "Your Minecraft username: " + McName + "\n"
                        + "Your Minecraft UUID: " + McId + "\n"
                        + "Your Discord username: " + DcName + "\n"
                        + "Your Discord user ID: " + DcId + "\n"
                        + "----------------------------"
        );
    }

    private void unlinkCommand(Player player) {
        UUID McId = player.getUniqueId();

        if (!linked_list.containsKey(McId)) {
            player.sendMessage("Your account isn't linked.");
            return;
        }

        getConfig().set("Verified_user." + McId, null);
        saveConfig();
        linked_list.remove(McId);
        player.sendMessage("Your account has been unlinked.");
    }
}