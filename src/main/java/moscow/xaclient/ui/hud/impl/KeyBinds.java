package moscow.xaclient.ui.hud.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.UIContext;
import moscow.xaclient.framework.msdf.Font;
import moscow.xaclient.framework.msdf.Fonts;
import moscow.xaclient.systems.modules.Module;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.ui.hud.HudList;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.animation.base.Easing;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.TextUtility;
import moscow.xaclient.utility.gui.GuiUtility;
import moscow.xaclient.utility.render.batching.Batching;
import moscow.xaclient.utility.render.batching.impl.FontBatching;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.VertexFormats;

public class KeyBinds extends HudList {
   int lastSize = -1;
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");

   public KeyBinds() {
      super("hud.keybinds", "icons/hud/keybinds.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = 92.0F;
      this.height = 18.0F;

      for (Module module : XaClient.getInstance().getModuleManager().getModules()) {
         boolean forward = module.isEnabled() && module.getKey() != -1;
         module.getKeybindsAnimation().update(forward);
         module.getKeybindsAnimation().setEasing(Easing.BAKEK);
         if (module.getKeybindsAnimation().getValue() > 0.0F) {
            this.width = Math.max(Fonts.REGULAR.getFont(7.0F).width(module.getName() + TextUtility.getKeyName(module.getKey())) + 20.0F, this.width);
         }

         this.height = this.height + 18.0F * module.getKeybindsAnimation().getValue();
      }

      if (this.height > 18.0F) {
         this.height += 5.0F;
      }

      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      Font font = Fonts.REGULAR.getFont(7.0F);
      List<Module> modules = new ArrayList<>(XaClient.getInstance().getModuleManager().getModules());
      if (this.lastSize == modules.size()) {
         modules.sort(Comparator.comparingDouble(m -> font.width(m.getName())));
         this.lastSize = modules.size();
      }

      float offset = 22.0F;
      super.renderComponent(context);

      for (Module module : modules) {
         Animation anim = module.getKeybindsAnimation();
         if (anim.getValue() != 0.0F && offset != 22.0F) {
            float off = -4.5F + 4.5F * anim.getValue();
            context.drawRect(this.x, this.y + offset + off, this.width, 0.5F, Colors.getTextColor().withAlpha(5.1F));
            offset += 18.0F * anim.getValue();
         }
      }

      Batching fontBatching = new FontBatching(VertexFormats.POSITION_TEXTURE_COLOR, font.getFont());
      offset = 22.0F;

      for (Module modulex : modules) {
         Animation anim = modulex.getKeybindsAnimation();
         if (anim.getValue() != 0.0F) {
            float off = -4.5F + 4.5F * anim.getValue();
            context.drawText(
               font,
               modulex.getName(),
               this.x + 7.0F * anim.getValue(),
               this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F),
               Colors.getTextColor().withAlpha(255.0F * anim.getValue())
            );
            context.drawRightText(
               font,
               TextUtility.getKeyName(modulex.getKey()),
               this.x + this.width - 7.0F * anim.getValue(),
               this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F),
               Colors.getTextColor().withAlpha(255.0F * anim.getValue())
            );
            offset += 18.0F * anim.getValue();
         }
      }

      fontBatching.draw();
   }

   @Override
   public boolean show() {
      return !XaClient.getInstance().getModuleManager().getModules().stream().filter(module -> module.isEnabled() && module.getKey() != -1).toList().isEmpty()
         || mc.currentScreen instanceof ChatScreen
         || this.alwaysDisplay.isEnabled();
   }
}
