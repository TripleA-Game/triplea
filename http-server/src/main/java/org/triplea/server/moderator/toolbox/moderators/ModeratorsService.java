package org.triplea.server.moderator.toolbox.moderators;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.java.Log;
import org.triplea.http.client.moderator.toolbox.moderator.management.ModeratorInfo;
import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.ModeratorSingleUseKeyDao;
import org.triplea.lobby.server.db.dao.ModeratorsDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@Builder
@Log
class ModeratorsService {
  @Nonnull private final ModeratorsDao moderatorsDao;
  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final ModeratorApiKeyDao moderatorApiKeyDao;
  @Nonnull private final ModeratorSingleUseKeyDao moderatorSingleUseKeyDao;
  @Nonnull private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  /** Returns a list of all users that are moderators. */
  List<ModeratorInfo> fetchModerators() {
    return moderatorsDao.getModerators().stream()
        .map(
            userInfo ->
                ModeratorInfo.builder()
                    .name(userInfo.getUsername())
                    .lastLogin(userInfo.getLastLogin())
                    .build())
        .collect(Collectors.toList());
  }

  /** Promotes a user to moderator. Can only be done by super-moderators. */
  void addModerator(final int moderatorIdRequesting, final String username) {
    final int userId =
        userJdbiDao
            .lookupUserIdByName(username)
            .orElseThrow(
                () -> new IllegalArgumentException("Unable to find username: " + username));

    Preconditions.checkState(moderatorsDao.addMod(userId) == 1);
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorIdRequesting)
            .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_MODERATOR)
            .actionTarget(username)
            .build());
    log.info(username + " was promoted to moderator");
  }

  /** Removes moderator status from a user. Can only be done by super moderators. */
  void removeMod(final int moderatorIdRequesting, final String moderatorNameToRemove) {
    final int userId =
        userJdbiDao
            .lookupUserIdByName(moderatorNameToRemove)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Failed to find moderator by user name: " + moderatorNameToRemove));

    Preconditions.checkState(
        moderatorsDao.removeMod(userId) == 1,
        "Failed to remove moderator status for: " + moderatorNameToRemove);
    moderatorApiKeyDao.deleteKeysByUserId(userId);
    moderatorSingleUseKeyDao.deleteKeysByUserId(userId);

    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorIdRequesting)
            .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_MODERATOR)
            .actionTarget(moderatorNameToRemove)
            .build());
    log.info(moderatorNameToRemove + " was removed from moderators");
  }

  /** Promotes a user to super-moderator. Can only be done by super moderators. */
  void addSuperMod(final int moderatorIdRequesting, final String username) {
    final int userId =
        userJdbiDao
            .lookupUserIdByName(username)
            .orElseThrow(
                () -> new IllegalArgumentException("Failed to find user by name: " + username));

    Preconditions.checkState(
        moderatorsDao.addSuperMod(userId) == 1,
        "Failed to add super moderator status for: " + username);
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorIdRequesting)
            .actionName(ModeratorAuditHistoryDao.AuditAction.ADD_SUPER_MOD)
            .actionTarget(username)
            .build());
    log.info(username + " was promoted to super mod");
  }

  /** Checks if any user exists in DB by the given name. */
  boolean userExistsByName(final String username) {
    return userJdbiDao.lookupUserIdByName(username).isPresent();
  }
}
