package com.cjm721.killserver;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("killserver")
public class KillServer {
  private static final Logger LOGGER = LogManager.getLogger();

  public KillServer() {
    // Register ourselves for server and other game events we are interested in
    MinecraftForge.EVENT_BUS.register(this);
  }

  // You can use SubscribeEvent and let the Event Bus discover methods to call
  @SubscribeEvent
  public void onServerStarting(FMLServerStartingEvent event) {
    Thread serverThread = Thread.currentThread();
    Thread watcherThread = new Thread(() -> {
      LOGGER.info("Starting Server Thread Watcher");
      while(true) {
        if (!serverThread.isAlive()) {
          LOGGER.fatal("Server Thread has been detected as dead. Waiting 10 seconds to kill server");
          try {
            Thread.sleep(10 * 1000L);
          } catch (InterruptedException ignored) {
          }
          LOGGER.fatal("10 seconds has pasted since Server Thread was dead. KILLING SERVER");
          System.exit(2);
        }
        try {
          Thread.sleep(10 * 1000L);
        } catch (InterruptedException ignored) {
        }
      }
    });
    watcherThread.setDaemon(true);
    watcherThread.setName("KillServer");
    watcherThread.start();
  }
}
