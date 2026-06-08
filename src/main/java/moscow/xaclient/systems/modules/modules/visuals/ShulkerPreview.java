package moscow.xaclient.systems.modules.modules.visuals;

import java.util.List;
import moscow.xaclient.framework.base.CustomDrawContext;
import moscow.xaclient.framework.objects.BorderRadius;
import moscow.xaclient.mixin.accessors.HandledScreenAccessor;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.render.ScreenRenderEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ColorSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.game.ItemUtility;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(name = "Shulker Preview", category = ModuleCategory.VISUALS, desc = "modules.descriptions.shulker_preview")
public class ShulkerPreview extends BaseModule {
   private static final int COLUMNS = 9;
   private static final int ROWS = 3;
   private static final int SLOT_SIZE = 18;
   private static final int PADDING = 5;
   private final BooleanSetting showEmptySlots = new BooleanSetting(this, "modules.settings.shulker_preview.show_empty_slots").enable();
   private final BooleanSetting clampToScreen = new BooleanSetting(this, "modules.settings.shulker_preview.clamp_to_screen").enable();
   private final SliderSetting scale = new SliderSetting(this, "modules.settings.shulker_preview.scale")
      .min(0.75F)
      .max(1.5F)
      .step(0.05F)
      .currentValue(1.0F);
   private final ColorSetting background = new ColorSetting(this, "modules.settings.shulker_preview.background")
      .color(new ColorRGBA(12.0F, 12.0F, 14.0F, 205.0F));
   private final ColorSetting border = new ColorSetting(this, "modules.settings.shulker_preview.border")
      .color(new ColorRGBA(160.0F, 90.0F, 255.0F, 180.0F));
   private final EventListener<ScreenRenderEvent> onScreenRender = event -> {
      if (!(mc.currentScreen instanceof HandledScreen<?> screen) || mc.player == null) {
         return;
      }

      Slot hoveredSlot = this.getHoveredSlot(screen, event.getMouseX(), event.getMouseY());
      if (hoveredSlot == null || !this.isShulker(hoveredSlot.getStack())) {
         return;
      }

      List<ItemStack> items = ItemUtility.getItemsInShulker(hoveredSlot.getStack());
      if (items.isEmpty() && !this.showEmptySlots.isEnabled()) {
         return;
      }

      this.renderPreview(event.getContext(), event.getMouseX(), event.getMouseY(), items);
   };

   private Slot getHoveredSlot(HandledScreen<?> screen, int mouseX, int mouseY) {
      HandledScreenAccessor accessor = (HandledScreenAccessor)screen;
      int screenX = accessor.getX();
      int screenY = accessor.getY();

      for (Slot slot : mc.player.currentScreenHandler.slots) {
         int x = screenX + slot.x;
         int y = screenY + slot.y;
         if (slot.isEnabled() && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            return slot;
         }
      }

      return null;
   }

   private boolean isShulker(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
   }

   private void renderPreview(CustomDrawContext context, int mouseX, int mouseY, List<ItemStack> items) {
      float previewScale = this.scale.getCurrentValue();
      float width = (COLUMNS * SLOT_SIZE + PADDING * 2) * previewScale;
      float height = (ROWS * SLOT_SIZE + PADDING * 2) * previewScale;
      float x = mouseX + 12.0F;
      float y = mouseY - height - 12.0F;

      if (this.clampToScreen.isEnabled()) {
         x = MathHelper.clamp(x, 2.0F, (float)sr.getScaledWidth() - width - 2.0F);
         y = MathHelper.clamp(y, 2.0F, (float)sr.getScaledHeight() - height - 2.0F);
      }

      context.getMatrices().push();
      context.getMatrices().translate(x, y, 400.0F);
      context.getMatrices().scale(previewScale, previewScale, 1.0F);
      float unscaledWidth = COLUMNS * SLOT_SIZE + PADDING * 2;
      float unscaledHeight = ROWS * SLOT_SIZE + PADDING * 2;
      context.drawRoundedRect(0.0F, 0.0F, unscaledWidth, unscaledHeight, BorderRadius.all(4.0F), this.background.getColor());
      context.drawRoundedBorder(0.0F, 0.0F, unscaledWidth, unscaledHeight, 1.0F, BorderRadius.all(4.0F), this.border.getColor());

      for (int row = 0; row < ROWS; row++) {
         for (int column = 0; column < COLUMNS; column++) {
            int index = row * COLUMNS + column;
            float slotX = PADDING + column * SLOT_SIZE;
            float slotY = PADDING + row * SLOT_SIZE;
            if (this.showEmptySlots.isEnabled()) {
               context.drawRoundedRect(slotX, slotY, 16.0F, 16.0F, BorderRadius.all(2.0F), new ColorRGBA(255.0F, 255.0F, 255.0F, 18.0F));
            }

            if (index < items.size()) {
               ItemStack stack = items.get(index);
               if (!stack.isEmpty()) {
                  context.drawItem(stack, slotX, slotY, 1.0F);
                  context.drawStackOverlay(mc.textRenderer, stack, (int)slotX, (int)slotY);
               }
            }
         }
      }

      context.getMatrices().pop();
   }
}
