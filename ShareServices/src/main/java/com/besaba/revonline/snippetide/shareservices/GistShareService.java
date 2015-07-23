package com.besaba.revonline.snippetide.shareservices;

import com.besaba.revonline.snippetide.api.application.IDEApplicationLauncher;
import com.besaba.revonline.snippetide.api.events.manager.EventManager;
import com.besaba.revonline.snippetide.api.events.share.ShareCompletedEvent;
import com.besaba.revonline.snippetide.api.events.share.ShareRequestEvent;
import com.besaba.revonline.snippetide.api.shareservices.ShareService;
import com.google.common.eventbus.Subscribe;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GistShareService implements ShareService {
  private final EventManager eventManager = IDEApplicationLauncher.getIDEApplication().getEventManager();

  @NotNull
  public String getServiceName() {
    return "GitHub Gist";
  }

  @NotNull
  public Image getImage() {
    return new Image(GistShareService.class.getResourceAsStream("github_logo.png"));
  }

  @Subscribe
  public void share(@NotNull final ShareRequestEvent event) throws IOException {
    if (event.getTarget() != this) {
      return;
    }

    final String fileName = event.getFileName();
    final String code = event.getCode();

    final HttpURLConnection httpURLConnection
        = ((HttpURLConnection) new URL("https://api.github.com/gists/").openConnection());
    final String completeJson = prepareJsonResponse(fileName, code);

    httpURLConnection.setDoOutput(true);
    httpURLConnection.setRequestMethod("POST");
    httpURLConnection.connect();

    try(final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpURLConnection.getOutputStream(), StandardCharsets.UTF_8)) {
      outputStreamWriter.write(completeJson);
      outputStreamWriter.flush();

      final int response = httpURLConnection.getResponseCode();

      if (response != 201) {
        throw new IOException("Unable to post it :( (" + response + ")");
      }

      final String location = httpURLConnection.getHeaderField("Location");
      eventManager.post(new ShareCompletedEvent(location, this));
    } finally {
      httpURLConnection.disconnect();
    }
  }

  private String prepareJsonResponse(@NotNull final String fileName, @NotNull final String code) {
    // the quote method adds the "
    final String rawJson = "{\n" +
        "  \"description\": \"SnippetIDE\", \n" +
        "  \"public\": true, \n" +
        "  \"files\": {\n" +
        "    %s: {\n" +
        "      \"content\": %s\n" +
        "    }\n" +
        "  }\n" +
        "}   ";

    final String escapedFileNameForJson = escapeForJson(fileName);
    final String escapedCodeForJson = escapeForJson(code);

    return String.format(rawJson, escapedFileNameForJson, escapedCodeForJson);
  }

  public static String escapeForJson(final String text) {
    return JsonParserQuote.quote(text);
  }
}
