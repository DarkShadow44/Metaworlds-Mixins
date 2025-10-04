package su.sergiusonesimus.metaworlds.zmixin.mixins.minecraft.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S05PacketSpawnPosition;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import su.sergiusonesimus.metaworlds.EventHookContainer;
import su.sergiusonesimus.metaworlds.api.SubWorld;
import su.sergiusonesimus.metaworlds.api.SubWorldTypeManager;
import su.sergiusonesimus.metaworlds.api.SubWorldTypeManager.SubWorldInfoProvider;
import su.sergiusonesimus.metaworlds.zmixin.interfaces.minecraft.entity.IMixinEntity;
import su.sergiusonesimus.metaworlds.zmixin.interfaces.minecraft.entity.player.IMixinEntityPlayer;
import su.sergiusonesimus.metaworlds.zmixin.interfaces.minecraft.network.play.PacketHandler;
import su.sergiusonesimus.metaworlds.zmixin.interfaces.minecraft.network.play.server.IMixinS05PacketSpawnPosition;
import su.sergiusonesimus.metaworlds.zmixin.interfaces.minecraft.network.play.server.IMixinS08PacketPlayerPosLook;
import su.sergiusonesimus.metaworlds.zmixin.interfaces.minecraft.network.play.server.IMixinS14PacketEntity;
import su.sergiusonesimus.metaworlds.zmixin.interfaces.minecraft.network.play.server.IMixinS18PacketEntityTeleport;
import su.sergiusonesimus.metaworlds.zmixin.interfaces.minecraft.world.IMixinWorld;
import su.sergiusonesimus.metaworlds.zmixin.interfaces.minecraft.world.storage.IMixinWorldInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Shadow(remap = true)
    public WorldClient clientWorldController;

    @Shadow(remap = true)
    public Minecraft gameController;

    @Shadow(remap = true)
    public NetworkManager netManager;

    @Shadow(remap = true)
    public boolean doneLoadingTerrain;

    // TODO

    @Redirect(
        method = "handleEntityTeleport",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setPositionAndRotation2(DDDFFI)V"))
    private void handleEntityTeleportSetPositionAndRotation2(Entity entity, double x, double y, double z, float yaw,
        float pitch, int rotationIncrements, @Local S18PacketEntityTeleport packetIn) {
        if (((IMixinS18PacketEntityTeleport) packetIn).getSendSubWorldPosFlag() != 0) {
            if (((IMixinS18PacketEntityTeleport) packetIn).getXPosOnSubWorld()
                != ((IMixinEntity) entity).getServerPosXOnSubWorld()
                || ((IMixinS18PacketEntityTeleport) packetIn).getYPosOnSubWorld()
                    != ((IMixinEntity) entity).getServerPosYOnSubWorld()
                || ((IMixinS18PacketEntityTeleport) packetIn).getZPosOnSubWorld()
                    != ((IMixinEntity) entity).getServerPosZOnSubWorld()) {
                ((IMixinEntity) entity)
                    .setServerPosXOnSubWorld(((IMixinS18PacketEntityTeleport) packetIn).getXPosOnSubWorld());
                ((IMixinEntity) entity)
                    .setServerPosYOnSubWorld(((IMixinS18PacketEntityTeleport) packetIn).getYPosOnSubWorld());
                ((IMixinEntity) entity)
                    .setServerPosZOnSubWorld(((IMixinS18PacketEntityTeleport) packetIn).getZPosOnSubWorld());

                Vec3 transformedPos = ((IMixinWorld) ((IMixinEntity) entity).getWorldBelowFeet()).transformLocalToOther(
                    entity.worldObj,
                    (double) ((IMixinEntity) entity).getServerPosXOnSubWorld() / 32.0D,
                    (double) ((IMixinEntity) entity).getServerPosYOnSubWorld() / 32.0D,
                    (double) ((IMixinEntity) entity).getServerPosZOnSubWorld() / 32.0D);
                x = transformedPos.xCoord;
                y = transformedPos.yCoord;
                z = transformedPos.zCoord;
                entity.setPositionAndRotation2(x, y, z, yaw, pitch, 1);
            }
        } else entity.setPositionAndRotation2(x, y, z, yaw, pitch, 3);
    }

    @Redirect(
        method = "handleEntityMovement",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setPositionAndRotation2(DDDFFI)V"))
    private void handleEntityMovementSetPositionAndRotation2(Entity entity, double x, double y, double z, float yaw,
        float pitch, int rotationIncrements, @Local S14PacketEntity packetIn) {
        if (((IMixinS14PacketEntity) packetIn).getSendSubWorldPosFlag() != 0) {
            if (((IMixinS14PacketEntity) packetIn).getXPosDiffOnSubWorld() != 0
                || ((IMixinS14PacketEntity) packetIn).getYPosDiffOnSubWorld() != 0
                || ((IMixinS14PacketEntity) packetIn).getZPosDiffOnSubWorld() != 0) {
                ((IMixinEntity) entity).setServerPosXOnSubWorld(
                    ((IMixinEntity) entity).getServerPosXOnSubWorld()
                        + ((IMixinS14PacketEntity) packetIn).getXPosDiffOnSubWorld());
                ((IMixinEntity) entity).setServerPosYOnSubWorld(
                    ((IMixinEntity) entity).getServerPosYOnSubWorld()
                        + ((IMixinS14PacketEntity) packetIn).getYPosDiffOnSubWorld());
                ((IMixinEntity) entity).setServerPosZOnSubWorld(
                    ((IMixinEntity) entity).getServerPosZOnSubWorld()
                        + ((IMixinS14PacketEntity) packetIn).getZPosDiffOnSubWorld());
                Vec3 transformedPos = ((IMixinWorld) ((IMixinEntity) entity).getWorldBelowFeet()).transformLocalToOther(
                    entity.worldObj,
                    (double) ((IMixinEntity) entity).getServerPosXOnSubWorld() / 32.0D,
                    (double) ((IMixinEntity) entity).getServerPosYOnSubWorld() / 32.0D,
                    (double) ((IMixinEntity) entity).getServerPosZOnSubWorld() / 32.0D);
                x = transformedPos.xCoord;
                y = transformedPos.yCoord;
                z = transformedPos.zCoord;
                entity.setPositionAndRotation2(x, y, z, yaw, pitch, 1);
            }
        } else entity.setPositionAndRotation2(x, y, z, yaw, pitch, 3);
    }

    @Redirect(
        method = "handlePlayerPosLook",
        at = @At(value = "NEW", target = "Lnet/minecraft/network/play/client/C03PacketPlayer$C06PacketPlayerPosLook;"))
    private C06PacketPlayerPosLook getC06PacketPlayerPosLook(double x, double minY, double y, double z, float yaw,
        float pitch, boolean isOnGround, @Local S08PacketPlayerPosLook packetIn,
        @Local EntityClientPlayerMP entityclientplayermp) {
        int subWorldID = ((IMixinS08PacketPlayerPosLook) packetIn).getSubWorldBelowFeetID();
        World subworld = ((IMixinWorld) ((IMixinWorld) entityclientplayermp.worldObj).getParentWorld())
            .getSubWorldsMap()
            .get(subWorldID);
        if (subworld == null && subWorldID != 0) {
            SubWorldInfoProvider provider = SubWorldTypeManager.getSubWorldInfoProvider(
                SubWorldTypeManager.getTypeByID(((IMixinS08PacketPlayerPosLook) packetIn).getSubWorldBelowFeetType()));
            subworld = provider.create(entityclientplayermp.worldObj, subWorldID);
        }
        ((IMixinEntity) entityclientplayermp).setWorldBelowFeet(subworld);

        World worldBelowFeet = ((IMixinEntity) entityclientplayermp).getWorldBelowFeet();

        return PacketHandler.getC06PacketPlayerPosLook(
            x,
            minY,
            y,
            z,
            yaw,
            pitch,
            isOnGround,
            ((IMixinWorld) worldBelowFeet).getSubWorldID(),
            ((IMixinEntity) entityclientplayermp).getTractionLossTicks(),
            ((IMixinEntity) entityclientplayermp).isLosingTraction());
    }

    @Inject(method = "handleSpawnPosition", at = @At(value = "TAIL"))
    public void handleSpawnPosition(S05PacketSpawnPosition packetIn, CallbackInfo ci) {
        int spawnWorld = ((IMixinS05PacketSpawnPosition) packetIn).getSpawnWorldID();
        ((IMixinEntityPlayer) this.gameController.thePlayer).setSpawnWorldID(spawnWorld);
        ((IMixinWorldInfo) this.gameController.theWorld.getWorldInfo()).setRespawnWorldID(spawnWorld);
        if (spawnWorld != 0) {
            World worldBelowFeet = ((IMixinWorld) ((IMixinWorld) this.gameController.thePlayer.worldObj)
                .getParentWorld()).getSubWorld(spawnWorld);
            if (worldBelowFeet != null) ((SubWorld) worldBelowFeet).registerEntityToDrag(this.gameController.thePlayer);
            else EventHookContainer.registerSubworldEvent(
                spawnWorld,
                subworld -> { subworld.registerEntityToDrag(this.gameController.thePlayer); });
        }
    }

}
