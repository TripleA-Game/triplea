package org.triplea.server.moderator.toolbox.moderators;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.moderator.toolbox.moderator.management.ToolboxModeratorManagementClient;
import org.triplea.server.http.ProtectedEndpointTest;

class ModeratorsControllerIntegrationTest
    extends ProtectedEndpointTest<ToolboxModeratorManagementClient> {
  ModeratorsControllerIntegrationTest() {
    super(ToolboxModeratorManagementClient::newClient);
  }

  @Test
  void isSuperMod() {
    verifyEndpointReturningObject(ToolboxModeratorManagementClient::isCurrentUserSuperMod);
  }

  @Test
  void removeMod() {
    verifyEndpointReturningVoid(client -> client.removeMod("mod"));
  }

  @Test
  void setSuperMod() {
    verifyEndpointReturningVoid(client -> client.addSuperMod("mod3"));
  }
}
