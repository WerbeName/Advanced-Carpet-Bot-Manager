package carpetbotrestriction.mixin;

import carpet.commands.PlayerCommand;
import carpetbotrestriction.CarpetBotRestriction;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerCommand.class)
public class PlayerCommandMixin {

    /**
     * Before controlling/removing a bot, checks if the player originally spawned the bot.
     */
    @Inject(
            method = "cantManipulate",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/context/CommandContext;getSource()Ljava/lang/Object;"),
            cancellable = true,
            remap = false
    )
    private static void checkIfOwnsBot(@NotNull CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) PlayerEntity bot) {
        ServerCommandSource source = context.getSource();
        
        // Erlaube OPs alle Bots zu manipulieren
        if (source.hasPermissionLevel(4)) return;
        
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return;
        
        UUID botID = bot.getUuid();
        UUID playerID = player.getUuid();
        if (botID.equals(playerID)) return;
        
        // Prüfe ob der Bot dem Spieler gehört (DarkCows-style)
        ObjectOpenHashSet<UUID> botList = CarpetBotRestriction.PLAYERS.get(playerID);
        if (botList == null || !botList.contains(botID)) {
            source.sendError(net.minecraft.text.Text.literal("You cannot manipulate this bot, it belongs to another player."));
            CarpetBotRestriction.LOGGER.debug("Prevented {} from manipulating {}.", player.getName(), bot.getName());
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    /**
     * Before spawning a new bot, check if the player will have more bots than the limited amount
     */
    @Inject(
            method = "cantSpawn",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void checkExistingBot(CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Boolean> cir) {
        ServerCommandSource source = context.getSource();
        
        // Erlaube OPs unlimited Bots
        if (source.hasPermissionLevel(4)) return;
        
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return;
        
        UUID playerID = player.getUuid();
        ObjectOpenHashSet<UUID> botList = CarpetBotRestriction.PLAYERS.get(playerID);
        int playerBotLimit = CarpetBotRestriction.getConfig().getInt("max_bots_per_player", 1);
        
        if (botList != null && botList.size() >= playerBotLimit) {
            source.sendError(net.minecraft.text.Text.literal("You cannot have more than " + playerBotLimit + " bots."));
            CarpetBotRestriction.LOGGER.debug("Prevented {} from spawning new bot: Limit is {} bots.", player.getName().getString(), playerBotLimit);
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    /**
     * Add a new bot to the list to keep track of which player owns which bot
     */
    @Inject(
            method = "spawn",
            at = @At("HEAD"),
            remap = false
    )
    private static void trackSpawnSource(@NotNull CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Integer> cir) {
        // Track which player tried to spawn bot
        CarpetBotRestriction.CREATE_BOT_SOURCE = context.getSource();
    }
}