package com.besaba.revonline.snippetide.run;

import com.besaba.revonline.snippetide.api.events.manager.EventManager;
import com.besaba.revonline.snippetide.api.events.run.MessageFromProcess;
import com.besaba.revonline.snippetide.api.events.run.RunInformationEvent;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

/**
 * Please make sure to call methods stop / start from only the main thread
 */
public class RunSnippet implements Runnable {
  private final static Logger logger = Logger.getLogger(RunSnippet.class);
  private volatile boolean running;
  private volatile Thread workingThread;

  private final RunInformationEvent runInformationEvent;
  @NotNull
  private final EventManager eventManager;

  public RunSnippet(@NotNull final RunInformationEvent runInformationEvent,
                    @NotNull final EventManager eventManager) {
    this.runInformationEvent = runInformationEvent;
    this.eventManager = eventManager;
  }

  /**
   * Call it only from one thread (javafx thread?)
   */
  public void stop() {
    if (workingThread != null) {
      workingThread.interrupt();
      workingThread = null;
    }

    running = false;
  }

  /**
   * Call it only from one thread (javafx thread?)
   */
  public void start() {
    logger.debug("start called -> workingThread is " + workingThread);

    if (workingThread != null) {
      workingThread.interrupt();
      workingThread = null;
    }

    running = true;
    workingThread = new Thread(this);
    workingThread.start();
  }

  public boolean isRunning() {
    return running;
  }

  @Override
  public void run() {
    logger.debug("run started by some thread " + Thread.currentThread());

    final String command = runInformationEvent.getCommand();
    final Path workingDirectory = runInformationEvent.getRunStartEvent().getSourceFile().toAbsolutePath();

    try {
      execute(command, workingDirectory);
    } catch (IOException|InterruptedException e) {
      logger.fatal("During processing messages from subprocess", e);

      final StringWriter stringWriter = new StringWriter();
      final PrintWriter printWriter = new PrintWriter(stringWriter);
      e.printStackTrace(printWriter);
      final String message = stringWriter.toString();

      eventManager.post(new MessageFromProcess(message));
    }

    workingThread = null;
  }

  private void execute(final String command, final Path workingDirectory) throws IOException, InterruptedException {
    final Process process = new ProcessBuilder(command)
        .directory(workingDirectory.getParent().toFile())
        .start();

    try(final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      while (running) {
        logger.debug("waiting for message");

        if (!running || workingThread.isInterrupted()) {
          logger.debug("running is " + running);
          logger.debug("workingThread.isInterrupted " + workingThread.isInterrupted());
          logger.debug("process is alive " + process.isAlive());
          throw new IOException("Exit forced by someone");
        }

        if (!process.isAlive()) {
          logger.debug("process is not alive");
          break;
        }

        if (reader.ready()) {
          logger.debug("reader is ready to read a message");

          final String message = reader.readLine();

          logger.debug("message read " + message);

          eventManager.post(new MessageFromProcess(message));
        }
      }

      logger.debug("ending... kill process forcibly");
      process.destroyForcibly();

      logger.debug("waiting for the exitCode (it should terminate now)");

      final int exitCode = process.waitFor();
      eventManager.post(new MessageFromProcess("Process finished with exit code " + exitCode));
      logger.debug("post exit message (exit is " + exitCode + ")");
    } catch (IOException | InterruptedException e) {
      logger.fatal("something went wrong in the message reading / exit process", e);
      process.destroyForcibly();
      throw e;
    }
  }
}
