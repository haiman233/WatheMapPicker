package net.nebula.wathemappicker.client.mixin;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.nebula.wathemappicker.client.gui.screen.MapVotingScreen;
import net.nebula.wathemappicker.client.gui.widget.MapVotingWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LimitedInventoryScreen.class)
public abstract class MapVotingMixin extends LimitedHandledScreen<PlayerScreenHandler> {

    public MapVotingMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    void renderGuidebookButton(CallbackInfo ci) {
        MapVotingWidget child = new MapVotingWidget(null, this.width / 2 - 15, 10, Items.PAPER.getDefaultStack(), button -> MinecraftClient.getInstance().setScreen(new MapVotingScreen()), "nebula.map_voting.tooltip");
        this.addDrawableChild(child);
    }
}