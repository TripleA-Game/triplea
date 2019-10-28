package org.triplea.server.user.account.login;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.PlayerName;
import org.triplea.domain.data.SystemId;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.LoginRequest;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.lobby.server.db.data.UserRole;

@Builder
class LoginModule {
  @Nonnull private Predicate<LoginRequest> registeredLogin;
  @Nonnull private Predicate<LoginRequest> tempPasswordLogin;
  @Nonnull private Function<PlayerName, Optional<String>> anonymousLogin;
  @Nonnull private final Function<LoginRecord, ApiKey> apiKeyGenerator;
  @Nonnull private final UserJdbiDao userJdbiDao;

  public LobbyLoginResponse doLogin(
      final LoginRequest loginRequest, final String systemId, final String ip) {
    if (loginRequest == null
        || Strings.nullToEmpty(loginRequest.getName()).isEmpty()
        || Strings.nullToEmpty(systemId).isEmpty()) {
      return LobbyLoginResponse.builder().failReason("Invalid login request").build();
    }

    final SystemId playerSystemId = SystemId.of(systemId);
    final String nameValidation = PlayerName.validate(loginRequest.getName());
    if (nameValidation != null) {
      return LobbyLoginResponse.builder().failReason("Invalid name: " + nameValidation).build();
    }

    final boolean hasPassword = !Strings.nullToEmpty(loginRequest.getPassword()).isEmpty();

    if (hasPassword && registeredLogin.test(loginRequest)) {
      final ApiKey apiKey =
          recordRegisteredLoginAndGenerateApiKey(loginRequest, playerSystemId, ip);
      return LobbyLoginResponse.builder()
          .apiKey(apiKey.getValue())
          .moderator(isModerator(loginRequest.getName()))
          .build();
    } else if (hasPassword && tempPasswordLogin.test(loginRequest)) {
      final ApiKey apiKey =
          recordRegisteredLoginAndGenerateApiKey(loginRequest, playerSystemId, ip);
      return LobbyLoginResponse.builder()
          .apiKey(apiKey.getValue())
          .passwordChangeRequired(true)
          .moderator(isModerator(loginRequest.getName()))
          .build();
    } else if (hasPassword) {
      return LobbyLoginResponse.builder()
          .failReason("Invalid username and password combination")
          .build();
    } else { // anonymous login
      final Optional<String> errorMessage =
          anonymousLogin.apply(PlayerName.of(loginRequest.getName()));
      if (errorMessage.isPresent()) {
        return LobbyLoginResponse.builder().failReason(errorMessage.get()).build();
      } else {
        final ApiKey apiKey =
            recordAnonymousLoginAndGenerateApiKey(loginRequest, playerSystemId, ip);
        return LobbyLoginResponse.builder().apiKey(apiKey.getValue()).build();
      }
    }
  }

  private ApiKey recordRegisteredLoginAndGenerateApiKey(
      final LoginRequest loginRequest, final SystemId systemId, final String ip) {
    return recordLoginAndGenerateApiKey(loginRequest, systemId, ip, true);
  }

  private ApiKey recordAnonymousLoginAndGenerateApiKey(
      final LoginRequest loginRequest, final SystemId systemId, final String ip) {
    return recordLoginAndGenerateApiKey(loginRequest, systemId, ip, false);
  }

  // TODO: Project#12 also update access log
  @SuppressWarnings("unused")
  private ApiKey recordLoginAndGenerateApiKey(
      final LoginRequest loginRequest,
      final SystemId systemId,
      final String ip,
      final boolean isRegistered) {
    return apiKeyGenerator.apply(
        LoginRecord.builder()
            .playerName(PlayerName.of(loginRequest.getName()))
            .systemId(systemId)
            .playerChatId(PlayerChatId.newId())
            .ip(ip)
            .build());
  }

  private boolean isModerator(final String username) {
    return userJdbiDao
        .lookupUserRoleByUserName(username)
        .map(UserRole::isModerator)
        .orElseThrow(() -> new AssertionError("Expected to find role for user: " + username));
  }
}
