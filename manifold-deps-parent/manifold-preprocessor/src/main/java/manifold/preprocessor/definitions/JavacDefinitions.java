/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.preprocessor.definitions;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import manifold.internal.javac.JavacPlugin;
import manifold.util.concurrent.LocklessLazyVar;

import java.util.HashMap;
import java.util.Map;

public class JavacDefinitions
{
  private static LocklessLazyVar<JavacDefinitions> INSTANCE =
    LocklessLazyVar.make( () -> new JavacDefinitions() );

  private final Map<String, String> _env;

  static JavacDefinitions instance()
  {
    return INSTANCE.get();
  }

  public JavacDefinitions()
  {
    Map<String, String> map = new HashMap<>();
    addJavacEnvironment( map );
    _env = map;
  }

  public Map<String, String> getEnv()
  {
    return _env;
  }

  protected void addJavacEnvironment( Map<String, String> map )
  {
    if( JavacPlugin.instance() == null )
    {
      return;
    }

    JavacProcessingEnvironment jpe = JavacProcessingEnvironment.instance( JavacPlugin.instance().getContext() );
    addAnnotationOptions( map, jpe );
  }

  /**
   * These are the {@code -Akey[=value]} options on the javac command line, much like {@code -D}, but for the javac
   * environment, not the JVM. Intended for use with annotations, but also great for a preprocessor.
   * See <a href="https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javac.html">Standard Options</a>.
   */
  protected void addAnnotationOptions( Map<String, String> map, JavacProcessingEnvironment jpe )
  {
    Map<String, String> options = jpe.getOptions();
    if( options != null )
    {
      map.putAll( options );
    }
  }
}
