package moscow.xaclient.systems.airdrop;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import lombok.Generated;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.config.ConfigFile;
import moscow.xaclient.systems.config.ConfigManager;
import moscow.xaclient.systems.file.FileManager;
import moscow.xaclient.utility.interfaces.IMinecraft;

public class ConfigUploadServer extends NanoHTTPD implements IMinecraft {
   private final File directory = new File(FileManager.DIRECTORY, "configs");
   private String name;
   private boolean render;

   public ConfigUploadServer() throws IOException {
      super(5656);
      if (!this.directory.exists() && !this.directory.mkdirs()) {
         throw new IOException("Failed to create configs directory: " + this.directory);
      }

      this.start(5000, false);
      XaClient.LOGGER.info("Config upload server started on port 5656, configs directory: {}", this.directory.getAbsolutePath());
   }

   public Response serve(IHTTPSession session) {
      if (Method.POST.equals(session.getMethod())) {
         return this.handleUpload(session);
      }

      this.render = false;
      return newFixedLengthResponse(Status.OK, "text/html", this.uploadPage());
   }

   private Response handleUpload(IHTTPSession session) {
      try {
         Map<String, String> files = new HashMap<>();
         session.parseBody(files);
         String tmpPath = files.get("file");
         if (tmpPath == null) {
            return newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "No file received");
         }

         String originalName = session.getParms().get("file");
         if (originalName == null || originalName.isEmpty()) {
            originalName = "client." + FileManager.DEFAULT_FILE_TYPE;
         }

         if (!FileManager.hasSupportedExtension(originalName)) {
            return newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Only .xani and .rock configs are supported");
         }

         String configName = FileManager.stripSupportedExtension(originalName);
         File dest = new File(this.directory, configName + "." + FileManager.DEFAULT_FILE_TYPE);
         Files.copy(Path.of(tmpPath), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

         this.name = dest.getName();
         this.render = true;

         ConfigManager mgr = XaClient.getInstance().getConfigManager();
         mgr.refresh();
         ConfigFile cfg = mgr.getConfig(configName);
         if (cfg == null) {
            cfg = new ConfigFile(configName);
            mgr.getConfigFiles().add(cfg);
         }

         cfg.load();
         return newFixedLengthResponse("Config loaded");
      } catch (Exception exception) {
         return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "Upload error: " + exception.getMessage());
      }
   }

   private String uploadPage() {
      return """
         <!DOCTYPE html>
         <html lang="ru">
         <head>
           <meta charset="UTF-8">
           <meta name="viewport" content="width=device-width, initial-scale=1.0">
           <title>Config Upload</title>
           <style>
             body {
               font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
               display: flex;
               flex-direction: column;
               align-items: center;
               justify-content: center;
               height: 100vh;
               margin: 0;
               padding: 20px;
               background: #f2f2f2;
             }
             h1 { font-size: 1.8em; margin-bottom: 1em; text-align: center; }
             form {
               display: flex;
               flex-direction: column;
               gap: 15px;
               width: 100%;
               max-width: 400px;
               background: #fff;
               padding: 20px;
               border-radius: 12px;
               box-shadow: 0 4px 10px rgba(0,0,0,0.1);
             }
             input[type="file"] { font-size: 1.1em; }
             input[type="submit"] {
               padding: 12px;
               font-size: 1.2em;
               border: none;
               border-radius: 8px;
               background: #007aff;
               color: white;
               cursor: pointer;
               transition: background 0.3s ease;
             }
             input[type="submit"]:hover { background: #005fcc; }
           </style>
         </head>
         <body>
           <h1>Upload XaClient config</h1>
           <form method="POST" enctype="multipart/form-data">
             <input type="file" name="file" accept=".xani,.rock" required />
             <input type="submit" value="Upload" />
           </form>
         </body>
         </html>
         """;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public boolean isRender() {
      return this.render;
   }
}
