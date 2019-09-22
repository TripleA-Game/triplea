package org.triplea.server.access;

import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.triplea.lobby.server.db.dao.ApiKeyDao;

/**
 * Verifies a 'bearer' token API key is valid. This means checking if the key is in database, if so
 * we return an {@code AuthenticatedUser} otherwise optional. Anonymous users will have a null
 * user-id and role of 'ANONYMOUS'.
 */
@AllArgsConstructor
public class ApiKeyAuthenticator implements Authenticator<String, AuthenticatedUser> {

  private final ApiKeyDao apiKeyDao;

  @Override
  public Optional<AuthenticatedUser> authenticate(final String apiKey) {
    return apiKeyDao
        .lookupByApiKey(apiKey)
        .map(
            userData ->
                AuthenticatedUser.builder()
                    .userId(userData.getUserId())
                    .userRole(userData.getRole())
                    .apiKey(ApiKey.of(apiKey))
                    .build());
  }
}
