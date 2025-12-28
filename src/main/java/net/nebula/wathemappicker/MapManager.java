package net.nebula.wathemappicker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.nebula.wathemappicker.CommandRegistration.isVanillaDimension;

public class MapManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    public static String currentDimension = "";


    public static void setMap(CommandContext<ServerCommandSource> ctx) {
        String path = StringArgumentType.getString(ctx, "dimension");
        setMap(ctx.getSource().getServer(), path);
    }

    public static void setMap(MinecraftServer server, String dimensionPath) {
        RegistryKey<World> worldKey = getWorldByPath(server, dimensionPath);
        if (worldKey == null) {
            return;
        }

        ServerWorld world = server.getWorld(worldKey);
        if (world == null) {
            return;
        }

        GameFunctions.finalizeGame(world);

        MapVariablesWorldComponent spawn = MapVariablesWorldComponent.KEY.get(world);

        MapVariablesWorldComponent.PosWithOrientation spawnPos = spawn.getSpawnPos();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.teleport(
                    world,
                    spawnPos.pos.getX() + 0.5,
                    spawnPos.pos.getY(),
                    spawnPos.pos.getZ() + 0.5,
                    spawnPos.yaw,
                    spawnPos.pitch
            );
            player.changeGameMode(GameMode.ADVENTURE);
            TrainVoicePlugin.resetPlayer(player.getUuid());
            player.getInventory().clear();
        }


        currentDimension = dimensionPath;
        saveCurrentDimension(server);
    }

    public static void teleportPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();

        SCHEDULER.schedule(() -> {
            if (server == null) return;
            server.execute(() -> {
                RegistryKey<World> worldKey = getWorldByPath(server, currentDimension);
                if (worldKey == null) return;

                ServerWorld world = server.getWorld(worldKey);
                if (world == null) return;

                MapVariablesWorldComponent spawn = MapVariablesWorldComponent.KEY.get(world);
                MapVariablesWorldComponent.PosWithOrientation spawnPos = spawn.getSpawnPos();

                world.getChunk(Wathemappicker.Vec3dToBlockPos(spawnPos.pos));

                player.teleport(
                        world,
                        spawnPos.pos.getX() + 0.5,
                        spawnPos.pos.getY() + 1,
                        spawnPos.pos.getZ() + 0.5,
                        spawnPos.yaw,
                        spawnPos.pitch
                );
                TrainVoicePlugin.resetPlayer(player.getUuid());
                player.getInventory().clear();
            });
        }, 100, TimeUnit.MILLISECONDS);
    }


    @Nullable
    public static RegistryKey<World> getWorldByPath(MinecraftServer server, String path) {
        for (RegistryKey<World> key : server.getWorldRegistryKeys()) {
            Identifier id = key.getValue();
            if (!isVanillaDimension(id) && id.getPath().equals(path)) {
                return key;
            }
        }
        return null;
    }

    public static void saveCurrentDimension(MinecraftServer server) {
        if (server == null || currentDimension == null || currentDimension.isEmpty()) {
            return;
        }

        Path path = server.getSavePath(WorldSavePath.ROOT)
                .resolve("wathemappicker.json");

        JsonObject json = new JsonObject();
        json.addProperty("currentDimension", currentDimension);

        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadCurrentDimension(MinecraftServer server) {
        Path path = server.getSavePath(WorldSavePath.ROOT)
                .resolve("wathemappicker.json");

        if (!Files.exists(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null && json.has("currentDimension")) {
                currentDimension = json.get("currentDimension").getAsString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
