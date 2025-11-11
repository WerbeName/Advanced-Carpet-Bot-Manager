package carpetbotrestriction;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
// import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents; // Not available in current Fabric version
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CarpetBotRestriction implements ModInitializer {
    public static final String MOD_ID = "carpetbotrestriction";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static final String BOT_TEAM_NAME = "CBR_Bots";
    private static final Map<String, String> BOT_OWNERS = new HashMap<>();
    private static CBRConfig config;
    
    // Bot-Tracking für versteckte Entities (kein Minimap/Locator, aber Tab-Liste bleibt)
    public static final Set<UUID> HIDDEN_BOTS = ConcurrentHashMap.newKeySet();
    public static final Map<UUID, UUID> BOT_OWNER_MAP = new ConcurrentHashMap<>();

    // SuggestionProvider für Bot-Namen und Spieler-Namen
    private static final SuggestionProvider<ServerCommandSource> BOT_AND_PLAYER_SUGGESTIONS = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        
        // Füge "ALL" als Option hinzu
        builder.suggest("ALL");
        
        // Füge alle Online-Spieler hinzu
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            builder.suggest(player.getName().getString());
        }
        
        // Füge alle aktiven Bot-Namen hinzu
        for (String botName : BOT_OWNERS.keySet()) {
            ServerPlayerEntity bot = server.getPlayerManager().getPlayer(botName);
            if (bot != null && bot instanceof EntityPlayerMPFake) {
                builder.suggest(botName);
            }
        }
        
        return builder.buildFuture();
    };

    @Override
    public void onInitialize() {
        LOGGER.info("Carpet Bot Restriction mod initialized!");
        config = new CBRConfig();
        
        // Server-Lifecycle-Events für Bot-Cleanup
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started - clearing bot ownership data");
            BOT_OWNERS.clear();
            HIDDEN_BOTS.clear();
            BOT_OWNER_MAP.clear();
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping - clearing bot ownership data");
            BOT_OWNERS.clear();
            HIDDEN_BOTS.clear();
            BOT_OWNER_MAP.clear();
        });
        
        // Register commands using Fabric's command registration callback
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher, registryAccess, environment);
        });
        
        // Entity-Tracking-Event: Verhindert dass Bots für andere Spieler getrackt werden (DISABLED - API not available)
        /*
        EntityTrackingEvents.ALLOW_TRACKING.register((tracked, viewer) -> {
            if (tracked instanceof ServerPlayerEntity bot && isHiddenBot(bot)) {
                boolean canSee = canSeeBot((ServerPlayerEntity) viewer, bot);
                LOGGER.info("Entity tracking check: Bot {} viewed by {} - Can see: {}", 
                    bot.getName().getString(), ((ServerPlayerEntity) viewer).getName().getString(), canSee);
                return canSee;
            }
            return true;
        });
        */
        
        // Event-Listener für Bot-Team-Assignment und Entity-Hiding
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String playerName = player.getName().getString();
            
            // Bot-Team-Assignment für eigene Bots
            if (player instanceof EntityPlayerMPFake && BOT_OWNERS.containsKey(playerName)) {
                LOGGER.info("Bot {} joined the game - adding to team", playerName);
                server.execute(() -> {
                    addBotToTeam(server, player);
                });
            }
            
            // Verstecke alle Bots vor dem neuen Spieler (außer seinen eigenen und wenn er OP ist)
            server.execute(() -> {
                for (UUID botUuid : HIDDEN_BOTS) {
                    ServerPlayerEntity bot = server.getPlayerManager().getPlayer(botUuid);
                    if (bot == null) continue;
                    
                    if (!canSeeBot(player, bot)) {
                        // Verstecke den Bot vom neuen Spieler - mehrfache Packets für bessere Abdeckung
                        player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(bot.getId()));
                        
                        // Delayed packet für persistente Versteckung
                        server.execute(() -> {
                            if (player.networkHandler != null && bot.isAlive()) {
                                player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(bot.getId()));
                            }
                        });
                        
                        LOGGER.info("Hidden bot {} from player {}", bot.getName().getString(), playerName);
                    }
                }
            });
            
            // Verstecke den neuen Spieler vor anderen Spielern, falls er ein Bot ist
            if (player instanceof EntityPlayerMPFake && BOT_OWNERS.containsKey(playerName)) {
                // Verstecke den Bot vor allen anderen Spielern (außer Owner und OPs)
                server.execute(() -> {
                    for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
                        if (otherPlayer == player) continue;
                        
                        if (!canSeeBot(otherPlayer, player)) {
                            // Verstecke den neuen Bot vor diesem Spieler
                            otherPlayer.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(player.getId()));
                            
                            // Delayed packet für persistente Versteckung
                            server.execute(() -> {
                                if (otherPlayer.networkHandler != null && player.isAlive()) {
                                    otherPlayer.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(player.getId()));
                                }
                            });
                            
                            LOGGER.info("Hidden new bot {} from player {}", playerName, otherPlayer.getName().getString());
                        }
                    }
                });
            }
        });
        
        // Event-Listener für Bot-Disconnect (um Index-Bug zu beheben)
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity disconnectedPlayer = handler.getPlayer();
            String playerName = disconnectedPlayer.getName().getString();
            
            // Prüfe ob es sich um einen Bot handelt
            if (disconnectedPlayer instanceof EntityPlayerMPFake && BOT_OWNERS.containsKey(playerName)) {
                LOGGER.info("Bot {} disconnected - cleaning up", playerName);
                
                // Entferne Bot aus allen Maps
                BOT_OWNERS.remove(playerName);
                unregisterHiddenBot(disconnectedPlayer);
                
                LOGGER.info("Bot {} cleanup completed", playerName);
            }
        });
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("cbr")
            .then(literal("spawn")
                .executes(CarpetBotRestriction::spawnPlayerBot))
            .then(literal("despawn")
                .then(argument("target", StringArgumentType.string())
                    .suggests(BOT_AND_PLAYER_SUGGESTIONS)
                    .executes(CarpetBotRestriction::despawnTarget))
                .executes(CarpetBotRestriction::despawnOwnBot))
            .then(literal("config")
                .then(literal("set")
                    .then(literal("max_bots")
                        .then(argument("limit", StringArgumentType.string())
                            .executes(CarpetBotRestriction::setMaxBots))))
                .then(literal("get")
                    .then(literal("max_bots")
                        .executes(CarpetBotRestriction::getMaxBots)))
                .executes(CarpetBotRestriction::showConfig)));
    }

    private static int spawnPlayerBot(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        
        try {
            PlayerEntity player = source.getPlayerOrThrow();
            String playerName = player.getName().getString();
            
            boolean isOperator = source.hasPermissionLevel(4);
            int maxBots = config.getInt("max_bots_per_player", 1); // Standard: 1 Bot pro User
            
            if (!isOperator) {
                // Bereinige zuerst nicht mehr existierende Bots aus den Maps
                cleanupDisconnectedBots(server);
                
                int existingBots = 0;
                for (String ownerName : BOT_OWNERS.values()) {
                    if (ownerName.equals(playerName)) {
                        existingBots++;
                    }
                }
                
                if (existingBots >= maxBots) {
                    source.sendError(Text.literal("You have reached the maximum number of bots (" + maxBots + "). Use /cbr despawn to remove a bot first."));
                    return 0;
                }
            }
            
            String botName = generateBotName(playerName, server);
            
            boolean botCreated = EntityPlayerMPFake.createFake(
                botName, 
                server, 
                new Vec3d(player.getX(), player.getY(), player.getZ()), 
                player.getYaw(), 
                player.getPitch(), 
                server.getOverworld().getRegistryKey(), 
                null, 
                true);
            
            if (botCreated) {
                BOT_OWNERS.put(botName, playerName);
                LOGGER.info("Bot {} will be assigned to team automatically when it joins the game", botName);
                
                source.sendFeedback(() -> Text.literal("Bot '" + botName + "' spawned successfully!").formatted(Formatting.GREEN), false);
                LOGGER.info("Player {} spawned bot {}", playerName, botName);
                return 1;
            } else {
                source.sendError(Text.literal("Failed to spawn bot. Please try again."));
                return 0;
            }
            
        } catch (Exception e) {
            source.sendError(Text.literal("Error spawning bot: " + e.getMessage()));
            LOGGER.error("Error spawning bot", e);
            return 0;
        }
    }

    private static int despawnTarget(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        String target = getString(context, "target");
        
        try {
            PlayerEntity player = source.getPlayerOrThrow();
            String playerName = player.getName().getString();
            boolean isOperator = source.hasPermissionLevel(4);
            
            // Spezialfall: "ALL" - alle Bots eines Spielers despawnen
            if ("ALL".equalsIgnoreCase(target)) {
                return despawnAllBotsOfPlayer(context, playerName, isOperator);
            }
            
            // Prüfe ob target ein Bot-Name ist
            ServerPlayerEntity bot = server.getPlayerManager().getPlayer(target);
            if (bot != null && bot instanceof EntityPlayerMPFake) {
                return despawnSpecificBot(source, server, target, playerName, isOperator);
            }
            
            // Prüfe ob target ein Spieler-Name ist - despawne alle Bots dieses Spielers
            ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(target);
            if (targetPlayer != null) {
                return despawnAllBotsOfPlayer(context, target, isOperator);
            }
            
            source.sendError(Text.literal("Bot or player '" + target + "' not found."));
            return 0;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Error despawning: " + e.getMessage()));
            LOGGER.error("Error despawning target: " + target, e);
            return 0;
        }
    }
    
    private static int despawnSpecificBot(ServerCommandSource source, MinecraftServer server, String botName, String playerName, boolean isOperator) {
        try {
            ServerPlayerEntity bot = server.getPlayerManager().getPlayer(botName);
            if (bot == null || !(bot instanceof EntityPlayerMPFake)) {
                source.sendError(Text.literal("Bot '" + botName + "' not found."));
                return 0;
            }
            
            String botOwner = BOT_OWNERS.get(botName);
            
            if (!isOperator && !playerName.equals(botOwner)) {
                source.sendError(Text.literal("You don't have permission to despawn this bot."));
                return 0;
            }
            
            // Bot aus Team entfernen vor dem Despawnen
            removeBotFromTeam(server, bot);
            
            ((EntityPlayerMPFake) bot).kill(Text.literal("Despawned by " + playerName));
            BOT_OWNERS.remove(botName);
            
            source.sendFeedback(() -> Text.literal("Bot '" + botName + "' despawned successfully!").formatted(Formatting.GREEN), false);
            LOGGER.info("Player {} despawned bot {}", playerName, botName);
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Error despawning bot: " + e.getMessage()));
            LOGGER.error("Error despawning bot", e);
            return 0;
        }
    }
    
    private static int despawnAllBotsOfPlayer(CommandContext<ServerCommandSource> context, String targetPlayerName, boolean isOperator) {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        
        try {
            PlayerEntity player = source.getPlayerOrThrow();
            String executorName = player.getName().getString();
            
            // Sammle alle Bots des Ziel-Spielers
            List<String> botsToRemove = new ArrayList<>();
            for (Map.Entry<String, String> entry : BOT_OWNERS.entrySet()) {
                String botOwner = entry.getValue();
                if (botOwner.equals(targetPlayerName)) {
                    ServerPlayerEntity bot = server.getPlayerManager().getPlayer(entry.getKey());
                    if (bot != null && bot instanceof EntityPlayerMPFake) {
                        // Prüfe Berechtigung
                        if (isOperator || executorName.equals(targetPlayerName)) {
                            botsToRemove.add(entry.getKey());
                        }
                    }
                }
            }
            
            if (botsToRemove.isEmpty()) {
                if (targetPlayerName.equals(executorName)) {
                    source.sendError(Text.literal("You don't have any active bots to despawn."));
                } else {
                    source.sendError(Text.literal("Player '" + targetPlayerName + "' has no active bots or you don't have permission."));
                }
                return 0;
            }
            
            // Despawne alle gefundenen Bots
            final int[] despawnedCount = {0};
            for (String botName : botsToRemove) {
                ServerPlayerEntity bot = server.getPlayerManager().getPlayer(botName);
                if (bot != null) {
                    removeBotFromTeam(server, bot);
                    ((EntityPlayerMPFake) bot).kill(Text.literal("Despawned by " + executorName));
                    BOT_OWNERS.remove(botName);
                    despawnedCount[0]++;
                }
            }
            
            final int finalCount = despawnedCount[0];
            if (targetPlayerName.equals(executorName)) {
                source.sendFeedback(() -> Text.literal("Despawned " + finalCount + " of your bots.").formatted(Formatting.GREEN), false);
            } else {
                source.sendFeedback(() -> Text.literal("Despawned " + finalCount + " bots of player '" + targetPlayerName + "'.").formatted(Formatting.GREEN), false);
            }
            
            LOGGER.info("Player {} despawned {} bots of player {}", executorName, finalCount, targetPlayerName);
            return finalCount;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Error despawning bots: " + e.getMessage()));
            LOGGER.error("Error despawning all bots", e);
            return 0;
        }
    }

    private static int despawnOwnBot(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        
        try {
            PlayerEntity player = source.getPlayerOrThrow();
            String playerName = player.getName().getString();
            
            String[] botToRemoveArray = {null};
            for (Map.Entry<String, String> entry : BOT_OWNERS.entrySet()) {
                if (entry.getValue().equals(playerName)) {
                    ServerPlayerEntity bot = server.getPlayerManager().getPlayer(entry.getKey());
                    if (bot != null && bot instanceof EntityPlayerMPFake) {
                        botToRemoveArray[0] = entry.getKey();
                        break;
                    }
                }
            }
            
            final String botToRemove = botToRemoveArray[0];
            if (botToRemove == null) {
                source.sendError(Text.literal("You don't have any active bots to despawn."));
                return 0;
            }
            
            ServerPlayerEntity bot = server.getPlayerManager().getPlayer(botToRemove);
            
            // Bot aus Team entfernen vor dem Despawnen
            removeBotFromTeam(server, bot);
            
            ((EntityPlayerMPFake) bot).kill(Text.literal("Despawned by " + playerName));
            BOT_OWNERS.remove(botToRemove);
            
            source.sendFeedback(() -> Text.literal("Bot '" + botToRemove + "' despawned successfully!").formatted(Formatting.GREEN), false);
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Error despawning bot: " + e.getMessage()));
            LOGGER.error("Error despawning bot", e);
            return 0;
        }
    }

    private static String generateBotName(String playerName, MinecraftServer server) {
        int counter = 1;
        String baseName = "bot_" + playerName + "_";
        String botName;
        
        do {
            botName = baseName + counter;
            counter++;
        } while (server.getPlayerManager().getPlayer(botName) != null || BOT_OWNERS.containsKey(botName));
        
        return botName;
    }

    private static int setMaxBots(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String limitStr = getString(context, "limit");
        
        try {
            if (!source.hasPermissionLevel(4)) {
                source.sendError(Text.literal("You need operator privileges to modify bot limits."));
                return 0;
            }
            
            int limit = Integer.parseInt(limitStr);
            if (limit < 1) {
                source.sendError(Text.literal("Bot limit must be at least 1."));
                return 0;
            }
            
            config.set("max_bots_per_player", limit);
            source.sendFeedback(() -> Text.literal("Maximum bots per player set to: " + limit).formatted(Formatting.GREEN), false);
            LOGGER.info("Bot limit changed to {} by operator", limit);
            return 1;
            
        } catch (NumberFormatException e) {
            source.sendError(Text.literal("Invalid number: " + limitStr));
            return 0;
        } catch (Exception e) {
            source.sendError(Text.literal("Error setting bot limit: " + e.getMessage()));
            return 0;
        }
    }

    private static int getMaxBots(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            int maxBots = config.getInt("max_bots_per_player", 1);
            source.sendFeedback(() -> Text.literal("Current maximum bots per player: " + maxBots).formatted(Formatting.AQUA), false);
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Error getting bot limit: " + e.getMessage()));
            return 0;
        }
    }

    private static int showConfig(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            int maxBots = config.getInt("max_bots_per_player", 1);
            source.sendFeedback(() -> Text.literal("=== CBR Configuration ===").formatted(Formatting.GOLD), false);
            source.sendFeedback(() -> Text.literal("Max bots per player: " + maxBots).formatted(Formatting.YELLOW), false);
            source.sendFeedback(() -> Text.literal("Use '/cbr config set max_bots <number>' to change (OP only)").formatted(Formatting.GRAY), false);
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Error showing configuration: " + e.getMessage()));
            return 0;
        }
    }

    // Team Management für graue Namen und [BOT] Anzeige
    private static Team getBotTeam(MinecraftServer server) {
        try {
            Scoreboard scoreboard = server.getScoreboard();
            Team botTeam = scoreboard.getTeam(BOT_TEAM_NAME);
            
            if (botTeam == null) {
                // Team erstellen wie mit /team add
                botTeam = scoreboard.addTeam(BOT_TEAM_NAME);
                
                // Team-Eigenschaften setzen wie mit /team modify
                botTeam.setDisplayName(Text.literal("[BOT]").formatted(Formatting.GRAY));
                botTeam.setColor(Formatting.GRAY);
                botTeam.setPrefix(Text.literal("[BOT] ").formatted(Formatting.GRAY));
                botTeam.setSuffix(Text.literal("").formatted(Formatting.GRAY)); // Leeres Suffix
                
                // Team-Verhalten konfigurieren
                botTeam.setCollisionRule(Team.CollisionRule.NEVER); // Keine Kollision
                botTeam.setNameTagVisibilityRule(Team.VisibilityRule.ALWAYS); // Name immer sichtbar
                botTeam.setDeathMessageVisibilityRule(Team.VisibilityRule.NEVER); // Keine Todesnachrichten
                botTeam.setShowFriendlyInvisibles(false); // Keine unsichtbaren Teammitglieder zeigen
                
                LOGGER.info("Successfully created bot team '{}' with gray formatting and [BOT] prefix", BOT_TEAM_NAME);
            }
            
            return botTeam;
        } catch (Exception e) {
            LOGGER.error("Failed to create or get bot team: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private static void addBotToTeam(MinecraftServer server, ServerPlayerEntity bot) {
        try {
            if (bot == null || !(bot instanceof EntityPlayerMPFake)) {
                LOGGER.warn("Cannot add null or non-bot entity to team");
                return;
            }
            
            String botName = bot.getName().getString();
            Scoreboard scoreboard = server.getScoreboard();
            
            // Prüfe ob Bot bereits in einem Team ist und entferne ihn
            Team currentTeam = scoreboard.getScoreHolderTeam(botName);
            if (currentTeam != null) {
                LOGGER.debug("Bot {} is already in team {}, removing first", botName, currentTeam.getName());
                scoreboard.removeScoreHolderFromTeam(botName, currentTeam);
            }
            
            // Bot-Team erstellen falls es nicht existiert
            Team botTeam = getBotTeam(server);
            if (botTeam == null) {
                LOGGER.error("Could not create or get bot team");
                return;
            }
            
            // Bot zum Team hinzufügen (wie /team join)
            boolean success = scoreboard.addScoreHolderToTeam(botName, botTeam);
            
            if (success) {
                LOGGER.info("Successfully added bot '{}' to team '{}' - bot will now have gray name and [BOT] prefix", botName, BOT_TEAM_NAME);
                
                // Register bot for entity hiding (minimap/player locator)
                String ownerName = BOT_OWNERS.get(botName);
                if (ownerName != null) {
                    ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerName);
                    if (owner != null) {
                        registerHiddenBot(bot, owner);
                        LOGGER.info("Registered bot {} for entity hiding from minimap", botName);
                    } else {
                        LOGGER.warn("Could not find owner {} for bot {} - bot will not be hidden from entity tracking", ownerName, botName);
                    }
                } else {
                    LOGGER.warn("No owner found for bot {} - bot will not be hidden from entity tracking", botName);
                }
                
                // Zusätzliche Verifikation
                Team verifyTeam = scoreboard.getScoreHolderTeam(botName);
                if (verifyTeam != null && verifyTeam.getName().equals(BOT_TEAM_NAME)) {
                    LOGGER.info("Team assignment verified: bot {} is in team {}", botName, verifyTeam.getName());
                } else {
                    LOGGER.warn("Team assignment verification failed for bot {}", botName);
                }
            } else {
                LOGGER.error("Failed to add bot '{}' to team '{}'", botName, BOT_TEAM_NAME);
            }
            
        } catch (Exception e) {
            LOGGER.error("Exception while adding bot {} to team: {}", bot.getName().getString(), e.getMessage(), e);
        }
    }
    
    private static void removeBotFromTeam(MinecraftServer server, ServerPlayerEntity bot) {
        try {
            if (bot == null) {
                LOGGER.debug("Cannot remove null bot from team");
                return;
            }
            
            String botName = bot.getName().getString();
            Scoreboard scoreboard = server.getScoreboard();
            Team currentTeam = scoreboard.getScoreHolderTeam(botName);
            
            if (currentTeam != null) {
                scoreboard.removeScoreHolderFromTeam(botName, currentTeam);
                LOGGER.info("Successfully removed bot '{}' from team '{}'", botName, currentTeam.getName());
            } else {
                LOGGER.debug("Bot '{}' was not in any team", botName);
            }
            
            // Unregister bot from entity hiding system
            unregisterHiddenBot(bot);
            LOGGER.info("Unregistered bot {} from entity hiding system", botName);
            
        } catch (Exception e) {
            LOGGER.error("Exception while removing bot {} from team: {}", bot.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Check if a bot should be hidden from entity tracking
     */
    public static boolean isHiddenBot(ServerPlayerEntity bot) {
        return HIDDEN_BOTS.contains(bot.getUuid());
    }

    /**
     * Check if a player can see a specific bot
     */
    public static boolean canSeeBot(ServerPlayerEntity viewer, ServerPlayerEntity bot) {
        UUID botUuid = bot.getUuid();
        
        // Bot owners can always see their own bots
        UUID ownerId = BOT_OWNER_MAP.get(botUuid);
        if (ownerId != null && ownerId.equals(viewer.getUuid())) {
            return true;
        }
        
        // Operators at level 4 can see all bots
        return viewer.hasPermissionLevel(4);
    }

    /**
     * Register a bot to be hidden from entity tracking
     */
    private static void registerHiddenBot(ServerPlayerEntity bot, ServerPlayerEntity owner) {
        UUID botUuid = bot.getUuid();
        UUID ownerUuid = owner.getUuid();
        
        HIDDEN_BOTS.add(botUuid);
        BOT_OWNER_MAP.put(botUuid, ownerUuid);
        
        LOGGER.debug("Registered hidden bot {} owned by {}", bot.getName().getString(), owner.getName().getString());
    }

    /**
     * Unregister a bot from hidden entity tracking
     */
    public static void unregisterHiddenBot(ServerPlayerEntity bot) {
        UUID botUuid = bot.getUuid();
        
        HIDDEN_BOTS.remove(botUuid);
        BOT_OWNER_MAP.remove(botUuid);
        
        LOGGER.debug("Unregistered hidden bot {}", bot.getName().getString());
    }
    
    /**
     * Bereinigt Bot-Daten von nicht mehr existierenden Bots
     */
    private static void cleanupDisconnectedBots(MinecraftServer server) {
        // Erstelle eine Liste der zu entfernenden Bot-Namen
        List<String> botsToRemove = new ArrayList<>();
        
        for (String botName : BOT_OWNERS.keySet()) {
            ServerPlayerEntity bot = server.getPlayerManager().getPlayer(botName);
            if (bot == null || !(bot instanceof EntityPlayerMPFake)) {
                botsToRemove.add(botName);
            }
        }
        
        // Entferne die nicht mehr existierenden Bots
        for (String botName : botsToRemove) {
            BOT_OWNERS.remove(botName);
            LOGGER.info("Cleaned up disconnected bot: {}", botName);
        }
        
        // Bereinige auch die Entity-Hiding-Maps
        List<UUID> uuidsToRemove = new ArrayList<>();
        for (UUID botUuid : HIDDEN_BOTS) {
            ServerPlayerEntity bot = server.getPlayerManager().getPlayer(botUuid);
            if (bot == null) {
                uuidsToRemove.add(botUuid);
            }
        }
        
        for (UUID uuid : uuidsToRemove) {
            HIDDEN_BOTS.remove(uuid);
            BOT_OWNER_MAP.remove(uuid);
        }
    }
}
