package top.ncserver.chatimg.Tools.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import top.ncserver.chatimg.ChatImgClient;
import top.ncserver.chatimg.Tools.Img;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(net.minecraft.client.gui.hud.ChatHud.class)
public abstract class ChatComponent extends DrawableHelper {
    @Shadow
    protected abstract boolean isChatHidden();

    @Shadow
    public abstract int getVisibleLineCount();

    @Shadow
    @Final
    private List<ChatHudLine.Visible> visibleMessages;
    @Shadow
    @Final
    private List<ChatHudLine> messages;

    @Shadow
    protected abstract boolean isChatFocused();

    @Shadow
    public abstract double getChatScale();

    @Shadow
    public abstract int getWidth();

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    protected abstract int getLineHeight();

    @Shadow
    private int scrolledLines;

    @Shadow
    private static double getMessageOpacityMultiplier(int pCounter) {
        double d0 = (double) pCounter / 200.0D;
        d0 = 1.0D - d0;
        d0 *= 10.0D;
        d0 = MathHelper.clamp(d0, 0.0D, 1.0D);
        return d0 * d0;
    }

    @Shadow
    protected abstract int getIndicatorX(ChatHudLine.Visible line);

    @Shadow
    protected abstract void drawIndicatorIcon(MatrixStack matrices, int x, int y, MessageIndicator.Icon icon);

    @Shadow
    private boolean hasUnreadNewMessages;

    @Shadow
    public abstract int getHeight();

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void scroll(int scroll) {
        this.scrolledLines += scroll;
        int i = this.visibleMessages.size();
        if (this.scrolledLines > i - 3) {
            this.scrolledLines = i - 3;
        }

        if (this.scrolledLines <= 0) {
            this.scrolledLines = 0;
            this.hasUnreadNewMessages = false;
        }

    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern patternP = Pattern.compile("\\[ImgID=(.+)\\]");

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void render(MatrixStack matrices, int currentTick) {
        if (!this.isChatHidden()) {
            int i = this.getVisibleLineCount();
            int j = this.visibleMessages.size();
            if (j > 0) {
                boolean flag = this.isChatFocused();
                float f = (float) this.getChatScale();
                int k = MathHelper.ceil((float) this.getWidth() / f);
                matrices.push();
                matrices.translate(4.0D, 8.0D, 0.0D);
                matrices.scale(f, f, 1.0F);
                double d0 = this.client.options.getChatOpacity().getValue() * (double) 0.9F + (double) 0.1F;
                double d1 = this.client.options.getTextBackgroundOpacity().getValue();
                double d2 = this.client.options.getChatLineSpacing().getValue();
                int l = this.getLineHeight();
                double d3 = -8.0D * (d2 + 1.0D) + 4.0D * d2;
                int i1 = 0;
                int indexY = (int) ((double) (-this.getVisibleLineCount()) * getLineHeight()) + getHeight();
                for (int u = 0; ; u++) {
                    if (indexY <= -getHeight() || u > this.visibleMessages.size() - 1 || u + this.scrolledLines > this.messages.size() - 1) {
                        break;
                    }
                    ChatHudLine.Visible guimessage$line = this.visibleMessages.get(u + this.scrolledLines);
                    if (guimessage$line != null) {
                        int k1 = currentTick - guimessage$line.addedTime();
                        if (k1 < 200 || flag) {
                            double d4 = flag ? 1.0D : getMessageOpacityMultiplier(k1);
                            int i2 = (int) (255.0D * d4 * d0);
                            int j2 = (int) (255.0D * d4 * d1);
                            ++i1;
                            if (i2 > 3) {
                                if (this.messages.get(u + this.scrolledLines).content().getString().contains("[ImgID=")) {
                                    Matcher matcher = patternP.matcher(this.messages.get(u + this.scrolledLines).content().getString());
                                    int imgID = -1;
                                    try {
                                        if (matcher.find()) {
                                            imgID = Integer.parseInt((matcher.group(0)).split("=")[1].replace("]", ""));
                                        }
                                        Img img = ChatImgClient.imgMap.get(imgID);
                                        if (img.allReceived()) {
                                            matrices.push();
                                            matrices.translate(0.0D, 0.0D, 50.0D);
                                            fill(matrices, -4, indexY, k + 4 + 4, indexY - img.getHeight(), j2 << 24);
                                            RenderSystem.enableBlend();
                                            //pPoseStack.translate(0.0D, 0.0D, 50.0D);
                                            //this.mc.fontRenderer.drawTextWithShadow(p_238492_1_, chatline.getLineString(), 0.0F, (float)((int)(d6 + d4)), 16777215 + (l1 << 24));
                                            //ChatImg.LOGGER.debug(String.valueOf(imgID));
                                            RenderSystem.setShaderTexture(0, new Identifier("chatimg", "imgs/" + imgID));
                                            RenderSystem.setShaderFogColor(0.7F, 0.7F, 0.7F, 0.7F);
                                            //this.minecraft.getTextureManager().bindForSetup(F);
                                            drawTexture(matrices, 0, indexY - img.getHeight(), 0, 0, img.getWidth(), img.getHeight(), img.getWidth(), img.getHeight());
                                            //
                                            matrices.pop();
                                            RenderSystem.disableBlend();
                                            indexY -= img.getHeight();


                                            matrices.push();
                                            matrices.translate(0.0D, 0.0D, 50.0D);
                                            fill(matrices, -4, indexY, k + 4 + 4, indexY - 9, j2 << 24);
                                            MessageIndicator guimessagetag = guimessage$line.indicator();

                                            if (guimessagetag != null) {
                                                int j3 = guimessagetag.indicatorColor() | i2 << 24;
                                                fill(matrices, -4, indexY - 9, -2, indexY + img.getHeight(), j3);
                                                if (flag && guimessage$line.endOfEntry() && guimessagetag.icon() != null) {
                                                    int k3 = this.getIndicatorX(guimessage$line);
                                                    this.drawIndicatorIcon(matrices, k3, indexY, guimessagetag.icon());
                                                }
                                            }

                                            RenderSystem.enableBlend();
                                            matrices.translate(0.0D, 0.0D, 50.0D);
                                            this.client.textRenderer.drawWithShadow(matrices, guimessage$line.content(), 0.0F, (float) indexY - 9, 16777215 + (i2 << 24));
                                            RenderSystem.disableBlend();
                                            matrices.pop();
                                            indexY -= 9;


                                        }
                                    } catch (Exception e) {
                                        matrices.push();
                                        matrices.translate(0.0D, 0.0D, 50.0D);
                                        fill(matrices, -4, indexY, k + 4 + 4, indexY - 9, j2 << 24);
                                        MessageIndicator guimessagetag = guimessage$line.indicator();

                                        if (guimessagetag != null) {
                                            int j3 = guimessagetag.indicatorColor() | i2 << 24;
                                            fill(matrices, -4, indexY - 9, -2, indexY - 9, j3);
                                            if (flag && guimessage$line.endOfEntry() && guimessagetag.icon() != null) {
                                                int k3 = this.getIndicatorX(guimessage$line);
                                                this.drawIndicatorIcon(matrices, k3, indexY, guimessagetag.icon());
                                            }
                                        }

                                        RenderSystem.enableBlend();
                                        matrices.translate(0.0D, 0.0D, 50.0D);
                                        this.client.textRenderer.drawWithShadow(matrices, guimessage$line.content(), 0.0F, (float) indexY - 9, 16777215 + (i2 << 24));
                                        RenderSystem.disableBlend();
                                        matrices.pop();
                                        indexY -= 9;
                                    }
                                } else {
                                    matrices.push();
                                    matrices.translate(0.0D, 0.0D, 50.0D);
                                    fill(matrices, -4, indexY, k + 4 + 4, indexY - 9, j2 << 24);
                                    MessageIndicator guimessagetag = guimessage$line.indicator();

                                    if (guimessagetag != null) {
                                        int j3 = guimessagetag.indicatorColor() | i2 << 24;
                                        fill(matrices, -4, indexY - 9, -2, indexY - 9, j3);
                                        if (flag && guimessage$line.endOfEntry() && guimessagetag.icon() != null) {
                                            int k3 = this.getIndicatorX(guimessage$line);
                                            this.drawIndicatorIcon(matrices, k3, indexY, guimessagetag.icon());
                                        }
                                    }

                                    RenderSystem.enableBlend();
                                    matrices.translate(0.0D, 0.0D, 50.0D);
                                    this.client.textRenderer.drawWithShadow(matrices, guimessage$line.content(), 0.0F, (float) indexY - 9, 16777215 + (i2 << 24));
                                    RenderSystem.disableBlend();
                                    matrices.pop();
                                    indexY -= 9;
                                }


                            }
                        }
                    }
                }

                long i4 = this.client.getMessageHandler().getUnprocessedMessageCount();
                if (i4 > 0L) {
                    int j4 = (int) (128.0D * d0);
                    int l4 = (int) (255.0D * d1);
                    matrices.push();
                    matrices.translate(0.0D, 0.0D, 50.0D);
                    fill(matrices, -2, 0, k + 4, 9, l4 << 24);
                    RenderSystem.enableBlend();
                    matrices.translate(0.0D, 0.0D, 50.0D);
                    this.client.textRenderer.drawWithShadow(matrices, Text.translatable("chat.queue", i4), 0.0F, 1.0F, 16777215 + (j4 << 24));
                    matrices.pop();
                    RenderSystem.disableBlend();
                }

                if (flag) {
                    int k4 = this.getLineHeight();
                    int i5 = j * k4;
                    int l1 = i1 * k4;
                    int j5 = this.scrolledLines * l1 / j;
                    int k5 = l1 * l1 / i5;
                    if (i5 != l1) {
                        int l5 = j5 > 0 ? 170 : 96;
                        int i6 = this.hasUnreadNewMessages ? 13382451 : 3355562;
                        int j6 = k + 4;
                        fill(matrices, j6, -j5, j6 + 2, -j5 - k5, i6 + (l5 << 24));
                        fill(matrices, j6 + 2, -j5, j6 + 1, -j5 - k5, 13421772 + (l5 << 24));
                    }
                }

                matrices.pop();
            }
        }
    }
}
