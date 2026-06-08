package moscow.xaclient.ui.mainmenu;

import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.CustomDrawContext;
import moscow.xaclient.framework.objects.BorderRadius;
import moscow.xaclient.systems.proxy.ProxyManager;
import moscow.xaclient.utility.colors.ColorRGBA;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ProxyScreen extends Screen {
   private static final int FIELD_WIDTH = 260;
   private static final int FIELD_HEIGHT = 26;
   private static final int BUTTON_HEIGHT = 26;

   private final Screen parent;
   private final StringBuilder inputText = new StringBuilder();
   private boolean typing;

   public ProxyScreen(Screen parent) {
      super(Text.literal("Proxy"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      super.init();
      if (this.inputText.length() == 0 && manager().getAddress() != null) {
         this.inputText.append(manager().formatted());
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, 0xFF0E0E12);
      context.draw();

      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int fieldX = centerX - FIELD_WIDTH / 2;
      int fieldY = centerY - 30;

      CustomDrawContext glass = CustomDrawContext.of(context);

      int cardPad = 18;
      int cardX = fieldX - cardPad;
      int cardY = fieldY - 52;
      int cardW = FIELD_WIDTH + cardPad * 2;
      int cardH = 200;
      this.glassPanel(glass, cardX, cardY, cardW, cardH, 12.0F, 14.0F, rgba(20, 20, 26, 180), rgba(72, 72, 88, 150));

      boolean active = manager().isActive();
      String status = active ? "Active: " + manager().formatted() : "Proxy disabled";
      ColorRGBA statusColor = active ? rgba(120, 220, 150, 255) : rgba(180, 180, 190, 255);
      context.drawCenteredTextWithShadow(this.textRenderer, status, centerX, cardY + 16, statusColor.getRGB());

      boolean inputHover = this.hovered(mouseX, mouseY, fieldX, fieldY, FIELD_WIDTH, FIELD_HEIGHT);
      this.glassPanel(
         glass,
         fieldX,
         fieldY,
         FIELD_WIDTH,
         FIELD_HEIGHT,
         6.0F,
         0.0F,
         inputHover ? rgba(46, 46, 56, 205) : rgba(32, 32, 40, 190),
         inputHover || this.typing ? rgba(120, 140, 255, 200) : rgba(60, 60, 74, 150)
      );

      String text;
      int textColor = inputHover || this.typing ? 0xFFE6E6E6 : 0xFFB4B4B4;
      if (this.inputText.length() == 0 && !this.typing) {
         text = "socks5://user:pass@host:port";
      } else {
         text = this.inputText.toString();
         if (this.typing && System.currentTimeMillis() / 500L % 2L == 0L) {
            text += "_";
         }
      }

      context.drawTextWithShadow(this.textRenderer, text, fieldX + 8, fieldY + 9, textColor);

      int rowY = fieldY + FIELD_HEIGHT + 12;
      int btnWidth = (FIELD_WIDTH - 10) / 2;
      this.drawButton(glass, context, "Apply", fieldX, rowY, btnWidth, mouseX, mouseY, rgba(60, 66, 110, 210));
      String toggleText = manager().isEnabled() ? "Disable" : "Enable";
      ColorRGBA toggleColor = manager().isEnabled() ? rgba(70, 150, 90, 210) : rgba(60, 66, 110, 210);
      this.drawButton(glass, context, toggleText, fieldX + btnWidth + 10, rowY, btnWidth, mouseX, mouseY, toggleColor);

      int row2Y = rowY + BUTTON_HEIGHT + 8;
      this.drawButton(glass, context, "Clear", fieldX, row2Y, btnWidth, mouseX, mouseY, rgba(150, 70, 70, 210));
      this.drawButton(glass, context, "Back", fieldX + btnWidth + 10, row2Y, btnWidth, mouseX, mouseY, rgba(50, 50, 62, 210));

      String error = manager().getLastError();
      if (!error.isEmpty()) {
         context.drawCenteredTextWithShadow(this.textRenderer, error, centerX, row2Y + BUTTON_HEIGHT + 12, 0xFFFF7070);
      }

      context.draw();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      }

      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int fieldX = centerX - FIELD_WIDTH / 2;
      int fieldY = centerY - 30;
      int btnWidth = (FIELD_WIDTH - 10) / 2;
      int rowY = fieldY + FIELD_HEIGHT + 12;
      int row2Y = rowY + BUTTON_HEIGHT + 8;

      if (this.hovered(mouseX, mouseY, fieldX, fieldY, FIELD_WIDTH, FIELD_HEIGHT)) {
         this.typing = true;
         return true;
      }

      if (this.hovered(mouseX, mouseY, fieldX, rowY, btnWidth, BUTTON_HEIGHT)) {
         manager().parse(this.inputText.toString());
         this.typing = false;
         return true;
      }

      if (this.hovered(mouseX, mouseY, fieldX + btnWidth + 10, rowY, btnWidth, BUTTON_HEIGHT)) {
         if (this.inputText.length() > 0 && manager().getAddress() == null) {
            manager().parse(this.inputText.toString());
         }

         manager().toggle();
         this.typing = false;
         return true;
      }

      if (this.hovered(mouseX, mouseY, fieldX, row2Y, btnWidth, BUTTON_HEIGHT)) {
         manager().reset();
         this.inputText.setLength(0);
         this.typing = false;
         return true;
      }

      if (this.hovered(mouseX, mouseY, fieldX + btnWidth + 10, row2Y, btnWidth, BUTTON_HEIGHT)) {
         this.close();
         return true;
      }

      this.typing = false;
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.typing) {
         if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_V) {
            String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clipboard != null) {
               this.inputText.append(clipboard.trim());
            }

            return true;
         }

         if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            manager().parse(this.inputText.toString());
            this.typing = false;
            return true;
         }

         if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (this.inputText.length() > 0) {
               this.inputText.deleteCharAt(this.inputText.length() - 1);
            }

            return true;
         }

         if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.typing = false;
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (!this.typing) {
         return super.charTyped(chr, modifiers);
      }

      if (this.inputText.length() < 64 && chr >= ' ') {
         this.inputText.append(chr);
      }

      return true;
   }

   @Override
   public void close() {
      MinecraftClient.getInstance().setScreen(this.parent);
   }

   private void drawButton(CustomDrawContext glass, DrawContext context, String text, int x, int y, int width, int mouseX, int mouseY, ColorRGBA color) {
      boolean hovered = this.hovered(mouseX, mouseY, x, y, width, BUTTON_HEIGHT);
      this.glassPanel(
         glass,
         x,
         y,
         width,
         BUTTON_HEIGHT,
         5.0F,
         0.0F,
         color.withAlpha(hovered ? 230.0F : 200.0F),
         hovered ? rgba(120, 140, 255, 200) : rgba(60, 60, 74, 150)
      );
      context.drawCenteredTextWithShadow(this.textRenderer, text, x + width / 2, y + BUTTON_HEIGHT / 2 - 4, 0xFFFFFFFF);
   }

   private void glassPanel(CustomDrawContext glass, float x, float y, float width, float height, float radius, float shadow, ColorRGBA tint, ColorRGBA borderColor) {
      if (shadow > 0.0F) {
         glass.drawShadow(x, y, width, height, shadow, BorderRadius.all(radius), rgba(0, 0, 0, 120));
      }

      glass.drawLiquidGlass(x, y, width, height, 2.0F, BorderRadius.all(radius), ColorRGBA.WHITE.withAlpha(255.0F), true);
      glass.drawRoundedRect(x, y, width, height, BorderRadius.all(radius), tint);
      glass.drawRoundedBorder(x, y, width, height, 1.0F, BorderRadius.all(radius), borderColor);
   }

   private boolean hovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private static ColorRGBA rgba(int red, int green, int blue, int alpha) {
      return new ColorRGBA(red, green, blue, alpha);
   }

   private static ProxyManager manager() {
      return XaClient.getInstance().getProxyManager();
   }
}
