package org.triplea.server.user.account.create;

import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.lobby.login.CreateAccountRequest;
import org.triplea.http.client.lobby.login.CreateAccountResponse;
import org.triplea.java.Postconditions;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

/**
 * Responsible to execute a 'create account' request. We should already have validated the request
 * and just need to store the new account in database.
 */
@Builder
class AccountCreator implements Function<CreateAccountRequest, CreateAccountResponse> {
  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final Function<String, String> passwordEncryptor;

  @Override
  public CreateAccountResponse apply(final CreateAccountRequest createAccountRequest) {
    final String cryptedPassword = passwordEncryptor.apply(createAccountRequest.getPassword());

    final int rowCount =
        userJdbiDao.createUser(
            createAccountRequest.getUsername(), createAccountRequest.getEmail(), cryptedPassword);
    Postconditions.assertState(rowCount == 1);
    return CreateAccountResponse.SUCCESS_RESPONSE;
  }
}
