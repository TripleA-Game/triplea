package games.strategy.engine.pbem;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.triplea.awt.OpenFileUtility;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import games.strategy.engine.framework.system.HttpProxy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

/**
 * Posts turn summaries to a NodeBB based forum of your choice.
 *
 * <p>
 * URL format is {@code https://your.forumurl.com/api/v2/topics/<topicID>}.
 * </p>
 */
@Log
@AllArgsConstructor(access = AccessLevel.PACKAGE)
abstract class NodeBbForumPoster implements IForumPoster {

  private final int topicId;
  private final String username;
  private final String password;

  abstract String getForumUrl();

  @Override
  public CompletableFuture<String> postTurnSummary(final String summary, final String title, final Path path) {
    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      final int userId = getUserId(client);
      final String token = getToken(client, userId);
      try {
        post(client, token, "### " + title + "\n" + summary, path);
        return CompletableFuture.completedFuture("Successfully posted!");
      } finally {
        deleteToken(client, userId, token);
      }
    } catch (final IOException | IllegalStateException e) {
      log.log(Level.SEVERE, "Failed to post game to forum", e);
      final CompletableFuture<String> result = new CompletableFuture<>();
      result.completeExceptionally(e);
      return result;
    }
  }

  private void post(final CloseableHttpClient client, final String token, final String text, final Path path)
      throws IOException {
    final HttpPost post = new HttpPost(getForumUrl() + "/api/v2/topics/" + topicId);
    addTokenHeader(post, token);
    post.setEntity(new UrlEncodedFormEntity(
        Collections.singletonList(new BasicNameValuePair("content",
            text + ((path != null) ? uploadSaveGame(client, token, path) : ""))),
        StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      final int code = response.getStatusLine().getStatusCode();
      if (code != HttpURLConnection.HTTP_OK) {
        throw new IllegalStateException("Forum responded with code " + code);
      }
    }
  }

  private String uploadSaveGame(final CloseableHttpClient client, final String token, final Path path)
      throws IOException {
    final HttpPost fileUpload = new HttpPost(getForumUrl() + "/api/v2/util/upload");
    fileUpload.setEntity(MultipartEntityBuilder.create()
        .addBinaryBody("files[]", path.toFile(), ContentType.APPLICATION_OCTET_STREAM, path.getFileName().toString())
        .build());
    HttpProxy.addProxy(fileUpload);
    addTokenHeader(fileUpload, token);
    try (CloseableHttpResponse response = client.execute(fileUpload)) {
      final int status = response.getStatusLine().getStatusCode();
      if (status == HttpURLConnection.HTTP_OK) {
        final String json = EntityUtils.toString(response.getEntity());
        return "\n[Savegame](" + new JSONArray(json).getJSONObject(0).getString("url") + ")";
      }
      throw new IllegalStateException("Failed to upload savegame, server returned Error Code "
          + status + "\nMessage:\n" + EntityUtils.toString(response.getEntity()));
    }
  }

  private void deleteToken(final CloseableHttpClient client, final int userId, final String token)
      throws IOException {
    final HttpDelete httpDelete = new HttpDelete(getForumUrl() + "/api/v2/users/" + userId + "/tokens/" + token);
    HttpProxy.addProxy(httpDelete);
    addTokenHeader(httpDelete, token);
    client.execute(httpDelete).close(); // ignore errors, execute and then close
  }

  private int getUserId(final CloseableHttpClient client) throws IOException {
    final JSONObject jsonObject = queryUserInfo(client);
    checkUser(jsonObject);
    return jsonObject.getInt("uid");
  }

  private void checkUser(final JSONObject jsonObject) {
    if (!jsonObject.has("uid")) {
      throw new IllegalStateException(String.format("User %s doesn't exist.", username));
    }
    if (jsonObject.getBoolean("banned")) {
      throw new IllegalStateException("Your account is banned from the forum.");
    }
    if (!jsonObject.getBoolean("email:confirmed")) {
      throw new IllegalStateException("Your email isn't confirmed yet!");
    }
  }

  private JSONObject queryUserInfo(final CloseableHttpClient client) throws IOException {
    final HttpGet post = new HttpGet(getForumUrl() + "/api/user/" + username);
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      return new JSONObject(EntityUtils.toString(response.getEntity()));
    }
  }

  private NameValuePair newPasswordParameter() {
    return new BasicNameValuePair("password", password);
  }

  private String getToken(final CloseableHttpClient client, final int userId) throws IOException {
    final HttpPost post = new HttpPost(getForumUrl() + "/api/v2/users/" + userId + "/tokens");
    post.setEntity(new UrlEncodedFormEntity(
        Collections.singletonList(newPasswordParameter()),
        StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      final String rawJson = EntityUtils.toString(response.getEntity());
      final JSONObject jsonObject = new JSONObject(rawJson);
      if (jsonObject.has("code")) {
        final String code = jsonObject.getString("code");
        if (code.equalsIgnoreCase("ok")) {
          return jsonObject.getJSONObject("payload").getString("token");
        }
        throw new IllegalStateException(
            "Failed to retrieve Token. Code: " + code + " Message: " + jsonObject.getString("message"));
      }
      throw new IllegalStateException("Failed to retrieve Token, server did not return correct response: "
          + response.getStatusLine() + "; JSON: " + rawJson);
    }
  }

  @Override
  public void viewPosted() {
    OpenFileUtility.openUrl(getForumUrl() + "/topic/" + topicId);
  }

  @Override
  public String getTestMessage() {
    return "Testing... This may take a while";
  }

  private static void addTokenHeader(final HttpRequestBase request, final String token) {
    request.addHeader("Authorization", "Bearer " + token);
  }
}
