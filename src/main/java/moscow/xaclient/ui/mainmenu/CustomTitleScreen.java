package moscow.xaclient.ui.mainmenu;

import java.util.ArrayList;
import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.CustomScreen;
import moscow.xaclient.framework.base.UIContext;
import moscow.xaclient.framework.msdf.Font;
import moscow.xaclient.framework.msdf.Fonts;
import moscow.xaclient.framework.objects.BorderRadius;
import moscow.xaclient.framework.objects.MouseButton;
import moscow.xaclient.framework.objects.gradient.impl.VerticalGradient;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.systems.modules.modules.other.Sounds;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.animation.base.Easing;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.game.TextUtility;
import moscow.xaclient.utility.interfaces.IMinecraft;
import moscow.xaclient.utility.math.MathUtility;
import moscow.xaclient.utility.render.DrawUtility;
import moscow.xaclient.utility.render.RenderUtility;
import moscow.xaclient.utility.render.obj.Rect;
import moscow.xaclient.utility.sounds.ClientSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;

public class CustomTitleScreen extends CustomScreen implements IMinecraft {
   private static boolean once;
   private static final List<CustomButton> buttons = new ArrayList<>();
   private boolean active;
   private final Animation activeAnimation = new Animation(1000L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final ColorRGBA dateColor = new ColorRGBA(171.0F, 254.0F, 255.0F);
   private final ColorRGBA timeColor = new ColorRGBA(203.0F, 254.0F, 255.0F);

   @Compile
   @VMProtect(type = VMProtectType.MUTATION)
   protected void init() {
      String basePath = "image/mainmenu/icons/";
      if (!once) {
         if (XaClient.getInstance().getModuleManager().getModule(Sounds.class).isEnabled()) {
            ClientSounds.WELCOME.play(XaClient.getInstance().getModuleManager().getModule(Sounds.class).getVolume().getCurrentValue());
         }

         buttons.add(new CustomButton(basePath + "single.png", 12.0F, () -> mc.setScreen(new SelectWorldScreen(this))));
         buttons.add(new CustomButton(basePath + "multi.png", 12.0F, () -> mc.setScreen(new MultiplayerScreen(this))));
         buttons.add(new CustomButton(basePath + "settings.png", 12.0F, () -> mc.setScreen(new OptionsScreen(this, mc.options))));
         buttons.add(new CustomButton(basePath + "quit.png", 14.0F, mc::stop));
         once = true;
      }

      super.init();
   }

   @Override
   public void render(UIContext context) {
      Font timeFont = Fonts.ROUND_BOLD.getFont(65.0F);
      Font dateFont = Fonts.MEDIUM.getFont(16.0F);
      Font unlockFont = Fonts.REGULAR.getFont(10.0F);
      float textAlpha = 255.0F * (0.5F + 0.5F * this.activeAnimation.getValue());
      float timeOffset = MathUtility.interpolate(this.height / 2.0F - 20.0F, 80.0, this.activeAnimation.getValue());
      Rect rect = new Rect(-this.width / 2.0F, -this.width / 3.0F, this.width * 1.5F, this.width);
      this.activeAnimation.update(this.active);
      context.drawRoundedRect(
         0.0F, 0.0F, this.width, this.height, BorderRadius.ZERO, new VerticalGradient(new ColorRGBA(26.0F, 34.0F, 56.0F), new ColorRGBA(5.0F, 3.0F, 12.0F))
      );
      RenderUtility.scale(context.getMatrices(), this.width / 2.0F, this.height / 2.0F, 1.1F - 0.1F * this.activeAnimation.getValue());
      context.drawTexture(XaClient.id("image/mainmenu/background.png"), rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
      RenderUtility.end(context.getMatrices());
      context.drawCenteredText(dateFont, TextUtility.getFormattedDate(), this.width / 2.0F, timeOffset - 23.0F, ColorRGBA.WHITE.withAlpha(textAlpha));
      context.drawCenteredText(timeFont, TextUtility.getCurrentTime(), this.width / 2.0F, timeOffset, ColorRGBA.WHITE.withAlpha(textAlpha));
      context.drawRoundedRect(
         this.width / 2.0F - 36.0F,
         this.height - 5 - 3.0F * this.activeAnimation.getValue(),
         72.0F,
         3.0F,
         BorderRadius.all(1.0F),
         ColorRGBA.WHITE.withAlpha(255.0F * this.activeAnimation.getValue())
      );
      context.drawCenteredText(
         unlockFont,
         Localizator.translate("mainmenu.next"),
         this.width / 2.0F,
         this.height - 15 + 3.0F * this.activeAnimation.getValue(),
         ColorRGBA.WHITE.withAlpha(155.0F * (1.0F - this.activeAnimation.getValue()))
      );
      DrawUtility.blurProgram.draw();
      float offset = 0.0F;
      float buttonsWidth = buttons.size() * 30.0F + (buttons.size() - 1) * 6.0F;

      for (CustomButton button : buttons) {
         button.getActiveAnim().update(buttons.size() - buttons.indexOf(button) > (1.0F - this.activeAnimation.getValue()) * buttons.size() + 0.5F);
         button.set(
            this.width / 2.0F - buttonsWidth / 2.0F + offset,
            (this.height > 500 ? this.height / 2.0F : this.height / 1.25F) - 5.0F - 10.0F * button.getActiveAnim().getValue(),
            30.0F,
            30.0F
         );
         offset += button.getWidth() + 6.0F;
         button.draw(context);
      }

      if (this.shouldShowIsland()) {
         XaClient.getInstance().getHud().getIsland().render(context);
      }
   }

   @Compile
   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (!this.shouldShowIsland() || !XaClient.getInstance().getHud().getIsland().handleClick((float)mouseX, (float)mouseY, button.getButtonIndex())) {
         for (CustomButton customButton : buttons) {
            if (customButton.hovered(mouseX, mouseY) && customButton.getActiveAnim().getValue() == 1.0F) {
               customButton.click(mouseX, mouseY, button.getButtonIndex());
               return;
            }
         }

         this.active = !this.active;
         super.onMouseClicked(mouseX, mouseY, button);
      }
   }

   @Compile
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 69) {
         XaClient.getInstance().getThemeManager().switchTheme();
      }

      if (Screen.hasControlDown() && keyCode == 82) {
         MinecraftClient.getInstance().setScreen(new MultiplayerScreen(this));
      }

      if (Screen.hasControlDown() && keyCode == 84) {
         MinecraftClient.getInstance().setScreen(new SelectWorldScreen(this));
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private boolean shouldShowIsland() {
      return XaClient.getInstance().getMusicTracker().haveActiveSession();
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }
}
