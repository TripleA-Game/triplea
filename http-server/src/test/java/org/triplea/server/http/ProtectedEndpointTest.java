package org.triplea.server.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.triplea.http.client.HttpInteractionException;

/**
 * Test to verify endpoints that are protected behind authorization/authentication. For each verify
 * method, we'll first verify the endpoint using a valid API key, then we'll verify that the
 * endpoint is not accessible when using an invalid API key.
 */
public abstract class ProtectedEndpointTest<T> extends DropwizardTest {
  private static final String VALID_API_TOKEN = "test";
  private static final String INVALID_API_TOKEN = "not-correct";

  private final BiFunction<URI, String, T> clientBuilder;

  protected ProtectedEndpointTest(final BiFunction<URI, String, T> clientBuilder) {
    this.clientBuilder = clientBuilder;
  }

  /** Use this to verify an endpoint that returns void. */
  protected void verifyEndpointReturningVoid(final Consumer<T> methodRunner) {
    methodRunner.accept(clientBuilder.apply(localhost, VALID_API_TOKEN));

    assertThrows(
        HttpInteractionException.class,
        () -> methodRunner.accept(clientBuilder.apply(localhost, INVALID_API_TOKEN)));
  }

  /** Use this to verify an endpoint that returns data. */
  protected void verifyEndpointReturningObject(final Function<T, ?> methodRunner) {
    assertThat(methodRunner.apply(clientBuilder.apply(localhost, VALID_API_TOKEN)), notNullValue());
    assertThrows(
        HttpInteractionException.class,
        () -> methodRunner.apply(clientBuilder.apply(localhost, INVALID_API_TOKEN)));
  }

  /**
   * Use this method to verify an endpoint that returns a collection of data. For better test
   * coverage, it is assumed that the DB will be set up so that some data will be returned, we
   * expect a non-empty collection to be returned.
   */
  protected void verifyEndpointReturningCollection(final Function<T, Collection<?>> methodRunner) {
    assertThat(methodRunner.apply(clientBuilder.apply(localhost, VALID_API_TOKEN)), not(empty()));

    assertThrows(
        HttpInteractionException.class,
        () -> methodRunner.apply(clientBuilder.apply(localhost, INVALID_API_TOKEN)));
  }
}
