package org.triplea.lobby.server.login;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.lobby.server.db.AccessLogController;
import org.triplea.lobby.server.db.BadWordController;
import org.triplea.lobby.server.db.BadWordDao;
import org.triplea.lobby.server.db.BannedMacController;
import org.triplea.lobby.server.db.BannedMacDao;
import org.triplea.lobby.server.db.BannedUsernameController;
import org.triplea.lobby.server.db.BannedUsernameDao;
import org.triplea.lobby.server.db.Database;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.UserController;
import org.triplea.lobby.server.db.UserDao;
import org.triplea.util.Tuple;
import org.triplea.util.Version;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.net.ILoginValidator;
import games.strategy.net.MacFinder;

/**
 * The server side of the lobby authentication protocol.
 *
 * <p>
 * In the lobby authentication protocol, the server sends a challenge to the client based on the username of the client.
 * Upon receiving the client's response, the server determines if the client has provided a valid password and gives
 * them access to the lobby if authentication is successful.
 * </p>
 */
public final class LobbyLoginValidator implements ILoginValidator {
  @VisibleForTesting
  interface ErrorMessages {
    String ANONYMOUS_AUTHENTICATION_FAILED = "Can't login anonymously, username already exists";
    String AUTHENTICATION_FAILED = "Incorrect username or password";
    String INVALID_MAC = "Invalid mac address";
    String THATS_NOT_A_NICE_NAME = "That's not a nice name";
    String USERNAME_HAS_BEEN_BANNED = "This username is banned, please create a new one.";
    String YOU_HAVE_BEEN_BANNED = "You have been banned from the TripleA lobby.";
  }

  private final AccessLog accessLog;
  private final BadWordDao badWordDao;
  private final BannedMacDao bannedMacDao;
  private final BannedUsernameDao bannedUsernameDao;
  private final BcryptSaltGenerator bcryptSaltGenerator;
  private final RsaAuthenticator rsaAuthenticator;
  private final UserDao userDao;

  public LobbyLoginValidator(final LobbyConfiguration lobbyConfiguration) {
    this(new Database(lobbyConfiguration));
  }

  private LobbyLoginValidator(final Database database) {
    this(
        new BadWordController(database),
        new BannedMacController(database),
        new BannedUsernameController(database),
        new UserController(database),
        new CompositeAccessLog(new AccessLogController(database)),
        new RsaAuthenticator(),
        BCrypt::gensalt);
  }

  @VisibleForTesting
  LobbyLoginValidator(
      final BadWordDao badWordDao,
      final BannedMacDao bannedMacDao,
      final BannedUsernameDao bannedUsernameDao,
      final UserDao userDao,
      final AccessLog accessLog,
      final RsaAuthenticator rsaAuthenticator,
      final BcryptSaltGenerator bcryptSaltGenerator) {
    this.accessLog = accessLog;
    this.badWordDao = badWordDao;
    this.bannedMacDao = bannedMacDao;
    this.bannedUsernameDao = bannedUsernameDao;
    this.bcryptSaltGenerator = bcryptSaltGenerator;
    this.rsaAuthenticator = rsaAuthenticator;
    this.userDao = userDao;
  }

  @Override
  public Map<String, String> getChallengeProperties(final String userName) {
    final Map<String, String> challenge = new HashMap<>();
    challenge.putAll(rsaAuthenticator.newChallenge());
    return challenge;
  }

  @Nullable
  @Override
  public String verifyConnection(
      final Map<String, String> challenge,
      final Map<String, String> response,
      final String clientName,
      final String clientMac,
      final SocketAddress remoteAddress) {
    final User user = User.builder()
        .username(clientName)
        .inetAddress(((InetSocketAddress) remoteAddress).getAddress())
        .hashedMacAddress(clientMac)
        .build();
    final @Nullable String errorMessage = authenticateUser(response, user);
    logAuthenticationResult(user, getUserTypeFor(response), errorMessage);
    return errorMessage;
  }

  private @Nullable String authenticateUser(final Map<String, String> response, final User user) {
    if (response == null) {
      return "No Client Properties";
    }
    final String clientVersionString = response.get(LobbyLoginResponseKeys.LOBBY_VERSION);
    if (clientVersionString == null) {
      return "No Client Version";
    }
    final Version clientVersion = new Version(clientVersionString);
    if (!clientVersion.equals(LobbyConstants.LOBBY_VERSION)) {
      return "Wrong version, we require " + LobbyConstants.LOBBY_VERSION.toString() + " but trying to log in with "
          + clientVersionString;
    }
    for (final String s : getBadWords()) {
      if (user.getUsername().toLowerCase().contains(s.toLowerCase())) {
        return ErrorMessages.THATS_NOT_A_NICE_NAME;
      }
    }
    if (!MacFinder.isValidHashedMacAddress(user.getHashedMacAddress())) {
      // Must have been tampered with
      return ErrorMessages.INVALID_MAC;
    }
    final Tuple<Boolean, Timestamp> macBanned = bannedMacDao.isMacBanned(user.getHashedMacAddress());
    if (macBanned.getFirst()) {
      return ErrorMessages.YOU_HAVE_BEEN_BANNED + " " + getBanDurationBreakdown(macBanned.getSecond());
    }
    // test for username ban after testing normal bans, because if it is only a username ban then the user should know
    // they can change their name
    final Tuple<Boolean, Timestamp> usernameBanned = bannedUsernameDao.isUsernameBanned(user.getUsername());
    if (usernameBanned.getFirst()) {
      return ErrorMessages.USERNAME_HAS_BEEN_BANNED + " " + getBanDurationBreakdown(usernameBanned.getSecond());
    }
    if (response.containsKey(LobbyLoginResponseKeys.REGISTER_NEW_USER)) {
      return createUser(response, user);
    }

    final UserType userType = getUserTypeFor(response);
    switch (userType) {
      case ANONYMOUS:
        return authenticateAnonymousUser(response, user);
      case REGISTERED:
        return authenticateRegisteredUser(response, user);
      default:
        throw new AssertionError("unknown user type: " + userType);
    }
  }

  private static UserType getUserTypeFor(final Map<String, String> response) {
    return response.containsKey(LobbyLoginResponseKeys.ANONYMOUS_LOGIN) ? UserType.ANONYMOUS : UserType.REGISTERED;
  }

  private void logAuthenticationResult(final User user, final UserType userType, final @Nullable String errorMessage) {
    if (errorMessage == null) {
      accessLog.logSuccessfulAuthentication(user, userType);
    } else {
      accessLog.logFailedAuthentication(user, userType, errorMessage);
    }
  }

  private static String getBanDurationBreakdown(final Timestamp stamp) {
    if (stamp == null) {
      return "Banned Forever";
    }
    final long millis = stamp.getTime() - System.currentTimeMillis();
    if (millis < 0) {
      return "Ban time left: 1 Minute";
    }
    long seconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(millis));
    final int minutesInSeconds = 60;
    final int hoursInSeconds = 60 * 60;
    final int daysInSeconds = 60 * 60 * 24;
    final long days = seconds / daysInSeconds;
    seconds -= days * daysInSeconds;
    final long hours = seconds / hoursInSeconds;
    seconds -= hours * hoursInSeconds;
    final long minutes = Math.max(1, seconds / minutesInSeconds);

    final StringBuilder sb = new StringBuilder(64);
    sb.append("Ban time left: ");
    if (days > 0) {
      sb.append(days).append(" Days ");
    }
    if (hours > 0) {
      sb.append(hours).append(" Hours ");
    }
    sb.append(minutes).append(" Minutes");
    return sb.toString();
  }

  private List<String> getBadWords() {
    return badWordDao.list();
  }

  private @Nullable String authenticateRegisteredUser(final Map<String, String> response, final User user) {
    final String username = user.getUsername();

    final HashedPassword hashedPassword = userDao.getPassword(username);
    if (hashedPassword != null && RsaAuthenticator.canProcessResponse(response)) {
      return rsaAuthenticator.decryptPasswordForAction(response,
          pass -> userDao.login(username, new HashedPassword(pass))
              ? null
              : ErrorMessages.AUTHENTICATION_FAILED);
    }
    return ErrorMessages.AUTHENTICATION_FAILED;
  }

  private @Nullable String authenticateAnonymousUser(final Map<String, String> response, final User user) {
    final String username = user.getUsername();
    if (userDao.doesUserExist(username)) {
      return ErrorMessages.ANONYMOUS_AUTHENTICATION_FAILED;
    }
    // If this is a lobby watcher, use a different set of validation
    if (Boolean.TRUE.toString().equals(response.get(LobbyLoginResponseKeys.LOBBY_WATCHER_LOGIN))) {
      if (!username.endsWith(LobbyConstants.LOBBY_WATCHER_NAME)) {
        return "Lobby watcher usernames must end with 'lobby_watcher'";
      }
      final String hostName = username.substring(0, username.indexOf(LobbyConstants.LOBBY_WATCHER_NAME));

      if (!DBUser.isValidUserName(hostName)) {
        return DBUser.getUserNameValidationErrorMessage(hostName);
      }
    } else {
      return DBUser.isValidUserName(username) ? null : DBUser.getUserNameValidationErrorMessage(username);
    }
    return null;
  }

  private @Nullable String createUser(final Map<String, String> response, final User user) {
    final DBUser dbUser = new DBUser(
        new DBUser.UserName(user.getUsername()),
        new DBUser.UserEmail(response.get(LobbyLoginResponseKeys.EMAIL)));

    if (!dbUser.isValid()) {
      return dbUser.getValidationErrorMessage();
    }

    if (userDao.doesUserExist(dbUser.getName())) {
      return "That user name has already been taken";
    }

    final HashedPassword password = new HashedPassword(response.get(LobbyLoginResponseKeys.HASHED_PASSWORD));
    if (RsaAuthenticator.canProcessResponse(response)) {
      return rsaAuthenticator.decryptPasswordForAction(response, pass -> {
        final HashedPassword newPass = new HashedPassword(BCrypt.hashpw(pass, bcryptSaltGenerator.newSalt()));
        userDao.createUser(dbUser, newPass);
        return null;
      });
    }
    if (!password.isHashedWithSalt()) {
      return "Password is not hashed correctly";
    }

    try {
      userDao.createUser(dbUser, password);
      return null;
    } catch (final Exception e) {
      return e.getMessage();
    }
  }

  @FunctionalInterface
  @VisibleForTesting
  interface BcryptSaltGenerator {
    String newSalt();
  }
}
