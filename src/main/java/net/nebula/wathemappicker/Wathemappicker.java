package net.nebula.wathemappicker;

import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.nebula.wathemappicker.packet.DimensionsS2CPacket;
import net.nebula.wathemappicker.packet.MapVoteC2SPacket;

import java.util.Objects;
import java.util.stream.Collectors;

import static net.nebula.wathemappicker.CommandRegistration.*;

public class Wathemappicker implements ModInitializer {

    public static MinecraftServer SERVER_INSTANCE;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER_INSTANCE = server);

        CommandRegistration.register();
        registerPackets();
        registerListeners();


        GameEvents.ON_GAME_STOP.register(gameMode -> {
            String win = MapVoteC2SPacket.getWinningMap();
            if (win == null) return;
            if (!Objects.equals(currentDimension, win)) {
                String path =  win.substring(win.indexOf(":") + 1);
                CommandRegistration.setMap(SERVER_INSTANCE, path);
                // Get spawn position from your MapVariablesWorldComponent
                MapVariablesWorldComponent spawnComponent = MapVariablesWorldComponent.KEY.get(SERVER_INSTANCE.getWorld(CommandRegistration.getWorldByPath(SERVER_INSTANCE, path)));
                MapVariablesWorldComponent.PosWithOrientation spawnPos = spawnComponent.getSpawnPos();

                // Update respawn position for all players
                for (ServerPlayerEntity player : SERVER_INSTANCE.getPlayerManager().getPlayerList()) {
                    player.setSpawnPoint(
                            CommandRegistration.getWorldByPath(SERVER_INSTANCE, path),                           // World
                            Vec3dToBlockPos(spawnPos.pos),                            // BlockPos
                            spawnPos.yaw,                            // yaw
                            true,                                    // force spawn
                            false                                    // keepBedSpawn
                    );
                }
            }
        });
    }

    public static Identifier id(String path) {
        return Identifier.of("wathemappicker", path);
    }

    public static void registerListeners() {
        ServerPlayConnectionEvents.JOIN.register(((serverPlayNetworkHandler, packetSender, minecraftServer) -> {
            ServerPlayNetworking.send(serverPlayNetworkHandler.getPlayer(), new DimensionsS2CPacket(getDimensionString(minecraftServer)));
            MapVoteC2SPacket.removeVote(serverPlayNetworkHandler.getPlayer());

            if (GameWorldComponent.KEY.get(serverPlayNetworkHandler.getPlayer().getWorld()).isRunning()) {
                if (!Objects.equals(CommandRegistration.currentDimension, serverPlayNetworkHandler.getPlayer().getWorld().getRegistryKey().getValue().toString())) {
                    teleportPlayer(serverPlayNetworkHandler.getPlayer());
                }
            }
        }));

        ServerPlayConnectionEvents.DISCONNECT.register(((serverPlayNetworkHandler, minecraftServer) -> {
            MapVoteC2SPacket.removeVote(serverPlayNetworkHandler.getPlayer());
        }));
    }

    public static void registerPackets() {
        PayloadTypeRegistry.playS2C().register(DimensionsS2CPacket.ID, DimensionsS2CPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(MapVoteC2SPacket.ID, MapVoteC2SPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(MapVoteC2SPacket.ID, new MapVoteC2SPacket.Receiver());
    }

    public static String getDimensionString(MinecraftServer server) {
        return server.getWorldRegistryKeys().stream()
                        .map(RegistryKey::getValue)
                        .filter(id -> !isVanillaDimension(id))
                        .map(Identifier::toString)
                        .collect(Collectors.joining(","));

    }

    public static BlockPos Vec3dToBlockPos(Vec3d vec3d) {
        return new BlockPos(
                (int) Math.round(vec3d.x),
                (int) Math.round(vec3d.y),
                (int) Math.round(vec3d.z)
        );

    }
}
