package moscow.xaclient.mixin.minecraft.client.gui.overlay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.modules.modules.visuals.ExtraTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Nullables;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
   @Unique
   private static final Comparator<PlayerListEntry> XACLIENT_ENTRY_ORDERING = Comparator.<PlayerListEntry>comparingInt(
         entry -> entry.getGameMode() == GameMode.SPECTATOR ? 1 : 0
      )
      .thenComparing(entry -> Nullables.mapOrElse(entry.getScoreboardTeam(), Team::getName, ""))
      .thenComparing(entry -> entry.getProfile().getName(), String::compareToIgnoreCase);

   @Inject(method = "collectPlayerEntries", at = @At("HEAD"), cancellable = true)
   private void collectPlayerEntriesHook(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null || mc.player.networkHandler == null) {
         return;
      }

      ExtraTab extraTab = XaClient.getInstance().getModuleManager().getModule(ExtraTab.class);
      if (!extraTab.isEnabled()) {
         return;
      }

      List<PlayerListEntry> entries = new ArrayList<>(mc.player.networkHandler.getListedPlayerListEntries());
      entries.sort(XACLIENT_ENTRY_ORDERING);
      int limit = extraTab.getEntryLimit();
      if (entries.size() > limit) {
         entries = entries.subList(0, limit);
      }

      cir.setReturnValue(entries);
   }
}
