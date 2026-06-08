package moscow.xaclient.ui.hud.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.UIContext;
import moscow.xaclient.framework.msdf.Font;
import moscow.xaclient.framework.msdf.Fonts;
import moscow.xaclient.systems.notifications.NotificationType;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.ui.components.animated.AnimatedNumber;
import moscow.xaclient.ui.hud.HudList;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.animation.base.Easing;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.server.ServerUtility;
import moscow.xaclient.utility.gui.GuiUtility;
import moscow.xaclient.utility.mixins.StatusEffectInstanceAddition;
import moscow.xaclient.utility.render.batching.Batching;
import moscow.xaclient.utility.render.batching.impl.FontBatching;
import moscow.xaclient.utility.render.batching.impl.IconBatching;
import moscow.xaclient.utility.render.batching.impl.RectBatching;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;

public class Effects extends HudList {
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");
   int lastSize = -1;
   private final Map<String, StatusEffectInstance> effects = new TreeMap<>();
   private final Map<StatusEffect, Boolean> ended = new HashMap<>();
   private final BooleanSetting alert = new BooleanSetting(this, "hud.effects.alert");

   public Effects() {
      super("hud.effects", "icons/hud/potion.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = 92.0F;
      this.height = 18.0F;
      Collection<StatusEffectInstance> original = mc.player.getStatusEffects();

      for (StatusEffectInstance eff : original) {
         StatusEffect potion = (StatusEffect)eff.getEffectType().value();
         String realName = potion.getName().getString();
         if (realName != null && !ServerUtility.isCM()) {
            if (this.effects.containsKey(realName)) {
               this.effects.replace(realName, eff);
               Animation anim = ((StatusEffectInstanceAddition)eff).xaclient$getAnimPotion();
               if (anim.getValue() == 0.0F) {
                  anim.setValue(1.0F);
               }
            } else {
               this.effects.put(realName, eff);
            }
         }
      }

      if (!this.effects.isEmpty()) {
         this.height += 5.0F;
      }

      for (StatusEffectInstance effx : this.effects.values()) {
         Animation anim = ((StatusEffectInstanceAddition)effx).xaclient$getAnimPotion();
         StatusEffect potion = (StatusEffect)effx.getEffectType().value();
         if (this.alert.isEnabled()) {
            String effectName = potion.getName().getString() + " " + (effx.getAmplifier() > 0 ? effx.getAmplifier() + 1 : "");
            if (!mc.player.hasStatusEffect(effx.getEffectType())) {
               if (!this.ended.getOrDefault(potion, false) && !potion.getCategory().equals(StatusEffectCategory.HARMFUL)) {
                  XaClient.getInstance()
                     .getNotificationManager()
                     .addNotificationOther(NotificationType.INFO, "Эффект " + effectName + " закончился", "Действие эффекта завершено");
                  this.ended.put(potion, true);
               }
            } else {
               this.ended.put(potion, false);
            }
         }

         anim.update(original.contains(effx));
         anim.setEasing(Easing.BAKEK);
         this.width = Math.max(Fonts.REGULAR.getFont(7.0F).width(potion.getName().getString()) + 60.0F, this.width);
         this.height = this.height + 18.0F * anim.getValue();
      }

      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (mc.player != null && mc.world != null) {
         Font font = Fonts.REGULAR.getFont(7.0F);
         float offset = 22.0F;
         super.renderComponent(context);
         StatusEffectInstance toRemove = null;
         Batching split = new RectBatching(VertexFormats.POSITION_COLOR, context.getMatrices());

         for (StatusEffectInstance eff : this.effects.values()) {
            Animation anim = ((StatusEffectInstanceAddition)eff).xaclient$getAnimPotion();
            if (anim.getValue() == 0.0F) {
               toRemove = eff;
            } else {
               float off = -4.5F + 4.5F * anim.getValue();
               if (offset != 22.0F) {
                  context.drawRect(this.x, this.y + offset + off, this.width, 0.5F, Colors.getTextColor().withAlpha(5.1F * anim.getValue()));
               }

               offset += 18.0F * anim.getValue();
            }
         }

         split.draw();
         offset = 22.0F;
         Batching texture = new IconBatching(VertexFormats.POSITION_TEXTURE_COLOR, context.getMatrices());

         for (StatusEffectInstance effx : this.effects.values()) {
            Animation anim = ((StatusEffectInstanceAddition)effx).xaclient$getAnimPotion();
            if (anim.getValue() != 0.0F) {
               float off = -4.5F + 4.5F * anim.getValue();
               Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effx.getEffectType());
               context.drawTexture(
                  sprite.getAtlasId(),
                  this.x + 7.0F * anim.getValue(),
                  this.y + offset + off + GuiUtility.getMiddleOfBox(8.0F, 18.0F) + 1.0F,
                  8.0F,
                  8.0F,
                  sprite.getMinU(),
                  sprite.getMaxU(),
                  sprite.getMinV(),
                  sprite.getMaxV(),
                  ColorRGBA.WHITE.withAlpha(255.0F * anim.getValue())
               );
               offset += 18.0F * anim.getValue();
            }
         }

         texture.draw();
         Batching fontBatching = new FontBatching(VertexFormats.POSITION_TEXTURE_COLOR, font.getFont());
         offset = 22.0F;

         for (StatusEffectInstance effxx : this.effects.values()) {
            Animation anim = ((StatusEffectInstanceAddition)effxx).xaclient$getAnimPotion();
            AnimatedNumber timeAnimation = ((StatusEffectInstanceAddition)effxx).xaclient$getTimeAnimation();
            StatusEffect potion = (StatusEffect)effxx.getEffectType().value();
            if (anim.getValue() != 0.0F) {
               float off = -4.5F + 4.5F * anim.getValue();
               String effectName = potion.getName().getString() + " " + (effxx.getAmplifier() > 0 ? effxx.getAmplifier() + 1 : "");
               if (!effxx.isInfinite() && effxx.getDuration() < 999999999) {
                  int totalSeconds = effxx.getDuration() / 20;
                  int minutes = totalSeconds / 60;
                  int seconds = totalSeconds % 60;
                  String timeStr = String.format("%02d:%02d", minutes, seconds);
                  String minutesAndSeparator = String.format("%02d:", minutes);
                  float timeX = this.x + this.width - 7.0F * anim.getValue();
                  float timeY = this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F);
                  float minutesWidth = font.width(minutesAndSeparator);
                  float totalWidth = font.width(timeStr);
                  context.drawText(font, minutesAndSeparator, timeX - totalWidth, timeY, Colors.getTextColor().withAlpha(255.0F * anim.getValue()));
                  timeAnimation.settings(true, Colors.getTextColor().withAlpha(255.0F * anim.getValue()));
                  timeAnimation.update(seconds);
                  timeAnimation.pos(timeX - totalWidth + minutesWidth, timeY);
                  timeAnimation.render(context);
               } else {
                  String duration = "**:**";
                  float timeX = this.x + this.width - 7.0F * anim.getValue();
                  float timeY = this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F);
                  context.drawRightText(font, duration, timeX, timeY, Colors.getTextColor().withAlpha((int)(255.0F * anim.getValue())));
               }

               context.drawText(
                  font,
                  effectName,
                  this.x + 13.0F + 7.0F * anim.getValue(),
                  this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F),
                  Colors.getTextColor().withAlpha(255.0F * anim.getValue())
               );
               offset += 18.0F * anim.getValue();
            }
         }

         fontBatching.draw();
         if (toRemove != null) {
            StatusEffect potion = (StatusEffect)toRemove.getEffectType().value();
            this.effects.remove(potion.getName().getString(), toRemove);
         }
      }
   }

   @Override
   public boolean show() {
      return mc.player != null && mc.world != null
         ? (!mc.player.getStatusEffects().isEmpty() || mc.currentScreen instanceof ChatScreen || this.alwaysDisplay.isEnabled()) && !ServerUtility.isCM()
         : false;
   }
}
