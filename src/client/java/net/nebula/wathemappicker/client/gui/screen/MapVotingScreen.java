package net.nebula.wathemappicker.client.gui.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.nebula.wathemappicker.MapVotePlayerComponent;
import net.nebula.wathemappicker.client.gui.widget.MapVotingWidget;
import net.nebula.wathemappicker.packet.MapVoteC2SPacket;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

import static net.nebula.wathemappicker.client.WathemappickerClient.dimensions;

public class MapVotingScreen extends Screen {

    private final MinecraftClient client = MinecraftClient.getInstance();

    private final int itemSize = 32;
    private final int padding = 10;

    public static Map<MapVotingWidget, String> widgetDimensions = new HashMap<>();

    public MapVotingScreen() {
        super(Text.literal("Custom Dimensions"));
    }

    @Override
    protected void init() {
        super.init();
        widgetDimensions.clear();

        // Calculate total width for centering
        int totalWidth = dimensions.size() * itemSize + (dimensions.size() - 1) * padding;
        int startX = (this.width - totalWidth) / 2;
        int y = this.height / 2 - itemSize / 2;

        for (int i = 0; i < dimensions.size(); i++) {
            String dim = dimensions.get(i);
            int x = startX + i * (itemSize + padding);

            // Example ItemStack: you can customize per dimension
            ItemStack item = Items.PAPER.getDefaultStack();

            MapVotingWidget widget = new MapVotingWidget(
                    null, // Pass screen if needed, otherwise null
                    x,
                    y,
                    item,
                    btn -> {
                        ClientPlayNetworking.send(new MapVoteC2SPacket(dim));
                        System.out.println("Voted for dimension: " + dim);
                    },
                    "nebula.map_voting." + dim.substring(dim.indexOf(":") + 1)
            );

            this.addDrawableChild(widget);
            widgetDimensions.put(widget, dim);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Draw vote counts over widgets
        if (client.player != null && client.player.networkHandler != null) {
            Map<String, Integer> voteCounts = new HashMap<>();
            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                if (player == null) continue;

                String vote = MapVotePlayerComponent.KEY.get(player).voteTarget;
                if (!vote.isEmpty() && !dimensions.contains(vote)) {
                    MapVotePlayerComponent.KEY.get(player).voteTarget = "";
                }

                if (!vote.isEmpty()) {
                    voteCounts.put(vote, voteCounts.getOrDefault(vote, 0) + 1);
                }
            }


            for (Map.Entry<MapVotingWidget, String> entry : widgetDimensions.entrySet()) {
                MapVotingWidget widget = entry.getKey();
                String dim = entry.getValue();
                int votes = voteCounts.getOrDefault(dim, 0);

                int textX = widget.getX() - (client.textRenderer.getWidth(String.valueOf(votes)) / 2) + widget.getWidth() + 3;
                int textY = widget.getY() + widget.getHeight() + 2; // below the widget

                context.drawText(client.textRenderer, String.valueOf(votes), textX, textY, 0xFFFFFF, false);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_E) {
            this.client.setScreen(null);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
