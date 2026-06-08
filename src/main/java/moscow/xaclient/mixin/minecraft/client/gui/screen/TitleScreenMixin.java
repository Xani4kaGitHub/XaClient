package moscow.xaclient.mixin.minecraft.client.gui.screen;

import java.util.ArrayList;
import moscow.xaclient.mixin.accessors.ScreenAccessor;
import moscow.xaclient.ui.mainmenu.AltManagerScreen;
import moscow.xaclient.ui.mainmenu.ProxyScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
   @Inject(method = "addNormalWidgets", at = @At("RETURN"))
   private void replaceRealmsButton(int y, int spacingY, CallbackInfoReturnable<Integer> cir) {
      TitleScreen screen = (TitleScreen)(Object)this;
      ScreenAccessor accessor = (ScreenAccessor)screen;
      String realmsText = Text.translatable("menu.online").getString();

      for (Element element : new ArrayList<>(accessor.getChildren())) {
         if (element instanceof ButtonWidget button && button.getMessage().getString().equals(realmsText)) {
            accessor.invokeRemove(element);
         }
      }

      accessor.invokeAddDrawableChild(
         ButtonWidget.builder(Text.literal("Alt Manager"), button -> MinecraftClient.getInstance().setScreen(new AltManagerScreen(screen)))
            .dimensions(screen.width / 2 - 100, cir.getReturnValue(), 98, 20)
            .build()
      );

      accessor.invokeAddDrawableChild(
         ButtonWidget.builder(Text.literal("Proxy"), button -> MinecraftClient.getInstance().setScreen(new ProxyScreen(screen)))
            .dimensions(screen.width / 2 + 2, cir.getReturnValue(), 98, 20)
            .build()
      );
   }
}
