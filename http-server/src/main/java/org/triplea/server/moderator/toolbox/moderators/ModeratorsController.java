package org.triplea.server.moderator.toolbox.moderators;

import io.dropwizard.auth.Auth;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.http.client.lobby.moderator.toolbox.management.ToolboxModeratorManagementClient;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.server.access.AuthenticatedUser;

/**
 * Provides endpoint for moderator maintenance actions and to support the moderators toolbox
 * 'moderators' tab. Actions include: adding moderators, removing moderators, and promoting
 * moderators to 'super-mod'. Some actions are only allowed for super-mods.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Builder
public class ModeratorsController {
  @Nonnull private final ModeratorsService moderatorsService;

  @POST
  @Path(ToolboxModeratorManagementClient.CHECK_USER_EXISTS_PATH)
  @RolesAllowed(UserRole.ADMIN)
  public Response checkUserExists(final String username) {
    return Response.ok().entity(moderatorsService.userExistsByName(username)).build();
  }

  @GET
  @Path(ToolboxModeratorManagementClient.FETCH_MODERATORS_PATH)
  @RolesAllowed(UserRole.MODERATOR)
  public Response getModerators() {
    return Response.ok().entity(moderatorsService.fetchModerators()).build();
  }

  @GET
  @Path(ToolboxModeratorManagementClient.IS_SUPER_MOD_PATH)
  @RolesAllowed(UserRole.MODERATOR)
  public Response isSuperMod(@Auth final AuthenticatedUser authenticatedUser) {
    return Response.ok().entity(authenticatedUser.getUserRole().equals(UserRole.ADMIN)).build();
  }

  @POST
  @Path(ToolboxModeratorManagementClient.REMOVE_MOD_PATH)
  @RolesAllowed(UserRole.ADMIN)
  public Response removeMod(
      @Auth final AuthenticatedUser authenticatedUser, final String moderatorName) {
    moderatorsService.removeMod(authenticatedUser.getUserIdOrThrow(), moderatorName);
    return Response.ok().build();
  }

  @POST
  @Path(ToolboxModeratorManagementClient.ADD_SUPER_MOD_PATH)
  @RolesAllowed(UserRole.ADMIN)
  public Response setSuperMod(
      @Auth final AuthenticatedUser authenticatedUser, final String moderatorName) {
    moderatorsService.addSuperMod(authenticatedUser.getUserIdOrThrow(), moderatorName);
    return Response.ok().build();
  }

  @POST
  @Path(ToolboxModeratorManagementClient.ADD_MODERATOR_PATH)
  @RolesAllowed(UserRole.ADMIN)
  public Response addModerator(
      @Auth final AuthenticatedUser authenticatedUser, final String username) {
    moderatorsService.addModerator(authenticatedUser.getUserIdOrThrow(), username);
    return Response.ok().build();
  }
}
