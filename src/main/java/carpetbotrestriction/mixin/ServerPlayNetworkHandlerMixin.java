package carpetbotrestriction.mixin;

import carpetbotrestriction.CarpetBotRestriction;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import carpet.patches.EntityPlayerMPFake;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    /**
     * Alternative approach: Block problematic packets at the network level
     * Since we can't easily find the right sendPacket method, let's focus on cleanup
     */

    /**
     * Clean up bot tracking when a player disconnects
     */
    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void cleanupBotTracking(CallbackInfo ci) {
        if (player == null) return;

        UUID playerUUID = player.getUuid();
        String playerName = player.getName().getString();
        
        // Enhanced cleanup for fake players
        if (player instanceof EntityPlayerMPFake) {
            CarpetBotRestriction.LOGGER.info("Fake player {} disconnected, performing enhanced cleanup", playerName);
            
            // Clean up from CBR tracking
            if (CarpetBotRestriction.BOT_OWNERS.containsKey(playerName)) {
                CarpetBotRestriction.BOT_OWNERS.remove(playerName);
                CarpetBotRestriction.LOGGER.info("Removed bot {} from CBR tracking", playerName);
            }
        }
        
        // Clean up DarkCows-style tracking
        ObjectOpenHashSet<UUID> botList = CarpetBotRestriction.PLAYERS.remove(playerUUID);
        if (botList != null) {
            for (UUID botUUID : botList) {
                CarpetBotRestriction.BOTS.remove(botUUID);
            }
            CarpetBotRestriction.LOGGER.debug("Cleaned up {} bots for disconnecting player {}", 
                botList.size(), playerName);
        }

        // Check if this player was a bot in DarkCows tracking
        UUID botOwner = CarpetBotRestriction.BOTS.remove(playerUUID);
        if (botOwner != null) {
            ObjectOpenHashSet<UUID> ownerBots = CarpetBotRestriction.PLAYERS.get(botOwner);
            if (ownerBots != null) {
                ownerBots.remove(playerUUID);
            }
            CarpetBotRestriction.LOGGER.debug("Cleaned up bot {} from DarkCows tracking", playerName);
        }

        // Clean up traditional tracking
        CarpetBotRestriction.HIDDEN_BOTS.remove(playerUUID);
        CarpetBotRestriction.BOT_OWNER_MAP.remove(playerUUID);
        
        CarpetBotRestriction.LOGGER.debug("Player {} disconnected - cleaned up tracking data", playerName);
    }
}