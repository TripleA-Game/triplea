package games.strategy.debug.error.reporting;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.GraphicsEnvironment;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import swinglib.ConfirmationDialogBuilder;

/**
 * Ensure components are wired to the expected action handlers.
 */
@ExtendWith(MockitoExtension.class)
class ErrorReportComponentsTest {

  private ErrorReportComponents errorReportComponents = new ErrorReportComponents();


  @BeforeAll
  static void suppressUiComponents() {
    ConfirmationDialogBuilder.suppressDialog();
  }


  @BeforeAll
  static void skipIfHeadless() {
    assumeFalse(GraphicsEnvironment.isHeadless());
  }

  @Mock
  private Consumer<UserErrorReport> errorReportHandler;

  @Mock
  private Supplier<UserErrorReport> guiReader;

  @Mock
  private UserErrorReport userErrorReport;

  @Mock
  private Runnable runnable;

  @Test
  void submitButtonSendsAnErrorReport() {
    when(guiReader.get()).thenReturn(userErrorReport);

    errorReportComponents.createSubmitButton(withMockedSubmitButtonConfig())
        .doClick();

    verify(errorReportHandler, times(1))
        .accept(userErrorReport);
  }

  private ErrorReportComponents.FormHandler withMockedSubmitButtonConfig() {
    return ErrorReportComponents.FormHandler.builder()
        .guiReader(guiReader)
        .guiDataHandler(errorReportHandler)
        .build();
  }

  @Test
  void closeButton() {
    errorReportComponents.createCancelButton(runnable)
        .doClick();

    verify(runnable, times(1))
        .run();
  }

  @Test
  void previewButton() {
    when(guiReader.get()).thenReturn(userErrorReport);

    errorReportComponents.createPreviewButton(
        ErrorReportComponents.FormHandler.builder()
            .guiReader(guiReader)
            .guiDataHandler(errorReportHandler)
            .build())
        .doClick();

    verify(errorReportHandler, times(1))
        .accept(userErrorReport);
  }

}
