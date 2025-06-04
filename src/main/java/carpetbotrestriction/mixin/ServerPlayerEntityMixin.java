package carpetbotrestriction.mixin;

import carpetbotrestriction.CarpetBotRestriction;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique
    private static boolean initialized = false;

    // Track UUIDs of created players
    @Inject(
            method = "<init>",
            at = @At("CTOR_HEAD")
    )
    private void detectRealPlayerJoin(CallbackInfo ci) {
        CarpetBotRestriction.REAL_PLAYERS.add(super.getUuid());
        if (!initialized) {
            CarpetBotRestriction.saveRealPlayerList.onServerStopping(this.getServer());
            initialized = true;
        }
    }
}
