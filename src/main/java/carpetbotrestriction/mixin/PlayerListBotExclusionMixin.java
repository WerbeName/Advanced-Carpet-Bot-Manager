package carpetbotrestriction.mixin;

import carpetbotrestriction.CarpetBotRestriction;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevent bots from being added to player list packets entirely
 */
@Mixin(PlayerManager.class)
public class PlayerListBotExclusionMixin {

    /**
     * Prevent bots from being included in player list updates
     */
    @Inject(method = "sendPlayerStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"), cancellable = true)
    private void preventBotPlayerStatus(ServerPlayerEntity player, CallbackInfo ci) {
        // If this is a bot, don't send status updates
        if (player instanceof EntityPlayerMPFake) {
            String playerName = player.getName().getString();
            if (CarpetBotRestriction.BOT_OWNERS.containsKey(playerName) || playerName.startsWith("[BOT] bot_")) {
                CarpetBotRestriction.LOGGER.info("PREVENTED bot {} from player status update - avoiding packet issues", playerName);
                ci.cancel();
            }
        }
    }
}