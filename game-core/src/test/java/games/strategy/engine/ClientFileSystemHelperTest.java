package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import games.strategy.triplea.settings.GameSetting;

public final class ClientFileSystemHelperTest {
  @ExtendWith(MockitoExtension.class)
  @MockitoSettings(strictness = Strictness.WARN)
  @Nested
  public final class GetFolderContainingFileWithNameTest {
    @Mock
    private File file;

    @Mock
    private File parentFolder;

    @Mock
    private File startFolder;

    private File getFolderContainingFileWithName() throws Exception {
      return ClientFileSystemHelper.getFolderContainingFileWithName(file.getName(), startFolder);
    }

    @BeforeEach
    public void setUp() {
      when(file.isFile()).thenReturn(true);
      when(startFolder.getParentFile()).thenReturn(parentFolder);
      when(file.getName()).thenReturn("filename.ext");
      when(parentFolder.getName()).thenReturn("parent");
      when(startFolder.getName()).thenReturn("start");
    }

    @Test
    public void shouldReturnStartFolderWhenStartFolderContainsFile() throws Exception {
      when(startFolder.listFiles()).thenReturn(new File[] {file});

      assertThat(getFolderContainingFileWithName(), is(startFolder));
    }

    @Test
    public void shouldReturnAncestorFolderWhenAncestorFolderContainsFile() throws Exception {
      when(startFolder.listFiles()).thenReturn(new File[0]);
      when(parentFolder.listFiles()).thenReturn(new File[] {file});

      assertThat(getFolderContainingFileWithName(), is(parentFolder));
    }

    @Test
    public void shouldThrowExceptionWhenNoFolderContainsFile() {
      when(startFolder.listFiles()).thenReturn(new File[0]);
      when(parentFolder.listFiles()).thenReturn(new File[0]);

      assertThrows(IOException.class, () -> getFolderContainingFileWithName());
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class GetUserMapsFolderPathTest {
    @Mock
    private GameSetting currentSetting;

    @Mock
    private GameSetting overrideSetting;

    private String getUserMapsFolderPath() {
      return ClientFileSystemHelper.getUserMapsFolderPath(currentSetting, overrideSetting);
    }

    @Test
    public void shouldReturnCurrentPathWhenOverridePathNotSet() {
      when(overrideSetting.isSet()).thenReturn(false);
      final String currentPath = "/path/to/current";
      when(currentSetting.value()).thenReturn(currentPath);

      assertThat(getUserMapsFolderPath(), is(currentPath));
    }

    @Test
    public void shouldReturnOverridePathWhenOverridePathSet() {
      when(overrideSetting.isSet()).thenReturn(true);
      final String overridePath = "/path/to/override";
      when(overrideSetting.value()).thenReturn(overridePath);

      assertThat(getUserMapsFolderPath(), is(overridePath));
    }
  }
}
