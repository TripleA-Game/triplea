package org.triplea.lobby.server.db.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.lobby.server.db.data.ModeratorAuditHistoryDaoData;
import org.triplea.test.common.Integration;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;

@Integration
@ExtendWith(DBUnitExtension.class)
class ModeratorAuditHistoryDaoTest {

  private static final int MODERATOR_ID = 900000;
  private static final int MODERATOR_ID_DOES_NOT_EXIST = 1111;

  private final ModeratorAuditHistoryDao dao =
      JdbiDatabase.newConnection().onDemand(ModeratorAuditHistoryDao.class);

  @Test
  @DataSet("moderator_audit/pre-insert.yml")
  void addAuditRecordThrowsIfModeratorNameNotFound() {
    assertThrows(
        UnableToExecuteStatementException.class,
        () -> dao.addAuditRecord(ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(MODERATOR_ID_DOES_NOT_EXIST)
            .actionTarget("any-value")
            .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USERNAME)
            .build()));
  }

  @Test
  @DataSet("moderator_audit/pre-insert.yml")
  @ExpectedDataSet("moderator_audit/post-insert.yml")
  void addAuditRecord() {
    dao.addAuditRecord(ModeratorAuditHistoryDao.AuditArgs.builder()
        .moderatorUserId(MODERATOR_ID)
        .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USERNAME)
        .actionTarget("ACTION_TARGET")
        .build());
  }

  @Test
  @DataSet("moderator_audit/history-select.yml")
  void selectHistory() {
    List<ModeratorAuditHistoryDaoData> results = dao.lookupHistoryItems(0, 3);

    assertThat(results, hasSize(3));

    assertThat(results.get(0).getUsername(), is("moderator2"));
    assertThat(results.get(0).getDateCreated(), is(Instant.parse("2016-01-05T23:59:20Z")));
    assertThat(results.get(0).getActionName(), is("BAN_USERNAME"));
    assertThat(results.get(0).getActionTarget(), is("ACTION_TARGET5"));

    assertThat(results.get(1).getUsername(), is("moderator1"));
    assertThat(results.get(1).getDateCreated(), is(Instant.parse("2016-01-04T23:59:20Z")));
    assertThat(results.get(1).getActionName(), is("BOOT_PLAYER"));
    assertThat(results.get(1).getActionTarget(), is("ACTION_TARGET4"));

    assertThat(results.get(2).getUsername(), is("moderator2"));
    assertThat(results.get(2).getDateCreated(), is(Instant.parse("2016-01-03T23:59:20Z")));
    assertThat(results.get(2).getActionName(), is("BAN_USERNAME"));
    assertThat(results.get(2).getActionTarget(), is("ACTION_TARGET3"));


    // there are only 5 records total
    results = dao.lookupHistoryItems(3, 3);
    assertThat(results, hasSize(2));
    assertThat(results.get(0).getUsername(), is("moderator2"));
    assertThat(results.get(0).getDateCreated(), is(Instant.parse("2016-01-02T23:59:20Z")));
    assertThat(results.get(0).getActionName(), is("MUTE_USERNAME"));
    assertThat(results.get(0).getActionTarget(), is("ACTION_TARGET2"));

    assertThat(results.get(1).getUsername(), is("moderator1"));
    assertThat(results.get(1).getDateCreated(), is(Instant.parse("2016-01-01T23:59:20Z")));
    assertThat(results.get(1).getActionName(), is("BAN_USERNAME"));
    assertThat(results.get(1).getActionTarget(), is("ACTION_TARGET1"));

    results = dao.lookupHistoryItems(5, 3);
    assertThat(results, hasSize(0));
  }
}
