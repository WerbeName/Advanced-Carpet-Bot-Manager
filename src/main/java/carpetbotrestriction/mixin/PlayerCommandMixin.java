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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.LinkedHashSet;

@Mixin(PlayerCommand.class)
public class PlayerCommandMixin {

    @Inject(method = "cantManipulate", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private static void checkIfOwnsBot(CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Boolean> cir) {
        if (context.getSource().hasPermissionLevel(3)) return;
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerPlayerEntity bot = CarpetBotRestriction.getBot(context);
        if (bot == null || player == null) return;
        if (bot.equals(player)) return;
        HashSet<String> botList = CarpetBotRestriction.PLAYERS.get(player.getName().getString());
        if (botList == null || !botList.contains(bot.getName().getString())) {
            CarpetBotRestriction.say(player, "You cannot manipulate this bot!");
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "cantSpawn", at = @At("HEAD"), cancellable = true, remap = false)
    private static void checkExistingBot(CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return;
        HashSet<String> botList = CarpetBotRestriction.PLAYERS.get(player.getName().getString());
        if (botList != null && botList.size() >= 2 && !context.getSource().hasPermissionLevel(3)) {
            CarpetBotRestriction.say(player,"You cannot have more than 2 bots.");
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyExpressionValue(method = "spawn", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;createFake(Ljava/lang/String;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/util/math/Vec3d;DDLnet/minecraft/registry/RegistryKey;Lnet/minecraft/world/GameMode;Z)Z"))
    private static boolean addPlayer(boolean original, CommandContext<ServerCommandSource> context, @Local String playerName) {
        if (!original) return false;
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return true;
        String realPlayer = player.getName().getString();
        if (!CarpetBotRestriction.PLAYERS.containsKey(realPlayer)) {
            CarpetBotRestriction.PLAYERS.put(realPlayer, new LinkedHashSet<>());
        }
        CarpetBotRestriction.PLAYERS.get(realPlayer).add(playerName);
        CarpetBotRestriction.BOTS.put(playerName, realPlayer);
        return true;
    }

}
