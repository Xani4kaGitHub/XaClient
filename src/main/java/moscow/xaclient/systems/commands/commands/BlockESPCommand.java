package moscow.xaclient.systems.commands.commands;

import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.commands.Command;
import moscow.xaclient.systems.commands.CommandBuilder;
import moscow.xaclient.systems.commands.ParameterValidator;
import moscow.xaclient.systems.commands.ValidationResult;
import moscow.xaclient.systems.modules.modules.visuals.BlockESP;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.game.MessageUtility;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BlockESPCommand {
   private static final List<String> COLOR_SUGGESTIONS = List.of(
      "white", "black", "red", "green", "blue", "yellow", "cyan", "magenta", "gray", "orange", "pink"
   );
   private static final ParameterValidator<String> BLOCK = text -> {
      Identifier id = Identifier.tryParse(text.contains(":") ? text : "minecraft:" + text);
      return id != null && Registries.BLOCK.containsId(id) && Registries.BLOCK.get(id) != net.minecraft.block.Blocks.AIR
         ? ValidationResult.ok(id.toString())
         : ValidationResult.error("Unknown block: " + text);
   };
   private static final ParameterValidator<ColorRGBA> COLOR = text -> {
      ColorRGBA color = parseColor(text);
      return color != null ? ValidationResult.ok(color) : ValidationResult.error("Invalid color: " + text);
   };

   public Command command() {
      List<String> blocks = Registries.BLOCK.stream().map(block -> Registries.BLOCK.getId(block).getPath()).toList();
      return CommandBuilder.begin(
            "blockesp",
            b -> b.hub()
               .subcommand(
                  CommandBuilder.begin("add")
                     .param("block", p -> p.validator((ParameterValidator)BLOCK).suggests(blocks))
                     .param("color", p -> p.validator((ParameterValidator)COLOR).suggests(COLOR_SUGGESTIONS))
                     .handler(ctx -> {
                        String blockId = (String)ctx.arguments().get(0);
                        ColorRGBA color = (ColorRGBA)ctx.arguments().get(1);
                        BlockESP blockESP = XaClient.getInstance().getModuleManager().getModule(BlockESP.class);
                        if (blockESP.addCustomBlock(blockId, color)) {
                           MessageUtility.info(Text.of("Added " + blockId + " to Block ESP"));
                        } else {
                           MessageUtility.error(Text.of("Could not add " + blockId + " to Block ESP"));
                        }
                     })
                     .build()
               )
               .subcommand(
                  CommandBuilder.begin("remove")
                     .aliases("del", "delete")
                     .param("block", p -> p.validator((ParameterValidator)BLOCK).suggests(blocks))
                     .handler(ctx -> {
                        String blockId = (String)ctx.arguments().get(0);
                        BlockESP blockESP = XaClient.getInstance().getModuleManager().getModule(BlockESP.class);
                        if (blockESP.removeCustomBlock(blockId)) {
                           MessageUtility.info(Text.of("Removed " + blockId + " from Block ESP"));
                        } else {
                           MessageUtility.warn(Text.of(blockId + " is not in Block ESP"));
                        }
                     })
                     .build()
               )
               .subcommand(
                  CommandBuilder.begin("list")
                     .handler(ctx -> {
                        BlockESP blockESP = XaClient.getInstance().getModuleManager().getModule(BlockESP.class);
                        if (blockESP.getCustomBlocks().isEmpty()) {
                           MessageUtility.info(Text.of("Block ESP custom list is empty"));
                        } else {
                           MessageUtility.info(Text.of("Block ESP custom blocks: " + String.join(", ", blockESP.getCustomBlocks().keySet())));
                        }
                     })
                     .build()
               ))
         .build();
   }

   private static ColorRGBA parseColor(String text) {
      return switch (text.toLowerCase()) {
         case "white" -> ColorRGBA.WHITE;
         case "black" -> ColorRGBA.BLACK;
         case "red" -> ColorRGBA.RED;
         case "green" -> ColorRGBA.GREEN;
         case "blue" -> ColorRGBA.BLUE;
         case "yellow" -> ColorRGBA.YELLOW;
         case "cyan" -> new ColorRGBA(0.0F, 255.0F, 255.0F);
         case "magenta" -> new ColorRGBA(255.0F, 0.0F, 255.0F);
         case "gray", "grey" -> new ColorRGBA(128.0F, 128.0F, 128.0F);
         case "orange" -> new ColorRGBA(255.0F, 165.0F, 0.0F);
         case "pink" -> new ColorRGBA(255.0F, 105.0F, 180.0F);
         default -> parseHexColor(text);
      };
   }

   private static ColorRGBA parseHexColor(String text) {
      String hex = text.startsWith("#") ? text.substring(1) : text;
      if (hex.length() != 6 && hex.length() != 8) {
         return null;
      }

      try {
         return ColorRGBA.fromHex(hex.length() == 6 ? "#" + hex + "FF" : "#" + hex);
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }
}
