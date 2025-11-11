package carpetbotrestriction.mixin;

import carpet.patches.EntityPlayerMPFake;
import carpetbotrestriction.CarpetBotRestriction;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.entity.Entity;

@Mixin(ServerPlayerEntity.class)
public class EntityTrackerEntryMixin {
    
    /**
     * Verhindert dass Bots von anderen Spielern getrackt werden
     * Injiziert in shouldTrackPlayer um Bot-Sichtbarkeit zu kontrollieren
     */
    @Inject(method = "shouldTrack", at = @At("HEAD"), cancellable = true)
    private void carpetBotRestriction$preventBotTracking(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity thisPlayer = (ServerPlayerEntity) (Object) this;
        
        // Prüfe ob das zu trackende Entity ein Bot ist
        if (entity instanceof EntityPlayerMPFake bot) {
            // Verwende die vorhandene Logik aus CarpetBotRestriction
            if (CarpetBotRestriction.isHiddenBot(bot) && !CarpetBotRestriction.canSeeBot(thisPlayer, bot)) {
                // Bot soll von diesem Spieler nicht getrackt werden
                cir.setReturnValue(false);
                CarpetBotRestriction.LOGGER.debug("Blocked tracking of bot {} for player {}", 
                    bot.getName().getString(), thisPlayer.getName().getString());
            }
        }
    }
}