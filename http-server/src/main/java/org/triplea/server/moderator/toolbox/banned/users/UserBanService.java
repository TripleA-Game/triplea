package org.triplea.server.moderator.toolbox.banned.users;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanData;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UserBanDao;

/**
 * Service layer for managing user bans, get bans, add and remove. User bans are done by MAC and IP
 * address, they are removed by the 'public ban id' that is assigned when a ban is issued.
 */
@Builder
public class UserBanService {

  @Nonnull private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  @Nonnull private final UserBanDao bannedUserDao;
  @Nonnull private final Supplier<String> publicIdSupplier;

  List<UserBanData> getBannedUsers() {
    return bannedUserDao.lookupBans().stream()
        .map(
            daoData ->
                UserBanData.builder()
                    .banId(daoData.getPublicBanId())
                    .username(daoData.getUsername())
                    .hashedMac(daoData.getHashedMac())
                    .ip(daoData.getIp())
                    .banDate(daoData.getDateCreated())
                    .banExpiry(daoData.getBanExpiry())
                    .build())
        .collect(Collectors.toList());
  }

  boolean removeUserBan(final int moderatorId, final String banId) {
    final String unbanName = bannedUserDao.lookupUserNameByBanId(banId);
    if (bannedUserDao.removeBan(banId) != 1) {
      return false;
    }
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_USER_BAN)
            .actionTarget(unbanName)
            .moderatorUserId(moderatorId)
            .build());
    return true;
  }

  boolean banUser(final int moderatorId, final UserBanParams banUserParams) {
    if (bannedUserDao.addBan(
            publicIdSupplier.get(),
            banUserParams.getUsername(),
            banUserParams.getHashedMac(),
            banUserParams.getIp(),
            banUserParams.getHoursToBan())
        != 1) {
      return false;
    }

    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USER)
            .actionTarget(banUserParams.getUsername())
            .build());
    return true;
  }
}
