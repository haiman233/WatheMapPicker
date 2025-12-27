package net.nebula.wathemappicker;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static net.nebula.wathemappicker.Wathemappicker.SERVER_INSTANCE;
import static net.nebula.wathemappicker.Wathemappicker.Vec3dToBlockPos;


public class CommandRegistration {
    public static String currentDimension = "";

    public static void register() {
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            commandDispatcher.register(
                    CommandManager.literal("wathe:setMap")
                            .then(CommandManager.argument("dimension", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        ctx.getSource().getServer().getWorldRegistryKeys().stream()
                                                .map(RegistryKey::getValue)
                                                .filter(id -> !isVanillaDimension(id))
                                                .forEach(id -> builder.suggest(id.getPath()));
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        setMap(ctx);
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .requires(source -> source.hasPermissionLevel(2))
            );

        }));
    }

    public static boolean isVanillaDimension(Identifier id) {
        return id.equals(World.OVERWORLD.getValue()) || id.equals(World.NETHER.getValue()) || id.equals(World.END.getValue());
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

    public static void setMap(CommandContext<ServerCommandSource> ctx) {
        String path = StringArgumentType.getString(ctx, "dimension");

        RegistryKey<World> worldKey = getWorldByPath(ctx.getSource().getServer(), path);
        ServerWorld world = ctx.getSource().getServer().getWorld(worldKey);

        if (worldKey == null || world == null) {
            ctx.getSource().sendError(Text.literal("Unknown dimension: " + path));
            return;
        }

        MapVariablesWorldComponent spawn = MapVariablesWorldComponent.KEY.get(world);
        MapVariablesWorldComponent.PosWithOrientation spawnPos = spawn.getSpawnPos();
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            // Teleport each player to the spawnPos
            player.teleport(
                    world, // target world from the component
                    spawnPos.pos.getX() + 0.5, // center in the block
                    spawnPos.pos.getY(),
                    spawnPos.pos.getZ() + 0.5,
                    spawnPos.yaw,
                    spawnPos.pitch
            );
        }
        currentDimension = path;


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

    public static boolean setMap(MinecraftServer server, String dimensionPath) {
        RegistryKey<World> worldKey = getWorldByPath(server, dimensionPath);
        if (worldKey == null) {
            return false;
        }

        ServerWorld world = server.getWorld(worldKey);
        if (world == null) {
            return false;
        }

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
        }
        currentDimension = dimensionPath;
        return true;
    }

    public static boolean teleportPlayer(ServerPlayerEntity player) {
        RegistryKey<World> worldKey = getWorldByPath(SERVER_INSTANCE, currentDimension);

        if (worldKey == null) return false;

        ServerWorld world = SERVER_INSTANCE.getWorld(worldKey);
        if (world == null) return false;

        MapVariablesWorldComponent spawn = MapVariablesWorldComponent.KEY.get(world);

        MapVariablesWorldComponent.PosWithOrientation spawnPos = spawn.getSpawnPos();

        player.teleport(
                world,
                spawnPos.pos.getX() + 0.5,
                spawnPos.pos.getY(),
                spawnPos.pos.getZ() + 0.5,
                spawnPos.yaw,
                spawnPos.pitch
        );
        return true;
    }

}
