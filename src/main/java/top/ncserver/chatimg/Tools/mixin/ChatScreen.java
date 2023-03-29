package top.ncserver.chatimg.Tools.mixin;


import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(net.minecraft.client.gui.screen.ChatScreen.class)
public abstract class ChatScreen extends Screen {
    @Shadow
    ChatInputSuggestor chatInputSuggestor;
    @Shadow
    private String originalChatText;

    public ChatScreen(String p_95579_) {
        super(Text.translatable("chat_screen.title"));
        this.originalChatText = p_95579_;
    }


    /**
     * @author MakesYT
     * @reason 原版一次滚动太多, 将Shift行为对换
     */
    @Overwrite
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        amount = MathHelper.clamp(amount, -1.0, 1.0);
        if (this.chatInputSuggestor.mouseScrolled(amount)) {
            return true;
        } else {
            if (hasShiftDown()) {
                amount *= 7.0;
            }

            this.client.inGameHud.getChatHud().scroll((int) amount);
            return true;
        }
    }


}
