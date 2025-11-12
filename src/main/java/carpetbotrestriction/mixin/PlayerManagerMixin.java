package carpetbotrestriction.mixin;

import carpetbotrestriction.CarpetBotRestriction;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import carpet.patches.EntityPlayerMPFake;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Mixin to fix packet encoding issues by preventing problematic operations on fake players
 * This addresses the core issue from the DarkCows investigation
 */
@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    /**
     * Ensure proper cleanup when players are removed
     */
    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerRemove(ServerPlayerEntity player, CallbackInfo ci) {
        if (player instanceof EntityPlayerMPFake) {
            String playerName = player.getName().getString();
            UUID playerUUID = player.getUuid();
            
            CarpetBotRestriction.LOGGER.info("Removing fake player {} - performing comprehensive cleanup", playerName);
            
            // Remove from all tracking systems
            CarpetBotRestriction.BOT_OWNERS.remove(playerName);
            CarpetBotRestriction.HIDDEN_BOTS.remove(playerUUID);
            CarpetBotRestriction.BOT_OWNER_MAP.remove(playerUUID);
            
            // Clean up DarkCows tracking
            var ownedBots = CarpetBotRestriction.PLAYERS.remove(playerUUID);
            if (ownedBots != null) {
                for (UUID botUUID : ownedBots) {
                    CarpetBotRestriction.BOTS.remove(botUUID);
                }
            }
            
            // Remove from bot tracking if this was a bot
            UUID owner = CarpetBotRestriction.BOTS.remove(playerUUID);
            if (owner != null) {
                var ownerBots = CarpetBotRestriction.PLAYERS.get(owner);
                if (ownerBots != null) {
                    ownerBots.remove(playerUUID);
                }
            }
            
            CarpetBotRestriction.LOGGER.debug("Comprehensive cleanup completed for fake player {}", playerName);
        }
    }
}