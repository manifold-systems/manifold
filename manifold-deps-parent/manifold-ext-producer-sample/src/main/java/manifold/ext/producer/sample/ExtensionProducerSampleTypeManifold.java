package manifold.ext.producer.sample;

import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.ext.AbstractExtensionProducer;


/**
 * A sample implementation to exercise the IExtensionClassProducer interface.
 * <p/>
 * Handles the contrived ".favs" file extension having the following format:
 * <pre>
 *   (&lt;qualified-type-name&gt; | &lt;favorite-name&gt; | &lt;favorite-value&gt; [new line])*
 * </pre>
 * For example:
 * <pre>
 *   java.lang.String|Food|Cheeseburger
 *   java.lang.String|Car|Alfieri
 *   java.util.Map|Food|Pizza
 * </pre>
 * As such this class adds methods favoriteFood() and favoriteCar() to String, and favoriteFood() to Map.
 * The methods return a String value corresponding with Cheeseburger, Alfieri, and Pizza.
 */
public class ExtensionProducerSampleTypeManifold extends AbstractExtensionProducer<Model>
{
  private static final String FILE_EXT = "favs";

  @Override
  protected Model createModel( String extensionFqn, Set<IFile> files )
  {
    return new Model( extensionFqn, files );
  }

  @Override
  protected String getFileExt()
  {
    return FILE_EXT;
  }

  protected Set<String> getExtendedTypes( IFile file )
  {
    return Model.getExtendedTypes( file );
  }

  @Override
  protected String makeExtensionClassName( String extendedClassFqn )
  {
    return Model.makeExtensionClassName( extendedClassFqn );
  }

  @Override
  protected String deriveExtendedClassFrom( String extensionClassFqn )
  {
    return Model.deriveExtendedClassFrom( extensionClassFqn );
  }

  @Override
  protected String contribute( String topLevelFqn, String existing, Model model,
                               DiagnosticListener<JavaFileObject> errorHandler )
  {
    return model.makeSource( topLevelFqn, errorHandler );
  }
}