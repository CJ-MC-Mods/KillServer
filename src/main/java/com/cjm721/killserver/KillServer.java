package com.cjm721.killserver;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Mod(modid = "killserver", version = "2.1", serverSideOnly = true, acceptableRemoteVersions = "*", useMetadata = true)
public class KillServer {
  private static final Logger LOGGER = LogManager.getLogger();

  public KillServer() {
    // Register ourselves for server and other game events we are interested in
    MinecraftForge.EVENT_BUS.register(this);
  }

  @Mod.EventHandler
  public void onServerStarting(FMLServerStartingEvent event) {
    Thread serverThread = Thread.currentThread();
    Thread watcherThread = new Thread(() -> watchServer(event.getServer(), serverThread));
    watcherThread.setDaemon(true);
    watcherThread.setName("KillServer");
    watcherThread.start();
  }

  private void watchServer(MinecraftServer server, Thread serverThread) {
    LOGGER.info("Starting Server Thread Watcher");
    while (true) {
      if (!serverThread.isAlive()) {
        LOGGER.fatal("Server Thread has been detected as dead. Attempting Save.");
        try {
          Executors.newCachedThreadPool().submit(() -> {
            try {
              server.saveAllWorlds(false);
            } catch (Exception e) {
              LOGGER.fatal("Exception thrown while saving.", e);
              e.printStackTrace();
            }
          }).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        } catch (ExecutionException e) {
          LOGGER.fatal("Uncaught Exception while saving. ", e);
        } catch (TimeoutException e) {
          LOGGER.fatal("Unable to Save within 10 seconds. Continuing.");
        }
        LOGGER.fatal("Waiting 10 seconds before killing in case of non-sync saving triggered.");
        sleepWait();
        LOGGER.fatal("10 seconds are up. Printing Non-Daemon Threads: ");
        for (Map.Entry<Thread, StackTraceElement[]> entry :
            Thread.getAllStackTraces().entrySet().stream()
                .filter(e -> !e.getKey().isDaemon())
                .collect(Collectors.toList())) {
          LOGGER.fatal(entry.getKey().toString());
          for (StackTraceElement s : entry.getValue()) {
            LOGGER.fatal("\tat " + s.getClassName() + "." + s.getMethodName()
                + "(" + s.getFileName() + ":" + s.getLineNumber() + ")");
          }
          LOGGER.fatal("----------------------------------");
        }
        LOGGER.fatal("KILLING NOW");
        FMLCommonHandler.instance().exitJava(10, true);
      }
      sleepWait();
    }
  }

  private void sleepWait() {
    try {
      Thread.sleep(10 *1000);
    } catch (InterruptedException e) {}
  }
}
