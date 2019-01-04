package org.triplea.server.reporting.error;

import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.github.issues.GithubIssueClientFactory;
import org.triplea.http.client.github.issues.create.CreateIssueRequest;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;
import org.triplea.server.EnvironmentConfiguration;

import lombok.AllArgsConstructor;

/**
 * Class that handles object creation and dependencies for error upload.
 */
@AllArgsConstructor
public class ErrorUploadConfiguration {

  private final EnvironmentConfiguration environmentConfiguration;

  /**
   * Factory method for {@code ErrorUploader}.
   */
  public ErrorUploadStrategy newErrorUploader() {
    final ServiceClient<CreateIssueRequest, CreateIssueResponse> createIssueClient =
        new GithubIssueClientFactory().newGithubIssueCreator(
            environmentConfiguration.getGithubAuthToken(),
            environmentConfiguration.getGithubOrg(),
            environmentConfiguration.getGithubRepo(),
            environmentConfiguration.getGithubHost());

    return ErrorUploadStrategy.builder()
        .createIssueClient(createIssueClient)
        .requestAdapter(new ErrorReportRequestAdapter())
        .responseAdapter(new ErrorReportResponseAdapter())
        .allowErrorReport(new ErrorReportGateKeeper())
        .build();
  }
}
