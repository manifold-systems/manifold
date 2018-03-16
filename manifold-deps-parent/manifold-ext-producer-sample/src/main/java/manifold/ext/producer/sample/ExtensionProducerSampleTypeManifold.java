package manifold.ext.producer.sample;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.host.IModuleComponent;
import manifold.api.host.RefreshKind;
import manifold.api.host.RefreshRequest;
import manifold.api.type.JavaTypeManifold;
import manifold.api.type.ResourceFileTypeManifold;
import manifold.ext.AbstractExtensionProducer;
import manifold.ext.IExtensionClassProducer;
import manifold.util.StreamUtil;
import manifold.util.cache.FqnCache;
import manifold.util.concurrent.LocklessLazyVar;


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