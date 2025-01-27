package carpetbotrestriction.mixin;

import carpetbotrestriction.CarpetBotRestriction;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void logDisconnect(DisconnectionInfo info, CallbackInfo ci) {
        String botName = this.player.getName().getString();
        String player = CarpetBotRestriction.BOTS.remove(botName);
        CarpetBotRestriction.LOGGER.info("Removing {} from {}", botName, player);
        if (botName == null || player == null) return;
        CarpetBotRestriction.PLAYERS.get(player).remove(botName);
    }
}
