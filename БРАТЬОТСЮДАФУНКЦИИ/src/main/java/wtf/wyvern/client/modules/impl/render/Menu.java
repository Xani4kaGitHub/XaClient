package wtf.wyvern.client.modules.impl.render;

import wtf.wyvern.Wyvern;
import wtf.wyvern.client.modules.api.Category;
import wtf.wyvern.client.modules.api.Module;
import wtf.wyvern.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "ClickGUI",
   category = Category.RENDER,
   description = "Меню чита"
)
public final class Menu extends Module {
   public static final Menu INSTANCE = new Menu();

   private Menu() {
      this.setKeyCode(344);
   }

   public void onEnable() {
      if (mc.world == null) {
         this.setEnabled(false);
      } else {
         Wyvern.getInstance().getMenuScreen().needToClose = false;
         if (mc.currentScreen != Wyvern.getInstance().getMenuScreen()) {
            mc.setScreen(Wyvern.getInstance().getMenuScreen());
            super.onEnable();
         }
      }
   }

   public void onDisable() {
      if (mc.currentScreen == Wyvern.getInstance().getMenuScreen()) {
         Wyvern.getInstance().getMenuScreen().needToClose = true;
      }
      super.onDisable();
   }

   public void setKeyCode(int keyCode) {
      if (keyCode != -1) {
         super.setKeyCode(keyCode);
      }
   }
}