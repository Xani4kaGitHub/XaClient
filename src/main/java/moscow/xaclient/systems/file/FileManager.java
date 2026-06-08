package moscow.xaclient.systems.file;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import moscow.xaclient.systems.file.impl.ClientDataFile;
import net.minecraft.client.MinecraftClient;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.Initialization;

public class FileManager {
   public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public static final File DIRECTORY = new File(MinecraftClient.getInstance().runDirectory, "XaClient");
   public static final String DEFAULT_FILE_TYPE = "xani";
   public static final String LEGACY_FILE_TYPE = "rock";
   private final List<ClientFile> clientFiles = new ArrayList<>();

   public FileManager() {
      try {
         if (!DIRECTORY.exists()) {
            Files.createDirectories(Path.of(DIRECTORY.toURI()));
         }
      } catch (IOException var2) {
         System.err.println("Error creating directory: " + var2.getMessage());
      }
   }

   @Initialization
   public void registerClientFiles() {
      this.clientFiles.add(new ClientDataFile());
   }

   public ClientFile getClientFile(String clientFileName) {
      return this.clientFiles.stream().filter(clientFile -> clientFile.getInfoAnnotation().name().equalsIgnoreCase(clientFileName)).findFirst().orElse(null);
   }

   public void readFile(ClientFile clientFile) {
      try {
         if (clientFile.getReadableFile().exists()) {
            clientFile.read();
         }
      } catch (Exception var3) {
         System.err.println("Error reading file: " + var3.getMessage());
      }
   }

   public void readFile(String clientFileName) {
      ClientFile clientFile = this.getClientFile(clientFileName);
      if (clientFile != null) {
         this.readFile(clientFile);
      }
   }

   public void writeFile(ClientFile clientFile) {
      try {
         if (!clientFile.getFile().exists()) {
            clientFile.getFile().createNewFile();
         }

         clientFile.write();
      } catch (IOException var3) {
         System.err.println("Error saving file: " + var3.getMessage());
      }
   }

   public void writeFile(String clientFileName) {
      ClientFile clientFile = this.getClientFile(clientFileName);
      if (clientFile != null) {
         clientFile.write();
      }
   }

   public static boolean hasSupportedExtension(String fileName) {
      String extension = getExtension(fileName);
      return DEFAULT_FILE_TYPE.equals(extension) || LEGACY_FILE_TYPE.equals(extension);
   }

   public static String stripSupportedExtension(String fileName) {
      String name = new File(fileName).getName();
      String extension = getExtension(name);
      if (DEFAULT_FILE_TYPE.equals(extension) || LEGACY_FILE_TYPE.equals(extension)) {
         return name.substring(0, name.length() - extension.length() - 1);
      }

      return name;
   }

   private static String getExtension(String fileName) {
      int dotIndex = fileName.lastIndexOf('.');
      if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
         return "";
      }

      return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
   }

   @Compile
   @Initialization
   public void loadClientFiles() {
      for (ClientFile file : this.clientFiles) {
         this.readFile(file);
      }
   }

   public void saveClientFiles() {
      for (ClientFile file : this.clientFiles) {
         this.writeFile(file);
      }
   }

   @Generated
   public List<ClientFile> getClientFiles() {
      return this.clientFiles;
   }
}
