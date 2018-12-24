/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.ext.producer.sample;

import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
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
    return new Model( getModule().getHost(), extensionFqn, files );
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
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, Model model,
                               DiagnosticListener<JavaFileObject> errorHandler )
  {
    return model.makeSource( topLevelFqn, errorHandler );
  }
}