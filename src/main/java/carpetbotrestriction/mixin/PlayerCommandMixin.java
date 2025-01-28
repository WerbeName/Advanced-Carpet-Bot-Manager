package carpetbotrestriction.mixin;

import carpet.commands.PlayerCommand;
import carpetbotrestriction.CarpetBotRestriction;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.LinkedHashSet;

@Mixin(PlayerCommand.class)
public class PlayerCommandMixin {

    /**
     * Before controlling/removing a bot, checks if the player originally spawned the bot.
     */
    @Inject(
            method = "cantManipulate",
            at = @At(value = "HEAD"),
            cancellable = true,
            remap = false
    )
    private static void checkIfOwnsBot(CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Boolean> cir) {
        if (context.getSource().hasPermissionLevel(3)) return;
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity bot = CarpetBotRestriction.getBot(context);
        if (bot == null || player == null) return;
        if (bot.equals(player)) return;
        HashSet<String> botList = CarpetBotRestriction.PLAYERS.get(player.getName().getString().toLowerCase());
        if (botList == null || !botList.contains(bot.getName().getString().toLowerCase())) {
            CarpetBotRestriction.say(player, "You cannot manipulate this bot!");
            CarpetBotRestriction.LOGGER.debug("Prevented {} from manipulating {}.", player.getName().getString(), bot.getName().getString());
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
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return;
        HashSet<String> botList = CarpetBotRestriction.PLAYERS.get(player.getName().getString().toLowerCase());
        if (botList != null && botList.size() >= CarpetBotRestriction.MAX_BOTS && !context.getSource().hasPermissionLevel(3)) {
            CarpetBotRestriction.say(player,String.format("You cannot have more than %d bots.", CarpetBotRestriction.MAX_BOTS));
            CarpetBotRestriction.LOGGER.debug("Prevented {} from spawning new bot: Limit is {} bots.", player.getName().getString(), CarpetBotRestriction.MAX_BOTS);
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    /**
     * Before shadowing, check if the player already is at the bot limit (shadowing creates another bot)
     */
    @Inject(
            method = "shadow",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void canShadow(CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Integer> cir) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return;
        HashSet<String> botList = CarpetBotRestriction.PLAYERS.get(player.getName().getString().toLowerCase());
        if (botList != null && botList.size() >= CarpetBotRestriction.MAX_BOTS && !context.getSource().hasPermissionLevel(3)) {
            CarpetBotRestriction.say(player,String.format("You cannot have more than %d bots. Shadowing will create another bot.", CarpetBotRestriction.MAX_BOTS));
            CarpetBotRestriction.LOGGER.debug("Prevented {} from shadowing: Limit is {} bots.", player.getName().getString(), CarpetBotRestriction.MAX_BOTS);
            cir.setReturnValue(0);
            cir.cancel();
        }
    }

    /**
     * Add a new bot to the list to keep track of which player owns which bot
     */
    @ModifyExpressionValue(
            method = "spawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lcarpet/patches/EntityPlayerMPFake;createFake(Ljava/lang/String;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/util/math/Vec3d;DDLnet/minecraft/registry/RegistryKey;Lnet/minecraft/world/GameMode;Z)Z"
            )
    )
    private static boolean addPlayer(boolean original, CommandContext<ServerCommandSource> context, @Local String playerName) {
        if (!original) return false;
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return true;
        String realPlayerName = player.getName().getString().toLowerCase();
        if (!CarpetBotRestriction.PLAYERS.containsKey(realPlayerName)) {
            CarpetBotRestriction.PLAYERS.put(realPlayerName, new LinkedHashSet<>());
        }
        CarpetBotRestriction.PLAYERS.get(realPlayerName).add(playerName.toLowerCase());
        CarpetBotRestriction.BOTS.put(playerName.toLowerCase(), realPlayerName);
        return true;
    }
}
