package moscow.xaclient.systems.proxy;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.ProxyHandler;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;

public class ProxyManager {
   private final File file = new File(MinecraftClient.getInstance().runDirectory, "files/proxy.ew");
   private ProxyType type = ProxyType.DIRECT;
   private InetSocketAddress address;
   private String username = "";
   private String password = "";
   private boolean enabled;
   private String lastError = "";

   public void load() {
      if (!this.file.exists()) {
         return;
      }

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.file), StandardCharsets.UTF_8))) {
         String address = reader.readLine();
         String enabled = reader.readLine();
         String username = reader.readLine();
         String password = reader.readLine();
         if (address != null && !address.isBlank()) {
            this.parse(address.trim());
         }

         if (username != null && !username.isBlank()) {
            this.username = username.trim();
            this.password = password == null ? "" : password;
         }

         this.enabled = "true".equalsIgnoreCase(enabled == null ? "" : enabled.trim()) && this.address != null;
      } catch (IOException exception) {
         this.lastError = "Failed to load proxy";
         exception.printStackTrace();
      }
   }

   public void save() {
      try {
         File parent = this.file.getParentFile();
         if (parent != null) {
            parent.mkdirs();
         }

         try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8))) {
            writer.write(this.address != null ? this.formatted() : "");
            writer.newLine();
            writer.write(Boolean.toString(this.enabled));
            writer.newLine();
            writer.write(this.username == null ? "" : this.username);
            writer.newLine();
            writer.write(this.password == null ? "" : this.password);
         }
      } catch (IOException exception) {
         this.lastError = "Failed to save proxy";
         exception.printStackTrace();
      }
   }

   public boolean parse(String input) {
      if (input == null || input.isBlank()) {
         this.reset();
         return false;
      }

      input = input.trim();

      try {
         ProxyType type = ProxyType.fromPrefix(input.toLowerCase(Locale.ROOT));
         String body = input.contains("//") ? input.split("//", 2)[1] : input;

         String username = "";
         String password = "";
         int at = body.lastIndexOf('@');
         if (at > 0) {
            String credentials = body.substring(0, at);
            String[] parts = credentials.split(":", 2);
            username = parts[0];
            password = parts.length > 1 ? parts[1] : "";
            body = body.substring(at + 1);
         }

         int separator = body.lastIndexOf(':');
         if (type == ProxyType.DIRECT || separator <= 0) {
            this.lastError = "Format: socks5://host:port";
            return false;
         }

         String host = body.substring(0, separator);
         int port = Integer.parseInt(body.substring(separator + 1));
         this.type = type;
         this.address = new InetSocketAddress(host, port);
         this.username = username;
         this.password = password;
         this.lastError = "";
         this.save();
         return true;
      } catch (Exception exception) {
         this.lastError = "Invalid proxy address";
         this.reset();
         return false;
      }
   }

   public void reset() {
      this.type = ProxyType.DIRECT;
      this.address = null;
      this.username = "";
      this.password = "";
      this.enabled = false;
      this.save();
   }

   public boolean toggle() {
      if (this.address == null) {
         this.enabled = false;
         this.lastError = "No proxy configured";
         return false;
      }

      this.enabled = !this.enabled;
      this.lastError = "";
      this.save();
      return this.enabled;
   }

   public boolean isActive() {
      return this.enabled && this.address != null && this.type != ProxyType.DIRECT;
   }

   public void apply(ChannelPipeline pipeline) {
      if (!this.isActive()) {
         return;
      }

      ProxyHandler handler = this.type.createHandler(this.address, this.username, this.password);
      if (handler != null && pipeline.get("xaclient_proxy") == null) {
         pipeline.addFirst("xaclient_proxy", handler);
      }
   }

   public String formatted() {
      if (this.address == null) {
         return "none";
      }

      return this.type.getPrefix() + "://" + this.address.getHostString() + ":" + this.address.getPort();
   }

   @Generated
   public ProxyType getType() {
      return this.type;
   }

   @Generated
   public InetSocketAddress getAddress() {
      return this.address;
   }

   @Generated
   public String getUsername() {
      return this.username;
   }

   @Generated
   public String getPassword() {
      return this.password;
   }

   @Generated
   public boolean isEnabled() {
      return this.enabled;
   }

   @Generated
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      this.save();
   }

   @Generated
   public String getLastError() {
      return this.lastError;
   }
}
