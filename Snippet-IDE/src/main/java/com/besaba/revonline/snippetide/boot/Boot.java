package com.besaba.revonline.snippetide.boot;


import com.besaba.revonline.snippetide.api.application.IDEApplication;
import com.besaba.revonline.snippetide.api.application.IDEApplicationLauncher;
import com.besaba.revonline.snippetide.api.events.manager.EventManager;
import com.besaba.revonline.snippetide.api.plugins.Plugin;
import com.besaba.revonline.snippetide.events.manager.impl.EventBusEventManager;
import com.besaba.revonline.snippetide.api.plugins.PluginManager;
import com.besaba.revonline.snippetide.api.plugins.UnableToLoadPluginException;
import com.besaba.revonline.snippetide.application.IDEApplicationImpl;
import com.besaba.revonline.snippetide.plugins.JarPluginManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Prepare the application
 */
public class Boot {
  private static final Path APPLICATION_PATH = Paths.get( System.getProperty("user.dir") , "SnippetIDE" );

  private volatile boolean booted;
  private static final Logger logger = Logger.getLogger(Boot.class);
  private IDEApplication ideApplication;

  public Boot() {
    try {
      IDEApplicationLauncher.getIDEApplication();
      booted = true;
    } catch (IllegalStateException ex) {
      booted = false;
    }
  }

  public IDEApplication boot() {
    return boot(APPLICATION_PATH, null, null);
  }

  /**
   * Prepare and initialize everything inside the application.
   *
   * @param applicationPath The path where are all the application files (plugins, etc)
   * @return The booted application
   * @throws IllegalStateException This method can be invoked only one time, if you try to invoke
   *                               it again it will throw this exception
   * @throws BootFailedException Throw when the boot of the application failed.
   *                             It can be anything, maybe the boot has failed to create
   *                             the directories or some configuration files are invalid
   */
  public IDEApplication boot(@NotNull final Path applicationPath,
                             @Nullable EventManager eventManager,
                             @Nullable PluginManager pluginManager) {
    if (booted) {
      throw new IllegalStateException("Application already started");
    }

    if (eventManager == null) {
      eventManager = new EventBusEventManager();
    }

    if (pluginManager == null) {
      pluginManager = new JarPluginManager();
    }

    logger.info("Boot phase started");

    final String applicationPathAsString = applicationPath.toString();
    final IDEApplication application = new IDEApplicationImpl(
        eventManager,
        pluginManager,
        applicationPath,
        Paths.get(applicationPathAsString, "plugins"),
        Paths.get(applicationPathAsString, "temp")
    );

    createDirectories();

    IDEApplicationLauncher.createApplication(application);
    loadPlugins(pluginManager, eventManager);

    ideApplication = application;
    booted = true;
    return application;
  }

  private void createDirectories() {
    final List<Path> pathsToCreate = new ArrayList<>(Arrays.asList(
        ideApplication.getApplicationDirectory(),
        ideApplication.getPluginsDirectory(),
        ideApplication.getTemporaryDirectory()
    ));

    for (final Path path : pathsToCreate) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        throw new BootFailedException("Unable to create application directory: " + path, e);
      }
    }
  }

  private void loadPlugins(@NotNull final PluginManager pluginManager,
                           @NotNull final EventManager eventManager) {
    final Path pluginPath = ideApplication.getPluginsDirectory();
    try {
      Files.walkFileTree(pluginPath, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
          try {
            final Plugin plugin = pluginManager.loadPlugin(file, ideApplication.getVersion());
            // we need to register the languages created by the plugin not the plugin class!
            plugin.getLanguages().forEach(eventManager::registerListener);

          } catch (UnableToLoadPluginException e) {
            logger.fatal("Unable to load plugin " + e.getFileLocation() + "! The manager is " + e.getPluginManager(), e);
          } catch (Exception e) {
            logger.fatal("Unable to load plugin " + file + ".", e);
          }

          logger.info("Loaded plugin " + file + "!");

          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      logger.fatal("Something went wrong during reading the plugin directory", e);
    }

    logger.info("Loaded " + pluginManager.getPluginsCount() + " plugin(s)");
  }

  public void unboot() {
    unboot(APPLICATION_PATH);
  }

  public void unboot(final Path applicationPath) {
    logger.info("Unboot phase started");

    try {
      cleanDirectory(ideApplication.getTemporaryDirectory());
    } catch (IOException e) {
      logger.fatal("Unboot failed", e);
    }

    logger.info("Unboot ended");
  }

  private void cleanDirectory(final Path temporaryDirectory) throws IOException {
    Files.walkFileTree(temporaryDirectory, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public boolean isBooted() {
    return booted;
  }
}
