package com.shushuwonie.clairvoyance.client.gui.bodyback;

import com.shushuwonie.clairvoyance.screen.BodyPartScreenHandler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BodyPartScreen extends HandledScreen<BodyPartScreenHandler> {
    // 原版通用容器纹理（9x3 槽位背景）
        private static final Identifier TEXTURE = Identifier.of("clairvoyance", "textures/gui/img_2.png");
//    private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/gui/container/generic_54.png");
    // 纹理的实际宽度和高度（原版为 176x166）
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public BodyPartScreen(BodyPartScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // 设置背景大小应与纹理区域一致
        this.backgroundWidth = 256;
        this.backgroundHeight = 256;
        // 设置标题和背包标题的坐标（相对于 GUI 左上角）
        this.titleX = 8+20;
        this.titleY = 6+20;
        this.playerInventoryTitleX = 8 + 20;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2+20;
        int y = (this.height - this.backgroundHeight) / 2+20;
        // 使用 drawTexture，指定纹理完整尺寸
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0,
                this.backgroundWidth, this.backgroundHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
//        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
    }

//    @Override
//    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
//        // 使用白色带阴影的文字
//        context.drawTextWithShadow(this.textRenderer, this.title, this.titleX, this.titleY, 0xFFFFFF);
////        context.drawTextWithShadow(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0xFFFFFF);
//    }
}