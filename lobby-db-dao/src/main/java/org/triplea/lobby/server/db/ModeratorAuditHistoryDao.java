package org.triplea.lobby.server.db;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Interface for adding new moderator audit records to database. These records
 * keep track of which actions moderators have taken, who the target was and
 * which moderator took the action.
 */
public interface ModeratorAuditHistoryDao {
  /**
   * Parameters needed when adding an audit record.
   */
  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  final class AuditArgs {
    private final String moderatorName;
    private final Integer moderatorUserId;
    @Nonnull
    private final AuditAction actionName;
    @Nonnull
    private final String actionTarget;
  }

  /**
   * The set of moderator actions.
   */
  enum AuditAction {
    BAN_MAC, BAN_USERNAME, BOOT_USER_FROM_BOT, BOOT_USER_FROM_LOBBY, BAN_PLAYER_FROM_BOT, ADD_BAD_WORD, REMOVE_BAD_WORD,
  }

  default void addAuditRecord(AuditArgs auditArgs) {
    final int moderatorId =
        Optional.ofNullable(auditArgs.moderatorUserId)
            .orElseGet(() -> lookupModeratorId(auditArgs.moderatorName));
    insertAuditRecord(moderatorId, auditArgs.actionName.toString(), auditArgs.actionTarget);
  }


  @SqlQuery("select id from lobby_user where username = :username")
  int lookupModeratorId(@Bind("username") String username);

  @SqlUpdate("insert into moderator_action_history "
      + "  (lobby_user_id, action_name, action_target) "
      + "values (:moderatorId, :actionName, :actionTarget)")
  void insertAuditRecord(
      @Bind("moderatorId") int moderatorId,
      @Bind("actionName") String actionName,
      @Bind("actionTarget") String actionTarget);


  /**
   * Column names as returned by query: lookupHistoryItems.
   *
   * @see #lookupHistoryItems(int, int)
   */
  interface LookupHistoryItemsColumns {
    String USER_NAME = "username";
    String DATE_CREATED = "date_created";
    String ACTION_NAME = "action_name";
    String ACTION_TARGET = "action_target";
  }

  @SqlQuery("select\n"
      + "  h.date_created " + LookupHistoryItemsColumns.DATE_CREATED + ",\n"
      + "  u.username " + LookupHistoryItemsColumns.USER_NAME + ",\n"
      + "  h.action_name " + LookupHistoryItemsColumns.ACTION_NAME + ",\n"
      + "  h.action_target " + LookupHistoryItemsColumns.ACTION_TARGET + "\n"
      + "from moderator_action_history h \n"
      + "join lobby_user u on u.id = h.lobby_user_id\n"
      + "order by h.date_created desc\n"
      + "offset :rowOffset rows\n"
      + "fetch next :rowCount rows only")
  List<ModeratorAuditHistoryItem> lookupHistoryItems(
      @Bind("rowOffset") int rowOffset, @Bind("rowCount") int rowCount);


}
