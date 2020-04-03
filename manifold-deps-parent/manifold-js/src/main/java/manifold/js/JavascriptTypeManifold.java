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

package manifold.js;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.gen.SrcClass;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;

public class JavascriptTypeManifold extends JavaTypeManifold<JavascriptModel>
{
  public static final String JS = "js";
  public static final String JST = "jst";
  private static final Set<String> FILE_EXTENSIONS = new HashSet<>( Arrays.asList( JS, JST ) );

  public void init( IModule module )
  {
    init( module, (fqn, files) -> new JavascriptModel( getModule().getHost(), fqn, files ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    return false;
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, boolean genStubs, String existing, JavascriptModel model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcClass = new JavascriptCodeGen( model.getFiles().iterator().next(), topLevelFqn ).make( errorHandler );
    return srcClass.render( new StringBuilder(), 0).toString();
  }
}