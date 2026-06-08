package moscow.xaclient.systems.file;

import java.io.File;
import lombok.Generated;
import moscow.xaclient.systems.file.api.FileInfo;

public abstract class ClientFile {
   public final FileInfo infoAnnotation = this.getClass().getAnnotation(FileInfo.class);
   public final File file = new File(FileManager.DIRECTORY, this.infoAnnotation.name() + "." + this.infoAnnotation.fileType());
   private final File legacyFile = new File(FileManager.DIRECTORY, this.infoAnnotation.name() + "." + FileManager.LEGACY_FILE_TYPE);

   public abstract void write();

   public abstract void read();

   @Generated
   public FileInfo getInfoAnnotation() {
      return this.infoAnnotation;
   }

   @Generated
   public File getFile() {
      return this.file;
   }

   public File getReadableFile() {
      return this.file.exists() || this.infoAnnotation.fileType().equals(FileManager.LEGACY_FILE_TYPE) ? this.file : this.legacyFile;
   }
}
