package manifold.api.type;

import java.util.Set;
import javax.tools.DiagnosticListener;
import manifold.api.fs.IFile;

/**
 * For use with {@link ResourceFileTypeManifold}. Implementors of {@link IModel} store and manage
 * state necessary to generate source code in the context of {@link ResourceFileTypeManifold#contribute(String, String, IModel, DiagnosticListener)}
 */
public interface IModel
{
  /**
   * @return The fully qualified type name to which code will be contributed
   */
  String getFqn();

  /**
   * @return The resource file[s] from which information is gathered to contribute source
   */
  Set<IFile> getFiles();

  /**
   * Add {@code file} to the set of files this model uses.  The addition of a new flie
   */
  void addFile( IFile file );

  /**
   * Remove {@code file} from the set of files this model uses
   */
  void removeFile( IFile file );

  /**
   * Updates {@code file} in the set of files this model uses
   */
  void updateFile( IFile file );
}
