package com.shushuwonie.clairvoyance.client.gui.openback;

import com.shushuwonie.clairvoyance.screen.OtherPlayerInventoryScreenHandler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class OtherPlayerInventoryScreen extends HandledScreen<OtherPlayerInventoryScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("clairvoyance", "textures/gui/img_1.png");
    private static final int TEXTURE_WIDTH = 256;   // 根据你的纹理实际尺寸修改
    private static final int TEXTURE_HEIGHT = 276;

    public OtherPlayerInventoryScreen(OtherPlayerInventoryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 276;
        this.titleX = 8;
        this.titleY = 8;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.x, this.y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}