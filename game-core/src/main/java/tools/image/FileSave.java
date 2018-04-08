package tools.image;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.google.common.base.Strings;

import games.strategy.engine.framework.system.SystemProperties;

public class FileSave {
  private final File file;

  public FileSave(final String title, final String name, final File currentDirectory) {
    this(title, name, currentDirectory, JFileChooser.DIRECTORIES_ONLY, null, new FileFilter() {
      @Override
      public boolean accept(final File f) {
        return f.isDirectory();
      }

      @Override
      public String getDescription() {
        return "Folder To Save In";
      }
    });
  }

  /**
   * @param fileSelectionMode The type of files to be displayed. Must be one of {@link JFileChooser#FILES_ONLY},
   *        {@link JFileChooser#DIRECTORIES_ONLY}, or {@link JFileChooser#FILES_AND_DIRECTORIES}.
   */
  public FileSave(final String title, final int fileSelectionMode, final File selectedFile,
      final File currentDirectory) {
    this(title, null, currentDirectory, fileSelectionMode, selectedFile, null);
  }

  /**
   * @param fileSelectionMode The type of files to be displayed. Must be one of {@link JFileChooser#FILES_ONLY},
   *        {@link JFileChooser#DIRECTORIES_ONLY}, or {@link JFileChooser#FILES_AND_DIRECTORIES}.
   */
  public FileSave(final String title, final String name, final File currentDirectory, final int fileSelectionMode,
      final File selectedFile, final FileFilter fileFilter) {
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(fileSelectionMode);
    chooser.setDialogTitle(title);
    if (selectedFile != null) {
      chooser.setSelectedFile(selectedFile);
    }
    chooser.setCurrentDirectory(((currentDirectory == null || !currentDirectory.exists())
        ? new File(SystemProperties.getUserDir())
        : currentDirectory));
    if (fileFilter != null) {
      chooser.setFileFilter(fileFilter);
    }
    file = chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION
        ? new File(chooser.getSelectedFile(), Strings.nullToEmpty(name))
        : null;
  }

  /**
   * Returns the directory path as a File object.
   */
  public File getFile() {
    return file;
  }

  /**
   * Returns the directory path as as string.
   */
  public String getPathString() {
    return (file == null) ? null : file.getPath();
  }
}
