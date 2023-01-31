package me.x150.mixin;

import me.x150.ul.HtmlScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.apache.commons.io.FileUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    private static HtmlScreen inst;
    private static HtmlScreen inst1;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    void postTitle(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("ULTRALIGHT: Google"), button -> {
			if (inst == null) {
				inst = new HtmlScreen("https://google.com");
			}
            this.client.setScreen(inst);
        }).dimensions(5, 5, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("ULTRALIGHT: Local file"), button -> {
            if (inst1 == null) {
                try {
                    FileUtils.copyDirectory(new File("../demo"), new File("demo"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                inst1 = new HtmlScreen("file:///demo/index.html");
            }
            inst1.reload();
            this.client.setScreen(inst1);
        }).dimensions(5, 30, 100, 20).build());
    }
}