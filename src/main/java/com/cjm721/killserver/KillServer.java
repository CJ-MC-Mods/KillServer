package com.cjm721.killserver;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class KillServer implements ModInitializer {
  private static final Logger LOGGER = LogManager.getLogger();

  @Override
  public void onInitialize() {
    ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
  }

  public void onServerStarting(MinecraftServer server) {
    Thread watcherThread = new Thread(() -> watchServer(server, server.getThread()));
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
              server.save(false, true, true);
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
        System.exit(10);
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
