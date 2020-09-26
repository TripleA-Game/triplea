package tools.map.making.ui.upload.map.validators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.java.function.ThrowingFunction;

@AllArgsConstructor
public class MapValidator {
  private final ThrowingFunction<Path, List<String>, IOException> validationFunction;

  public List<String> validate(Path path) throws IOException {
    return validationFunction.apply(path);
  }
}
