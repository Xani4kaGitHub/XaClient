package moscow.xaclient;

import lombok.Generated;
import moscow.xaclient.framework.shader.GlProgram;
import moscow.xaclient.systems.ai.aura.AuraAIManager;
import moscow.xaclient.systems.alts.AltManager;
import moscow.xaclient.systems.commands.CommandRegistry;
import moscow.xaclient.systems.config.ConfigDropHandler;
import moscow.xaclient.systems.config.ConfigManager;
import moscow.xaclient.systems.event.EventIntegration;
import moscow.xaclient.systems.event.EventManager;
import moscow.xaclient.systems.event.handlers.ServerConnectionHandler;
import moscow.xaclient.systems.file.FileManager;
import moscow.xaclient.systems.friends.FriendManager;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.systems.modules.ModuleManager;
import moscow.xaclient.systems.modules.constructions.swinganim.SwingManager;
import moscow.xaclient.systems.modules.constructions.swinganim.presets.SwingPresetManager;
import moscow.xaclient.systems.modules.listeners.ModuleTickListener;
import moscow.xaclient.systems.modules.listeners.ModuleWidgetRenderer;
import moscow.xaclient.systems.notifications.NotificationManager;
import moscow.xaclient.systems.proxy.ProxyManager;
import moscow.xaclient.systems.poshalko.PoshalkoHandler;
import moscow.xaclient.systems.target.TargetManager;
import moscow.xaclient.systems.theme.ThemeManager;
import moscow.xaclient.systems.waypoints.WayPointsManager;
import moscow.xaclient.ui.hud.Hud;
import moscow.xaclient.ui.menu.MenuScreen;
import moscow.xaclient.utility.game.ProcessTerminator;
import moscow.xaclient.utility.game.TitleBarHelper;
import moscow.xaclient.utility.game.server.TPSHandler;
import moscow.xaclient.utility.interfaces.IMinecraft;
import moscow.xaclient.utility.math.calculator.ChatListener;
import moscow.xaclient.utility.render.DrawUtility;
import moscow.xaclient.utility.rotations.RotationHandler;
import moscow.xaclient.utility.rotations.RotationUpdateListener;
import moscow.xaclient.utility.sounds.MusicTracker;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.Initialization;

public enum XaClient implements IMinecraft {
   INSTANCE;

   public static final String NAME = "XaClient";
   public static final String BUILD_TYPE = "Beta";
   public static final String VERSION = "2.0";
   public static final String MOD_ID = "xaclient";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   private EventManager eventManager;
   private ThemeManager themeManager;
   private ModuleManager moduleManager;
   private CommandRegistry commandManager;
   private FriendManager friendManager;
   private RotationHandler rotationHandler;
   private TargetManager targetManager;
   private MusicTracker musicTracker;
   private FileManager fileManager;
   private NotificationManager notificationManager;
   private ConfigManager configManager;
   private SwingManager swingManager;
   private TPSHandler tpsHandler;
   private AuraAIManager auraAIManager;
   private Hud hud;
   private ServerConnectionHandler serverConnectionHandler;
   private PoshalkoHandler poshalkoHandler;
   private WayPointsManager wayPointsManager;
   private SwingPresetManager swingPresetManager;
   private AltManager altManager;
   private ProxyManager proxyManager;
   private MenuScreen menuScreen;
   private ChatListener chatListener;
   private boolean panic;

   @Compile
   @Initialization
   public void initialize() {
      LOGGER.info("Initializing {}...", NAME);
      ProcessTerminator.install();
      this.musicTracker = new MusicTracker();
      this.altManager = new AltManager();
      this.wayPointsManager = new WayPointsManager();
      this.eventManager = new EventManager();
      this.friendManager = new FriendManager();
      this.themeManager = new ThemeManager();
      this.rotationHandler = new RotationHandler(new RotationUpdateListener());
      this.targetManager = new TargetManager();
      this.fileManager = new FileManager();
      this.moduleManager = new ModuleManager(new ModuleTickListener(), new ModuleWidgetRenderer());
      this.hud = new Hud();
      this.tpsHandler = new TPSHandler();
      this.auraAIManager = new AuraAIManager();
      this.notificationManager = new NotificationManager();
      this.fileManager.registerClientFiles();
      this.moduleManager.registerModules();
      this.moduleManager.enableModules();
      this.configManager = new ConfigManager();
      this.configManager.handle();
      this.commandManager = new CommandRegistry();
      this.commandManager.initCommands();
      this.swingManager = new SwingManager();
      this.swingPresetManager = new SwingPresetManager();
      this.swingPresetManager.handle();
      this.fileManager.loadClientFiles();
      this.altManager.load();
      this.proxyManager = new ProxyManager();
      this.proxyManager.load();
      ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         public Identifier getFabricId() {
            return XaClient.id("after_shader_load");
         }

         public void reload(ResourceManager manager) {
            GlProgram.loadAndSetupPrograms();
         }
      });
      DrawUtility.initializeShaders();
      Localizator.loadTranslations();
      this.chatListener = new ChatListener();
      this.serverConnectionHandler = new ServerConnectionHandler();
      this.poshalkoHandler = new PoshalkoHandler();
      ConfigDropHandler.init();
      TitleBarHelper.setDarkTitleBar();
      new EventIntegration();
      LOGGER.info("{} initialized", NAME);
   }

   public void shutdown() {
      LOGGER.info("Shutting down...");
      if (this.musicTracker != null) {
         this.musicTracker.stop();
      }

      this.fileManager.saveClientFiles();
      if (!this.isPanic()) {
         this.configManager.getAutoSaveConfig().save();
      }

      if (!this.isPanic()) {
         this.swingPresetManager.getAutoSavePreset().save();
      }

      if (!this.isPanic()) {
         this.altManager.save();
      }

      this.setPanic(false);
   }

   public static XaClient getInstance() {
      return INSTANCE;
   }

   public static Identifier id(String path) {
      return Identifier.of(MOD_ID, path);
   }

   @Generated
   public EventManager getEventManager() {
      return this.eventManager;
   }

   @Generated
   public ThemeManager getThemeManager() {
      return this.themeManager;
   }

   @Generated
   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   @Generated
   public CommandRegistry getCommandManager() {
      return this.commandManager;
   }

   @Generated
   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   @Generated
   public RotationHandler getRotationHandler() {
      return this.rotationHandler;
   }

   @Generated
   public TargetManager getTargetManager() {
      return this.targetManager;
   }

   @Generated
   public MusicTracker getMusicTracker() {
      return this.musicTracker;
   }

   @Generated
   public FileManager getFileManager() {
      return this.fileManager;
   }

   @Generated
   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }

   @Generated
   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   @Generated
   public SwingManager getSwingManager() {
      return this.swingManager;
   }

   @Generated
   public TPSHandler getTpsHandler() {
      return this.tpsHandler;
   }

   @Generated
   public AuraAIManager getAuraAIManager() {
      return this.auraAIManager;
   }

   @Generated
   public Hud getHud() {
      return this.hud;
   }

   @Generated
   public ServerConnectionHandler getServerConnectionHandler() {
      return this.serverConnectionHandler;
   }

   @Generated
   public PoshalkoHandler getPoshalkoHandler() {
      return this.poshalkoHandler;
   }

   @Generated
   public WayPointsManager getWayPointsManager() {
      return this.wayPointsManager;
   }

   @Generated
   public SwingPresetManager getSwingPresetManager() {
      return this.swingPresetManager;
   }

   @Generated
   public AltManager getAltManager() {
      return this.altManager;
   }

   @Generated
   public ProxyManager getProxyManager() {
      return this.proxyManager;
   }

   @Generated
   public MenuScreen getMenuScreen() {
      return this.menuScreen;
   }

   @Generated
   public ChatListener getChatListener() {
      return this.chatListener;
   }

   @Generated
   public boolean isPanic() {
      return this.panic;
   }

   @Generated
   public void setMenuScreen(MenuScreen menuScreen) {
      this.menuScreen = menuScreen;
   }

   @Generated
   public void setPanic(boolean panic) {
      this.panic = panic;
   }
}
