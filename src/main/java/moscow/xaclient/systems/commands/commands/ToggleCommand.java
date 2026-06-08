package moscow.xaclient.systems.commands.commands;

import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.commands.Command;
import moscow.xaclient.systems.commands.CommandBuilder;
import moscow.xaclient.systems.commands.ParameterBuilder;
import moscow.xaclient.systems.commands.ParameterValidator;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.systems.modules.Module;
import moscow.xaclient.utility.game.MessageUtility;
import net.minecraft.text.Text;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class ToggleCommand {
      @Compile
      public Command command() {
            List<String> moduleNames = XaClient.getInstance().getModuleManager().getModules().stream()
                        .map(module -> module.getName().replace(" ", "")).toList();
            return CommandBuilder.begin("toggle")
                        .aliases("t")
                        .desc("commands.toggle.description")
                        .param("module",
                                    p -> p.validator(
                                                (moscow.xaclient.systems.commands.ParameterValidator) ParameterBuilder.MODULE)
                                                .suggests(moduleNames))
                        .handler(context -> {
                              Module module = (Module) context.arguments().getFirst();
                              module.toggle();
                              MessageUtility.info(Text.of(Localizator
                                          .translate("commands.toggle." + (module.isEnabled() ? "enabled" : "disabled"),
                                                      module.getName())));
                        })
                        .build();
      }
}
