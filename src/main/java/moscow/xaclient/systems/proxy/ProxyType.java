package moscow.xaclient.systems.proxy;

import java.net.InetSocketAddress;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

public enum ProxyType {
   DIRECT("direct"),
   HTTP("http"),
   SOCKS4("socks4"),
   SOCKS5("socks5");

   private final String prefix;

   ProxyType(String prefix) {
      this.prefix = prefix;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public ProxyHandler createHandler(InetSocketAddress address, String username, String password) {
      boolean hasAuth = username != null && !username.isEmpty();
      return switch (this) {
         case HTTP -> hasAuth ? new HttpProxyHandler(address, username, password) : new HttpProxyHandler(address);
         case SOCKS4 -> hasAuth ? new Socks4ProxyHandler(address, username) : new Socks4ProxyHandler(address);
         case SOCKS5 -> hasAuth ? new Socks5ProxyHandler(address, username, password) : new Socks5ProxyHandler(address);
         default -> null;
      };
   }

   public static ProxyType fromPrefix(String input) {
      for (ProxyType type : values()) {
         if (type != DIRECT && input.startsWith(type.prefix + "://")) {
            return type;
         }
      }

      return DIRECT;
   }
}
