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

package manifold.api.json;

import java.util.Collections;
import java.util.Set;
import java.util.StringTokenizer;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;
import manifold.util.ManClassUtil;

/**
 */
public class JsonImplTypeManifold extends JavaTypeManifold<Model>
{
  private static final String FILE_EXTENSION = "json";
  private static final Set<String> FILE_EXTENSIONS = Collections.singleton( FILE_EXTENSION );
  private static final String IMPL = "impl";

  public void init( IModule module )
  {
    init( module, (fqn, files) -> new Model( getModule().getHost(), fqn, files ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  public String getTypeNameForFile( String fqn, IFile file )
  {
    return makeImplName( fqn );
  }

  static String makeImplName( String fqn )
  {
    return ManClassUtil.getPackage( fqn ) + '.' + IMPL + '.' + ManClassUtil.getShortClassName( fqn );
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    Model model = getModel( topLevel );
    JsonStructureType type = model == null ? null : model.getType();
    if( type == null )
    {
      return false;
    }
    JsonStructureType csr = type;
    for( StringTokenizer tokenizer = new StringTokenizer( relativeInner, "." ); tokenizer.hasMoreTokens(); )
    {
      String childName = tokenizer.nextToken();
      IJsonType child = csr.findChild( childName );
      if( child instanceof JsonStructureType )
      {
        csr = (JsonStructureType)child;
        continue;
      }
      else if( child instanceof JsonListType )
      {
        IJsonType componentType = ((JsonListType)child).getComponentType();
        if( componentType instanceof JsonStructureType )
        {
          csr = (JsonStructureType)componentType;
          continue;
        }
      }
      return false;
    }
    return true;
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    StringBuilder sb = new StringBuilder();
    SrcClass srcClass = new JsonImplCodeGen( model.getType(), topLevelFqn ).make();
    model.report( errorHandler );
    srcClass.render( sb, 0 );
    return sb.toString();
  }
}