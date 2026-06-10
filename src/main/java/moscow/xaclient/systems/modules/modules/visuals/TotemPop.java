package moscow.xaclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.network.ReceivePacketEvent;
import moscow.xaclient.systems.event.impl.render.Render3DEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ColorSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.EntityUtility;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Totem Pop", category = ModuleCategory.VISUALS, desc = "modules.descriptions.totem_pop")
public class TotemPop extends BaseModule {
   private static boolean renderingGhost;
   private static int ghostColor = Colors.WHITE.withAlpha(150.0F).getRGB();

   private final SliderSetting duration = new SliderSetting(this, "modules.settings.totem_pop.duration")
      .min(250.0F)
      .max(2500.0F)
      .step(50.0F)
      .currentValue(850.0F)
      .suffix("ms");
   private final SliderSetting rise = new SliderSetting(this, "modules.settings.totem_pop.rise")
      .min(0.0F)
      .max(6.0F)
      .step(0.1F)
      .currentValue(2.4F);
   private final BooleanSetting self = new BooleanSetting(this, "modules.settings.totem_pop.self").enable();
   private final ColorSetting color = new ColorSetting(this, "modules.settings.totem_pop.color").color(Colors.ACCENT);
   private final List<PopEntry> entries = new ArrayList<>();

   // RU: Слушает пакет срабатывания тотема и запоминает позу игрока.
   // EN: Listens for totem activation packets and stores the player's pose.
   private final EventListener<ReceivePacketEvent> onPacket = event -> {
      if (!EntityUtility.isInGame() || !(event.getPacket() instanceof EntityStatusS2CPacket packet) || packet.getStatus() != 35) {
         return;
      }

      if (!(packet.getEntity(mc.world) instanceof PlayerEntity player)) {
         return;
      }

      if (!this.self.isEnabled() && player == mc.player) {
         return;
      }

      this.entries.add(new PopEntry(player, System.currentTimeMillis()));
   };

   // RU: Рендерит полупрозрачную копию игрока, которая поднимается вверх и исчезает.
   // EN: Renders a translucent player copy that rises and fades away.
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (this.entries.isEmpty() || !EntityUtility.isInGame()) {
         return;
      }

      MatrixStack matrices = event.getMatrices();
      Vec3d cameraPos = event.getCamera().getPos();
      EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
      matrices.push();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);

      Iterator<PopEntry> iterator = this.entries.iterator();
      while (iterator.hasNext()) {
         PopEntry entry = iterator.next();
         float progress = this.getProgress(entry);
         if (progress >= 1.0F) {
            iterator.remove();
            continue;
         }

         float alpha = 0.7F * (1.0F - progress);
         ColorRGBA ghost = this.color.getColor().withAlpha(this.color.getColor().getAlpha() * alpha);
         renderingGhost = true;
         ghostColor = ghost.getRGB();

         try {
            this.renderGhost(entry, progress, event.getTickDelta(), matrices, cameraPos, dispatcher);
         } finally {
            renderingGhost = false;
         }
      }

      RenderSystem.depthMask(true);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      matrices.pop();
   };

   @Override
   // RU: Очищает все призраки при выключении модуля.
   // EN: Clears all ghost entries when the module is disabled.
   public void onDisable() {
      this.entries.clear();
   }

   // RU: Сообщает mixin'у рендера, что сейчас рисуется призрак.
   // EN: Tells the renderer mixin that a ghost is currently being drawn.
   public static boolean isRenderingGhost() {
      return renderingGhost;
   }

   // RU: Возвращает цвет, которым нужно тонировать призрачную модель.
   // EN: Returns the color used to tint the ghost model.
   public static int getGhostColor() {
      return ghostColor;
   }

   // RU: Временно подставляет сохраненные повороты, чтобы копия не дергалась.
   // EN: Temporarily restores saved rotations so the ghost does not jitter.
   private void renderGhost(
      PopEntry entry,
      float progress,
      float tickDelta,
      MatrixStack matrices,
      Vec3d cameraPos,
      EntityRenderDispatcher dispatcher
   ) {
      PlayerEntity player = entry.player;
      float originalYaw = player.getYaw();
      float originalPitch = player.getPitch();
      float originalHeadYaw = player.headYaw;
      float originalBodyYaw = player.bodyYaw;
      float originalPrevYaw = player.prevYaw;
      float originalPrevPitch = player.prevPitch;
      float originalPrevHeadYaw = player.prevHeadYaw;
      float originalPrevBodyYaw = player.prevBodyYaw;
      player.setYaw(entry.yaw);
      player.setPitch(entry.pitch);
      player.headYaw = entry.headYaw;
      player.bodyYaw = entry.bodyYaw;
      player.prevYaw = entry.yaw;
      player.prevPitch = entry.pitch;
      player.prevHeadYaw = entry.headYaw;
      player.prevBodyYaw = entry.bodyYaw;

      try {
         dispatcher.render(
            player,
            entry.x - cameraPos.x,
            entry.y + this.rise.getCurrentValue() * progress - cameraPos.y,
            entry.z - cameraPos.z,
            tickDelta,
            matrices,
            mc.getBufferBuilders().getEntityVertexConsumers(),
            15728880
         );
         mc.getBufferBuilders().getEntityVertexConsumers().draw();
      } finally {
         player.setYaw(originalYaw);
         player.setPitch(originalPitch);
         player.headYaw = originalHeadYaw;
         player.bodyYaw = originalBodyYaw;
         player.prevYaw = originalPrevYaw;
         player.prevPitch = originalPrevPitch;
         player.prevHeadYaw = originalPrevHeadYaw;
         player.prevBodyYaw = originalPrevBodyYaw;
      }
   }

   // RU: Возвращает прогресс жизни призрака от 0 до 1.
   // EN: Returns ghost lifetime progress from 0 to 1.
   private float getProgress(PopEntry entry) {
      return Math.min(1.0F, (System.currentTimeMillis() - entry.startTime) / this.duration.getCurrentValue());
   }

   private static class PopEntry {
      private final PlayerEntity player;
      private final double x;
      private final double y;
      private final double z;
      private final long startTime;
      private final float yaw;
      private final float pitch;
      private final float headYaw;
      private final float bodyYaw;

      // RU: Сохраняет позицию и повороты игрока на момент срабатывания тотема.
      // EN: Stores the player's position and rotations when the totem pops.
      private PopEntry(PlayerEntity player, long startTime) {
         this.player = player;
         this.x = player.getX();
         this.y = player.getY();
         this.z = player.getZ();
         this.startTime = startTime;
         this.yaw = player.getYaw();
         this.pitch = player.getPitch();
         this.headYaw = player.headYaw;
         this.bodyYaw = player.bodyYaw;
      }
   }
}
