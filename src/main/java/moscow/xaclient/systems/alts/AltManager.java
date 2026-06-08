package moscow.xaclient.systems.alts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import moscow.xaclient.mixin.minecraft.client.IMinecraftClient;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;

public class AltManager implements IMinecraft {
   private static final int MIN_USERNAME_LENGTH = 3;
   private static final int MAX_USERNAME_LENGTH = 16;
   private static final int RANDOM_MIN_LENGTH = 6;
   private static final int RANDOM_MAX_LENGTH = 10;
   private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
   private static final Random RANDOM = new Random();

   private final File file = new File(MinecraftClient.getInstance().runDirectory, "files/alts.ew");
   private final File lastAltFile = new File(MinecraftClient.getInstance().runDirectory, "files/lastAlt.ew");
   private final List<AltAccount> accounts = new ArrayList<>();
   private String lastSelectedAccount;
   private String lastError = "";

   public void init() {
      this.load();
   }

   public void load() {
      this.accounts.clear();
      this.ensureFile(this.file);

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.file), StandardCharsets.UTF_8))) {
         String line;
         while ((line = reader.readLine()) != null) {
            String username = line.trim();
            if (this.isValidUsername(username) && this.getAccount(username) == null) {
               this.accounts.add(new AltAccount(username, this.uuidFor(username)));
            }
         }
      } catch (IOException exception) {
         this.lastError = "Failed to load alts";
         exception.printStackTrace();
      }

      this.loadLastAlt();
   }

   public void save() {
      this.saveAccounts();
   }

   public void saveAccounts() {
      this.ensureFile(this.file);

      try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8))) {
         for (AltAccount account : this.accounts) {
            writer.write(account.getUsername());
            writer.newLine();
         }
      } catch (IOException exception) {
         this.lastError = "Failed to save alts";
         exception.printStackTrace();
      }
   }

   public boolean add(String username) {
      username = username == null ? "" : username.trim();
      if (!this.isValidUsername(username)) {
         this.lastError = "Nick must be 3-16 chars: A-Z, 0-9, _";
         return false;
      }

      if (this.getAccount(username) != null) {
         this.lastError = "This nick is already added";
         return false;
      }

      this.accounts.add(new AltAccount(username, this.uuidFor(username)));
      this.lastError = "";
      this.saveAccounts();
      return true;
   }

   public boolean remove(String username) {
      AltAccount account = this.getAccount(username);
      if (account == null) {
         return false;
      }

      this.accounts.remove(account);
      if (this.lastSelectedAccount != null && this.lastSelectedAccount.equalsIgnoreCase(account.getUsername())) {
         this.lastSelectedAccount = null;
         this.saveLastAlt();
      }

      this.lastError = "";
      this.saveAccounts();
      return true;
   }

   public void clearAll() {
      this.accounts.clear();
      this.lastSelectedAccount = null;
      this.lastError = "";
      this.saveAccounts();
      this.saveLastAlt();
   }

   public boolean login(String username) {
      AltAccount account = this.getAccount(username);
      if (account == null) {
         this.lastError = "Alt not found";
         return false;
      }

      this.apply(account);
      this.setLastSelectedAccount(account.getUsername());
      this.lastError = "";
      return true;
   }

   public AltAccount createRandom() {
      String username;
      do {
         username = this.randomUsername();
      } while (this.getAccount(username) != null);

      AltAccount account = new AltAccount(username, this.uuidFor(username));
      this.accounts.add(account);
      this.saveAccounts();
      this.apply(account);
      this.setLastSelectedAccount(account.getUsername());
      this.lastError = "";
      return account;
   }

   public List<AltAccount> getAccounts() {
      return Collections.unmodifiableList(this.accounts);
   }

   public AltAccount getAccount(String username) {
      if (username == null) {
         return null;
      }

      for (AltAccount account : this.accounts) {
         if (account.getUsername().equalsIgnoreCase(username)) {
            return account;
         }
      }

      return null;
   }

   public void setLastSelectedAccount(String account) {
      this.lastSelectedAccount = account;
      this.saveLastAlt();
   }

   public String getLastSelectedAccount() {
      return this.lastSelectedAccount;
   }

   public void saveLastAlt() {
      try {
         if (this.lastSelectedAccount == null || this.lastSelectedAccount.isBlank()) {
            if (this.lastAltFile.exists() && !this.lastAltFile.delete()) {
               try (BufferedWriter ignored = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.lastAltFile), StandardCharsets.UTF_8))) {
               }
            }
            return;
         }

         this.ensureFile(this.lastAltFile);
         try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.lastAltFile), StandardCharsets.UTF_8))) {
            writer.write(this.lastSelectedAccount);
         }
      } catch (IOException exception) {
         this.lastError = "Failed to save last alt";
         exception.printStackTrace();
      }
   }

   public void loadLastAlt() {
      if (!this.lastAltFile.exists()) {
         return;
      }

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.lastAltFile), StandardCharsets.UTF_8))) {
         String line = reader.readLine();
         if (line != null) {
            String username = line.trim();
            if (this.isValidUsername(username)) {
               this.lastSelectedAccount = username;
               this.apply(new AltAccount(username, this.uuidFor(username)));
            }
         }
      } catch (IOException exception) {
         this.lastError = "Failed to load last alt";
         exception.printStackTrace();
      }
   }

   public boolean isValidUsername(String username) {
      if (username == null || username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
         return false;
      }

      return this.hasValidUsernameChars(username);
   }

   public boolean isValidPartialUsername(String username) {
      return username != null && username.length() <= MAX_USERNAME_LENGTH && this.hasValidUsernameChars(username);
   }

   public String getLastError() {
      return this.lastError;
   }

   private boolean hasValidUsernameChars(String username) {
      for (char c : username.toCharArray()) {
         if (!Character.isLetterOrDigit(c) && c != '_') {
            return false;
         }
      }

      return true;
   }

   private void apply(AltAccount account) {
      Session session = new Session(account.getUsername(), account.getUuid(), "invalid_token", Optional.empty(), Optional.empty(), AccountType.MOJANG);
      ((IMinecraftClient)mc).setSession(session);
   }

   private String randomUsername() {
      int length = RANDOM_MIN_LENGTH + RANDOM.nextInt(RANDOM_MAX_LENGTH - RANDOM_MIN_LENGTH + 1);
      StringBuilder builder = new StringBuilder(length);

      while (builder.length() < length) {
         builder.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
      }

      return builder.toString();
   }

   private UUID uuidFor(String username) {
      return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
   }

   private void ensureFile(File targetFile) {
      try {
         File parent = targetFile.getParentFile();
         if (parent != null) {
            parent.mkdirs();
         }

         if (!targetFile.exists()) {
            targetFile.createNewFile();
         }
      } catch (IOException exception) {
         this.lastError = "Failed to create alt file";
         exception.printStackTrace();
      }
   }
}
