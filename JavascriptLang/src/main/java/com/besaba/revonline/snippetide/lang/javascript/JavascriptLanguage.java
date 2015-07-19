package com.besaba.revonline.snippetide.lang.javascript;

import com.besaba.revonline.snippetide.api.application.IDEApplicationLauncher;
import com.besaba.revonline.snippetide.api.events.manager.EventManager;
import com.besaba.revonline.snippetide.api.events.run.MessageFromProcess;
import com.besaba.revonline.snippetide.api.events.run.RunStartEvent;
import com.besaba.revonline.snippetide.api.language.Language;
import com.besaba.revonline.snippetide.api.run.RunConfiguration;
import com.google.common.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class JavascriptLanguage implements Language {
  @NotNull
  public String getName() {
    return "Javascript";
  }

  @NotNull
  public String[] getExtensions() {
    return new String[] {".js"};
  }

  @NotNull
  public String getTemplate() {
    return "function helloWorld() { console.log(\"Hello world\"); }\nhelloWorld();";
  }

  @NotNull
  public RunConfiguration[] getRunConfigurations() {
    return new RunConfiguration[0];
  }

  @Subscribe
  public void onRunEvent(final RunStartEvent runStartEvent) {
    final ScriptEngineManager factory = new ScriptEngineManager();
    final ScriptEngine scriptEngine = factory.getEngineByName("JavaScript");
    final EventManager eventManager = IDEApplicationLauncher.getIDEApplication().getEventManager();
    final StringWriter writer = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(writer);

    scriptEngine.getContext().setWriter(printWriter);
    scriptEngine.getContext().setErrorWriter(printWriter);

    try(final FileReader fileReader = new FileReader(runStartEvent.getSourceFile().toFile())) {
      scriptEngine.eval(fileReader);
      eventManager.post(new MessageFromProcess(writer.toString()));
    } catch (IOException|ScriptException e) {
      final String message = stacktraceToString(e);
      eventManager.post(new MessageFromProcess(message));
    }
  }

  private String stacktraceToString(final Throwable e) {
    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);
    e.printStackTrace(printWriter);
    return stringWriter.toString();
  }
}
