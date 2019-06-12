package org.triplea.http.client.moderator.toolbox;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.HttpInteractionException;

@ExtendWith(MockitoExtension.class)
class ModeratorToolboxClientTest {

  private static final String TEST_VALUE = "test-value";
  private static final String RETURN_VALUE = "return-value";
  private static final String EXCEPTION_MESSAGE = "exception-message";
  private static final HttpInteractionException EXCEPTION = new HttpInteractionException(500, EXCEPTION_MESSAGE);
  private static final List<String> BAD_WORDS = Arrays.asList("word1", "word2");

  private static final int ROW_COUNT = 5;
  private static final int ROW_START = 10;

  private static final String API_KEY = "api-key";
  private static final String PASSWORD = "password";
  private static final Map<String, Object> expectedHeader;

  static {
    expectedHeader = new HashMap<>();
    expectedHeader.put(ModeratorToolboxClient.API_KEY_HEADER, API_KEY);
    expectedHeader.put(ModeratorToolboxClient.API_KEY_PASSWORD_HEADER, PASSWORD);
  }

  private static final RegisterApiKeyResult REGISTER_API_KEY_RESULT = RegisterApiKeyResult.builder()
      .build();


  @Mock
  private ModeratorToolboxFeignClient moderatorToolboxFeignClient;

  private ModeratorToolboxClient moderatorToolboxClient;

  @Mock
  private ModeratorEvent moderatorEvent;

  @BeforeEach
  void setup() {
    moderatorToolboxClient = new ModeratorToolboxClient(moderatorToolboxFeignClient, PASSWORD);
  }


  @Test
  void registerApiKey() {
    when(moderatorToolboxFeignClient.registerKey(
        RegisterApiKeyParam.builder()
            .singleUseKey(API_KEY)
            .newPassword(PASSWORD)
            .build()))
                .thenReturn(REGISTER_API_KEY_RESULT);

    assertThat(
        moderatorToolboxClient.registerNewKey(API_KEY),
        sameInstance(REGISTER_API_KEY_RESULT));
  }

  @Test
  void validateApiKey() {
    when(moderatorToolboxFeignClient.validateApiKey(expectedHeader))
        .thenReturn(RETURN_VALUE);

    assertThat(
        moderatorToolboxClient.validateApiKey(API_KEY),
        is(RETURN_VALUE));
  }

  @Test
  void validateApiKeyWithException() {
    when(moderatorToolboxFeignClient.validateApiKey(expectedHeader)).thenThrow(EXCEPTION);

    assertThat(
        moderatorToolboxClient.validateApiKey(API_KEY),
        containsString(EXCEPTION_MESSAGE));
  }

  @Test
  void addBadWord() {
    when(moderatorToolboxFeignClient.addBadWord(expectedHeader, TEST_VALUE)).thenReturn(RETURN_VALUE);

    assertThat(
        moderatorToolboxClient.addBadWord(UpdateBadWordsArg.builder()
            .apiKey(API_KEY)
            .badWord(TEST_VALUE)
            .build()),
        is(RETURN_VALUE));
  }


  @Test
  void addBadWordWithException() {
    when(moderatorToolboxFeignClient.addBadWord(expectedHeader, TEST_VALUE)).thenThrow(EXCEPTION);

    assertThat(
        moderatorToolboxClient.addBadWord(UpdateBadWordsArg.builder()
            .apiKey(API_KEY)
            .badWord(TEST_VALUE)
            .build()),
        containsString(EXCEPTION_MESSAGE));
  }


  @Test
  void removeBadWord() {
    when(moderatorToolboxFeignClient.removeBadWord(expectedHeader, TEST_VALUE)).thenReturn(RETURN_VALUE);

    assertThat(
        moderatorToolboxClient.removeBadWord(UpdateBadWordsArg.builder()
            .apiKey(API_KEY)
            .badWord(TEST_VALUE)
            .build()),
        is(RETURN_VALUE));
  }


  @Test
  void removeBadWordWithException() {
    when(moderatorToolboxFeignClient.removeBadWord(expectedHeader, TEST_VALUE)).thenThrow(EXCEPTION);

    assertThat(
        moderatorToolboxClient.removeBadWord(UpdateBadWordsArg.builder()
            .apiKey(API_KEY)
            .badWord(TEST_VALUE)
            .build()),
        containsString(EXCEPTION_MESSAGE));
  }

  @Test
  void getBadWords() {
    when(moderatorToolboxFeignClient.getBadWords(expectedHeader)).thenReturn(BAD_WORDS);

    assertThat(
        moderatorToolboxClient.getBadWords(API_KEY),
        is(BAD_WORDS));
  }

  @Test
  void getBadWordsWithException() {
    when(moderatorToolboxFeignClient.getBadWords(expectedHeader)).thenThrow(EXCEPTION);

    assertThrows(EXCEPTION.getClass(), () -> moderatorToolboxClient.getBadWords(API_KEY));
  }



  @Test
  void lookupModeratorEvents() {
    when(moderatorToolboxFeignClient.lookupModeratorEvents(expectedHeader, ROW_START, ROW_COUNT))
        .thenReturn(singletonList(moderatorEvent));

    final List<ModeratorEvent> results = moderatorToolboxClient.lookupModeratorEvents(
        LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowCount(ROW_COUNT)
            .rowStart(ROW_START)
            .build());

    assertThat(results, hasSize(1));
    assertThat(results.get(0), sameInstance(moderatorEvent));
  }
}
