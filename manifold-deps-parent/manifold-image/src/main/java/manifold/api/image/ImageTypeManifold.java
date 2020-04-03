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

package manifold.api.image;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;

/**
 */
public class ImageTypeManifold extends JavaTypeManifold<Model>
{
  private static final Set<String> FILE_EXTENSIONS = new HashSet<>( Arrays.asList( "jpg", "png", "bmp", "wbmp", "gif" ) );

  @Override
  public void init( IModule module )
  {
    init( module, (host, fqn) -> new Model( getModule().getHost(), host, fqn ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  public String getTypeNameForFile( String fqn, IFile file )
  {
    if( !(fqn.endsWith( file.getBaseName() + '_' + file.getExtension() )) )
    {
      fqn = fqn + '_' + file.getExtension();
    }
    return fqn;
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    return false;
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, boolean genStubs, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcClass = new ImageCodeGen( model._url, topLevelFqn ).make();
    StringBuilder sb = srcClass.render( new StringBuilder(), 0 );
    return sb.toString();
  }
}