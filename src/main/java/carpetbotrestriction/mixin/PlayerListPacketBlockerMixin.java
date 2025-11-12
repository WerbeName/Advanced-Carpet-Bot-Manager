package carpetbotrestriction.mixin;

import carpetbotrestriction.CarpetBotRestriction;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Critical fix: Prevent PlayerListS2CPacket creation for fake players
 * This directly addresses the encoding error at the source
 */
@Mixin(PlayerManager.class)
public class PlayerListPacketBlockerMixin {

    /**
     * Block all packet sending methods that involve PlayerListS2CPacket for bots
     */
    @Inject(method = "sendToAll(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void blockSendToAllPlayerListPackets(net.minecraft.network.packet.Packet<?> packet, CallbackInfo ci) {
        if (shouldBlockPlayerListPacket(packet)) {
            ci.cancel();
        }
    }

    /**
     * Central method to check if a PlayerListS2CPacket should be blocked
     */
    private boolean shouldBlockPlayerListPacket(net.minecraft.network.packet.Packet<?> packet) {
        if (packet instanceof PlayerListS2CPacket playerListPacket) {
            try {
                var entries = playerListPacket.getEntries();
                if (entries != null) {
                    for (var entry : entries) {
                        String playerName = entry.profile().name();
                        
                        // If this involves any of our bots, block the entire packet
                        if (CarpetBotRestriction.BOT_OWNERS.containsKey(playerName)) {
                            CarpetBotRestriction.LOGGER.info("BLOCKED PlayerListS2CPacket involving bot: {} - preventing encoding error", playerName);
                            return true;
                        }
                        
                        // Also block any names that start with [BOT] (our new bot format)
                        if (playerName.startsWith("[BOT] bot_")) {
                            CarpetBotRestriction.LOGGER.info("BLOCKED PlayerListS2CPacket involving bot name: {} - preventing encoding error", playerName);
                            return true;
                        }
                        
                        // Legacy: Also block any ultra-short names that might be old bots
                        if (playerName.length() <= 5 && playerName.matches("\\w{2,3}\\d+")) {
                            CarpetBotRestriction.LOGGER.info("BLOCKED PlayerListS2CPacket involving suspected legacy bot: {} - preventing encoding error", playerName);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                // If we can't analyze the packet safely, be conservative and block it
                CarpetBotRestriction.LOGGER.warn("Could not analyze PlayerListS2CPacket, blocking to be safe: {}", e.getMessage());
                return true;
            }
        }
        return false;
    }
}