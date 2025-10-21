package su.sergiusonesimus.metaworlds.zmixin.mixins.codechickenlib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.server.management.PlayerManager.PlayerInstance;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import codechicken.lib.packet.PacketCustom;
import su.sergiusonesimus.metaworlds.entity.player.EntityPlayerProxy;

@Mixin(PacketCustom.class)
public class MixinPacketCustom {

    private static PlayerManager storedPlayerManager;

    @Inject(
        method = "sendToChunk(Lnet/minecraft/network/Packet;Lnet/minecraft/world/World;II)V",
        remap = false,
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/management/ServerConfigurationManager;playerEntityList:Ljava/util/List;",
            shift = Shift.BEFORE),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private static void storePlayerManager(Packet packet, World world, int chunkX, int chunkZ, CallbackInfo ci,
        @Local(name = "playerManager") PlayerManager playerManager) {
        storedPlayerManager = playerManager;
    }

    @SuppressWarnings("unchecked")
    @WrapOperation(
        method = "sendToChunk(Lnet/minecraft/network/Packet;Lnet/minecraft/world/World;II)V",
        remap = false,
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/management/ServerConfigurationManager;playerEntityList:Ljava/util/List;"))
    private static List<EntityPlayerMP> getPlayersList(ServerConfigurationManager instance,
        Operation<List<EntityPlayerMP>> original) {
        return storedPlayerManager.players;
    }

    @WrapOperation(
        method = "sendToChunk(Lnet/minecraft/network/Packet;Lnet/minecraft/world/World;II)V",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/management/PlayerManager;isPlayerWatchingChunk(Lnet/minecraft/entity/player/EntityPlayerMP;II)Z"))
    private static boolean isPlayerWatchingChunk(PlayerManager instance, EntityPlayerMP player, int x, int z,
        Operation<Boolean> original) {
        if (player instanceof EntityPlayerProxy) {
            Method getOrCreateChunkWatcher;
            try {
                getOrCreateChunkWatcher = instance.getClass()
                    .getDeclaredMethod("getOrCreateChunkWatcher", int.class, int.class, boolean.class);
                getOrCreateChunkWatcher.setAccessible(true);
                PlayerManager.PlayerInstance playerinstance = (PlayerInstance) getOrCreateChunkWatcher
                    .invoke(instance, x, z, false);
                return playerinstance != null && playerinstance.playersWatchingChunk.contains(player);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
                e.printStackTrace();
                return false;
            }
        } else return original.call(instance, player, x, z);
    }

    @WrapOperation(
        method = "sendToPlayer(Lnet/minecraft/network/Packet;Lnet/minecraft/entity/player/EntityPlayer;)V",
        remap = false,
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/player/EntityPlayerMP;playerNetServerHandler:Lnet/minecraft/network/NetHandlerPlayServer;"))
    private static NetHandlerPlayServer getPlayerNetServerHandler(EntityPlayerMP player,
        Operation<NetHandlerPlayServer> original) {
        return original.call(player instanceof EntityPlayerProxy playerProxy ? playerProxy.getRealPlayer() : player);
    }

}
