package moscow.xaclient.utility.game;

import com.sun.jna.platform.win32.Kernel32;
import moscow.xaclient.XaClient;

public final class ProcessTerminator {
   private static volatile boolean installed;

   private ProcessTerminator() {
   }

   public static void install() {
      if (installed) {
         return;
      }

      installed = true;
      Thread hook = new Thread(ProcessTerminator::forceExit, "XaClient-ForceExit");
      Runtime.getRuntime().addShutdownHook(hook);
   }

   private static void forceExit() {
      try {
         Thread.sleep(800L);
      } catch (InterruptedException ignored) {
         Thread.currentThread().interrupt();
      }

      try {
         if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            // TerminateProcess завершает процесс немедленно, не вызывая DllMain(DLL_PROCESS_DETACH).
            // Это обходит зависание WinRT/COM-потоков внутри MediaPlayerInfo.dll, из-за которого
            // ExitProcess не завершался и игра оставалась в диспетчере задач после закрытия.
            Kernel32.INSTANCE.TerminateProcess(Kernel32.INSTANCE.GetCurrentProcess(), 0);
         }
      } catch (Throwable t) {
         XaClient.LOGGER.error("TerminateProcess failed, falling back to halt", t);
      }

      Runtime.getRuntime().halt(0);
   }
}
