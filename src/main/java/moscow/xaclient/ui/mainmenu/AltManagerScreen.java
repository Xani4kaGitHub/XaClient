package moscow.xaclient.ui.mainmenu;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.CustomDrawContext;
import moscow.xaclient.framework.objects.BorderRadius;
import moscow.xaclient.systems.alts.AltAccount;
import moscow.xaclient.systems.alts.AltManager;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.render.DrawUtility;
import moscow.xaclient.utility.render.ScissorUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AltManagerScreen extends Screen {
   private static final float SCALE = 1.5F;
   private static final int INPUT_WIDTH = (int)(220.0F * SCALE);
   private static final int INPUT_HEIGHT = (int)(17.0F * SCALE);
   private static final int LIST_WIDTH = (int)(220.0F * SCALE);
   private static final int LIST_HEIGHT = (int)(140.0F * SCALE);
   private static final int BUTTON_WIDTH = (int)(70.0F * SCALE);
   private static final int BUTTON_HEIGHT = INPUT_HEIGHT;
   private static final int ROW_HEIGHT = (int)(35.0F * SCALE);
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

   private final Screen parent;
   private final StringBuilder inputText = new StringBuilder();
   private boolean focused;
   private boolean showConfirmDialog;
   private float scrollOffset;
   private float targetScrollOffset;
   private int shakeTime;
   private String selectedName;

   public AltManagerScreen(Screen parent) {
      super(Text.literal("Account Manager"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      super.init();
      manager().load();
      if (this.selectedName == null) {
         String lastSelected = manager().getLastSelectedAccount();
         this.selectedName = lastSelected != null ? lastSelected : MinecraftClient.getInstance().getSession().getUsername();
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.scrollOffset += (this.targetScrollOffset - this.scrollOffset) * 0.25F;
      this.clampScroll();

      context.fill(0, 0, this.width, this.height, 0xFF0E0E12);
      context.draw();

      int centerX = this.width / 2;
      int centerY = this.height / 2;

      CustomDrawContext glass = CustomDrawContext.of(context);
      int cardPad = 16;
      int cardX = this.inputX(centerX) - cardPad;
      int cardY = this.inputY(centerY) - cardPad;
      int cardW = LIST_WIDTH + cardPad * 2;
      int cardH = this.buttonsY(centerY) + BUTTON_HEIGHT + 74 - cardY;
      this.glassPanel(glass, cardX, cardY, cardW, cardH, 12.0F, 14.0F, rgba(20, 20, 26, 180), rgba(72, 72, 88, 150));

      this.renderInput(context, centerX, centerY, mouseX, mouseY);
      this.renderAccountList(context, centerX, centerY, mouseX, mouseY);
      this.renderBottomButtons(context, centerX, centerY, mouseX, mouseY);

      if (this.showConfirmDialog) {
         this.drawConfirmDialog(context, mouseX, mouseY);
      }
   }

   private void renderInput(DrawContext context, int centerX, int centerY, int mouseX, int mouseY) {
      int inputX = this.inputX(centerX);
      int inputY = this.inputY(centerY);
      boolean hovered = this.hovered(mouseX, mouseY, inputX, inputY, INPUT_WIDTH, INPUT_HEIGHT);

      CustomDrawContext glass = CustomDrawContext.of(context);
      this.glassPanel(
         glass,
         inputX,
         inputY,
         INPUT_WIDTH,
         INPUT_HEIGHT,
         5.0F,
         0.0F,
         hovered ? rgba(46, 46, 56, 205) : rgba(32, 32, 40, 190),
         hovered || this.focused ? rgba(120, 140, 255, 200) : rgba(60, 60, 74, 150)
      );

      int textX = inputX + 8;
      int textY = inputY + 8;
      boolean empty = this.inputText.length() == 0;
      String text = empty ? "Enter your name" : this.inputText.toString();
      int color = empty ? 0xFF6E6E78 : (this.focused ? 0xFFFFFFFF : 0xFFC8C8C8);
      context.drawTextWithShadow(this.textRenderer, text, textX, textY, color);

      if (this.focused) {
         int caretX = textX + (empty ? 0 : this.textRenderer.getWidth(this.inputText.toString()));
         context.fill(caretX + 1, textY - 1, caretX + 2, textY + this.textRenderer.fontHeight, 0xFFE6E6E6);
      }
   }

   private void renderAccountList(DrawContext context, int centerX, int centerY, int mouseX, int mouseY) {
      int listX = this.listX(centerX);
      int listY = this.listY(centerY);
      List<AltAccount> accounts = manager().getAccounts();

      CustomDrawContext glass = CustomDrawContext.of(context);
      this.glassPanel(glass, listX, listY, LIST_WIDTH, LIST_HEIGHT, 8.0F, 0.0F, rgba(24, 24, 30, 175), rgba(60, 60, 74, 140));

      ScissorUtility.push(context.getMatrices(), listX, listY, LIST_WIDTH, LIST_HEIGHT);
      float startY = listY + 5.0F;

      if (accounts.isEmpty()) {
         context.drawCenteredTextWithShadow(this.textRenderer, "No accounts", centerX, listY + LIST_HEIGHT / 2 - 4, 0xFF8C8C8C);
      }

      for (int i = 0; i < accounts.size(); i++) {
         AltAccount account = accounts.get(i);
         float rowY = startY - this.scrollOffset + i * ROW_HEIGHT;
         if (rowY + 45.0F < listY || rowY > listY + LIST_HEIGHT) {
            continue;
         }

         this.renderAccountRow(context, account, listX + 8, rowY, LIST_WIDTH - 16, mouseX, mouseY);
      }

      context.draw();
      ScissorUtility.pop();
   }

   private void renderAccountRow(DrawContext context, AltAccount account, int rowX, float rowY, int rowWidth, int mouseX, int mouseY) {
      int y = Math.round(rowY);
      int rowHeight = 45;
      boolean selected = this.selectedName != null && this.selectedName.equalsIgnoreCase(account.getUsername());
      boolean current = MinecraftClient.getInstance().getSession().getUsername().equalsIgnoreCase(account.getUsername());
      boolean hovered = this.hovered(mouseX, mouseY, rowX, y, rowWidth, rowHeight);

      CustomDrawContext glass = CustomDrawContext.of(context);
      ColorRGBA bg = selected ? rgba(48, 54, 96, 210) : hovered ? rgba(40, 40, 50, 200) : rgba(26, 26, 32, 180);
      ColorRGBA brd = selected ? rgba(120, 140, 255, 220) : current ? rgba(90, 200, 210, 200) : rgba(48, 48, 58, 150);
      this.glassPanel(glass, rowX, y, rowWidth, rowHeight, 5.0F, 0.0F, bg, brd);

      int nameColor = current ? 0xFFA7FEFF : 0xFFC8C8C8;
      context.drawTextWithShadow(this.textRenderer, this.trimToWidth(account.getUsername(), 170), rowX + 10, y + 8, nameColor);
      context.drawTextWithShadow(this.textRenderer, "Date " + LocalDate.now().format(DATE_FORMAT), rowX + 10, y + 27, 0xFF8C8C8C);

      int actionX = rowX + rowWidth - 84;
      this.drawButton(context, "Select", actionX, y + 5, 74, 17, mouseX, mouseY, rgba(35, 35, 35, 255), rgba(55, 55, 55, 255));
      this.drawButton(context, "Delete", actionX, y + 24, 74, 17, mouseX, mouseY, rgba(35, 35, 35, 255), rgba(55, 55, 55, 255));
   }

   private void renderBottomButtons(DrawContext context, int centerX, int centerY, int mouseX, int mouseY) {
      int buttonsY = this.buttonsY(centerY);
      int createX = this.createX(centerX);
      int clearX = this.clearX(centerX);
      int randomX = this.randomX(centerX);

      this.drawButton(context, "Create", createX, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY, rgba(35, 35, 35, 255), rgba(55, 55, 55, 255));
      this.drawButton(context, "Clear all", clearX, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY, rgba(35, 35, 35, 255), rgba(55, 55, 55, 255));
      this.drawButton(context, "Random", randomX, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY, rgba(35, 35, 35, 255), rgba(55, 55, 55, 255));

      String accountName = MinecraftClient.getInstance().getSession().getUsername();
      context.drawCenteredTextWithShadow(this.textRenderer, "Selected account: " + accountName, centerX, buttonsY + BUTTON_HEIGHT + 24, 0xFFFFFFFF);
      context.drawCenteredTextWithShadow(this.textRenderer, "Quantity: " + manager().getAccounts().size(), centerX, buttonsY + BUTTON_HEIGHT + 44, 0xFFFFFFFF);

      String error = manager().getLastError();
      if (!error.isEmpty()) {
         context.drawCenteredTextWithShadow(this.textRenderer, error, centerX, buttonsY + BUTTON_HEIGHT + 64, 0xFFFF7070);
      }
   }

   private void drawButton(DrawContext context, String text, int x, int y, int width, int height, int mouseX, int mouseY, ColorRGBA color, ColorRGBA hoverColor) {
      boolean hovered = this.hovered(mouseX, mouseY, x, y, width, height);
      CustomDrawContext glass = CustomDrawContext.of(context);
      ColorRGBA tint = (hovered ? hoverColor : color).withAlpha(210.0F);
      ColorRGBA brd = hovered ? rgba(120, 140, 255, 200) : rgba(60, 60, 74, 150);
      this.glassPanel(glass, x, y, width, height, 4.0F, 0.0F, tint, brd);
      context.drawCenteredTextWithShadow(this.textRenderer, text, x + width / 2, y + height / 2 - 4, 0xFFFFFFFF);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      }

      if (this.showConfirmDialog) {
         return this.handleConfirmClick(mouseX, mouseY);
      }

      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int inputX = this.inputX(centerX);
      int inputY = this.inputY(centerY);
      int buttonsY = this.buttonsY(centerY);

      if (this.hovered(mouseX, mouseY, inputX, inputY, INPUT_WIDTH, INPUT_HEIGHT)) {
         this.focused = true;
         return true;
      }

      if (this.hovered(mouseX, mouseY, this.createX(centerX), buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
         this.createFromInput();
         return true;
      }

      if (this.hovered(mouseX, mouseY, this.clearX(centerX), buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
         this.focused = false;
         this.showConfirmDialog = true;
         return true;
      }

      if (this.hovered(mouseX, mouseY, this.randomX(centerX), buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
         this.focused = false;
         AltAccount account = manager().createRandom();
         this.selectedName = account.getUsername();
         this.targetScrollOffset = this.maxScrollOffset();
         return true;
      }

      if (this.handleAccountClick(mouseX, mouseY, centerX, centerY)) {
         this.focused = false;
         return true;
      }

      this.focused = false;
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean handleAccountClick(double mouseX, double mouseY, int centerX, int centerY) {
      int listX = this.listX(centerX);
      int listY = this.listY(centerY);
      if (!this.hovered(mouseX, mouseY, listX, listY, LIST_WIDTH, LIST_HEIGHT)) {
         return false;
      }

      List<AltAccount> accounts = manager().getAccounts();
      float startY = listY + 5.0F;
      for (int i = 0; i < accounts.size(); i++) {
         AltAccount account = accounts.get(i);
         int rowY = Math.round(startY - this.scrollOffset + i * ROW_HEIGHT);
         int rowX = listX + 8;
         int rowWidth = LIST_WIDTH - 16;
         int actionX = rowX + rowWidth - 84;

         if (this.hovered(mouseX, mouseY, actionX, rowY + 24, 74, 17)) {
            manager().remove(account.getUsername());
            if (this.selectedName != null && this.selectedName.equalsIgnoreCase(account.getUsername())) {
               this.selectedName = null;
            }
            this.clampScroll();
            return true;
         }

         if (this.hovered(mouseX, mouseY, actionX, rowY + 5, 74, 17) || this.hovered(mouseX, mouseY, rowX, rowY, rowWidth, 45)) {
            manager().login(account.getUsername());
            this.selectedName = account.getUsername();
            return true;
         }
      }

      return true;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int listX = this.listX(centerX);
      int listY = this.listY(centerY);
      if (this.hovered(mouseX, mouseY, listX, listY, LIST_WIDTH, LIST_HEIGHT)) {
         this.targetScrollOffset -= (float)verticalAmount * 30.0F;
         this.clampScroll();
         return true;
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.showConfirmDialog) {
         if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.showConfirmDialog = false;
            return true;
         }
         return true;
      }

      if (this.focused) {
         if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_V) {
            this.pasteClipboard();
            return true;
         }

         if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.createFromInput();
            return true;
         }

         if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (this.inputText.length() > 0) {
               this.inputText.deleteCharAt(this.inputText.length() - 1);
            }
            return true;
         }

         if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.focused = false;
            return true;
         }

         return true;
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (!this.focused) {
         return super.charTyped(chr, modifiers);
      }

      if (this.inputText.length() < 16 && (Character.isLetterOrDigit(chr) || chr == '_')) {
         this.inputText.append(chr);
      }

      return true;
   }

   @Override
   public void close() {
      MinecraftClient.getInstance().setScreen(this.parent);
   }

   private void createFromInput() {
      String username = this.inputText.toString().trim();
      if (manager().add(username)) {
         manager().login(username);
         this.selectedName = username;
         this.inputText.setLength(0);
         this.focused = false;
         this.targetScrollOffset = this.maxScrollOffset();
      }
   }

   private void pasteClipboard() {
      String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
      if (clipboard == null || clipboard.isEmpty()) {
         return;
      }

      for (int i = 0; i < clipboard.length() && this.inputText.length() < 16; i++) {
         char chr = clipboard.charAt(i);
         if (Character.isLetterOrDigit(chr) || chr == '_') {
            this.inputText.append(chr);
         }
      }
   }

   private void drawConfirmDialog(DrawContext context, int mouseX, int mouseY) {
      int boxWidth = 300;
      int boxHeight = 130;
      int boxX = (this.width - boxWidth) / 2;
      int boxY = (this.height - boxHeight) / 2;
      int btnWidth = 90;
      int btnHeight = 28;
      int yesX = boxX + 35;
      int noX = boxX + boxWidth - 35 - btnWidth;
      int btnY = boxY + boxHeight - 50;

      context.fill(0, 0, this.width, this.height, 0x99000000);
      CustomDrawContext glass = CustomDrawContext.of(context);
      this.glassPanel(glass, boxX, boxY, boxWidth, boxHeight, 10.0F, 14.0F, rgba(28, 28, 36, 225), rgba(90, 90, 110, 180));
      context.drawCenteredTextWithShadow(this.textRenderer, "Clear all accounts?", this.width / 2, boxY + 30, 0xFFFFFFFF);

      this.drawButton(context, "Yes", yesX, btnY, btnWidth, btnHeight, mouseX, mouseY, rgba(60, 150, 75, 255), rgba(72, 180, 88, 255));
      this.drawButton(context, "No", noX, btnY, btnWidth, btnHeight, mouseX, mouseY, rgba(180, 60, 60, 255), rgba(210, 72, 72, 255));
   }

   private boolean handleConfirmClick(double mouseX, double mouseY) {
      int boxWidth = 300;
      int boxHeight = 130;
      int boxX = (this.width - boxWidth) / 2;
      int boxY = (this.height - boxHeight) / 2;
      int btnWidth = 90;
      int btnHeight = 28;
      int yesX = boxX + 35;
      int noX = boxX + boxWidth - 35 - btnWidth;
      int btnY = boxY + boxHeight - 50;

      if (this.hovered(mouseX, mouseY, yesX, btnY, btnWidth, btnHeight)) {
         manager().clearAll();
         this.selectedName = null;
         this.scrollOffset = 0.0F;
         this.targetScrollOffset = 0.0F;
         this.showConfirmDialog = false;
         return true;
      }

      if (this.hovered(mouseX, mouseY, noX, btnY, btnWidth, btnHeight)) {
         this.showConfirmDialog = false;
         return true;
      }

      return true;
   }

   private void drawGradientTitle(DrawContext context, String title, float y) {
      float scale = 2.6F;
      int titleWidth = this.titleWidth();
      float x = (this.width - titleWidth) / 2.0F;
      float drawX = x / scale;
      float drawY = y / scale;
      float offset = 0.0F;

      context.getMatrices().push();
      context.getMatrices().scale(scale, scale, 1.0F);
      for (int i = 0; i < title.length(); i++) {
         String chr = String.valueOf(title.charAt(i));
         float hue = ((System.currentTimeMillis() % 4000L) / 4000.0F + i / (float)Math.max(1, title.length())) % 1.0F;
         int color = java.awt.Color.HSBtoRGB(hue, 0.55F, 1.0F);
         context.drawTextWithShadow(this.textRenderer, chr, Math.round(drawX + offset), Math.round(drawY), 0xFF000000 | color & 0x00FFFFFF);
         offset += this.textRenderer.getWidth(chr);
      }
      context.getMatrices().pop();
   }

   private String trimToWidth(String text, int maxWidth) {
      if (this.textRenderer.getWidth(text) <= maxWidth) {
         return text;
      }

      String ellipsis = "...";
      while (!text.isEmpty() && this.textRenderer.getWidth(text + ellipsis) > maxWidth) {
         text = text.substring(0, text.length() - 1);
      }

      return text + ellipsis;
   }

   private int inputX(int centerX) {
      return centerX - (int)(110.0F * SCALE);
   }

   private int inputY(int centerY) {
      return centerY - (int)(92.0F * SCALE);
   }

   private int listX(int centerX) {
      return this.inputX(centerX);
   }

   private int listY(int centerY) {
      return centerY - (int)(70.0F * SCALE);
   }

   private int buttonsY(int centerY) {
      return this.listY(centerY) + LIST_HEIGHT + (int)(10.0F * SCALE);
   }

   private int createX(int centerX) {
      return centerX - BUTTON_WIDTH - (int)(40.0F * SCALE);
   }

   private int clearX(int centerX) {
      return centerX - BUTTON_WIDTH / 2;
   }

   private int randomX(int centerX) {
      return centerX + BUTTON_WIDTH - (int)(30.0F * SCALE);
   }

   private float titleX() {
      return (this.width - this.titleWidth()) / 2.0F;
   }

   private int titleWidth() {
      return Math.round(this.textRenderer.getWidth("AltManager") * 2.6F);
   }

   private void clampScroll() {
      float max = this.maxScrollOffset();
      this.targetScrollOffset = Math.max(0.0F, Math.min(this.targetScrollOffset, max));
      this.scrollOffset = Math.max(0.0F, Math.min(this.scrollOffset, max));
   }

   private float maxScrollOffset() {
      return Math.max(0.0F, manager().getAccounts().size() * ROW_HEIGHT - LIST_HEIGHT + 5.0F);
   }

   private boolean hovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private void rounded(DrawContext context, float x, float y, float width, float height, float radius, ColorRGBA color) {
      DrawUtility.drawRoundedRect(context.getMatrices(), x, y, width, height, BorderRadius.all(radius), color);
   }

   private void border(DrawContext context, float x, float y, float width, float height, float radius, ColorRGBA color) {
      DrawUtility.drawRoundedBorder(context.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(radius), color);
   }

   private void glassPanel(CustomDrawContext glass, float x, float y, float width, float height, float radius, float shadow, ColorRGBA tint, ColorRGBA borderColor) {
      if (shadow > 0.0F) {
         glass.drawShadow(x, y, width, height, shadow, BorderRadius.all(radius), rgba(0, 0, 0, 120));
      }

      glass.drawLiquidGlass(x, y, width, height, 2.0F, BorderRadius.all(radius), ColorRGBA.WHITE.withAlpha(255.0F), true);
      glass.drawRoundedRect(x, y, width, height, BorderRadius.all(radius), tint);
      glass.drawRoundedBorder(x, y, width, height, 1.0F, BorderRadius.all(radius), borderColor);
   }

   private static ColorRGBA rgba(int red, int green, int blue, int alpha) {
      return new ColorRGBA(red, green, blue, alpha);
   }

   private static AltManager manager() {
      return XaClient.getInstance().getAltManager();
   }
}
