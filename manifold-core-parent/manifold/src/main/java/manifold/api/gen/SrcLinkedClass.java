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

package manifold.api.gen;

import java.lang.reflect.Modifier;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileUtil;
import manifold.api.type.ActualName;
import manifold.api.type.SourcePosition;
import manifold.util.JsonUtil;
import manifold.util.ManStringUtil;

public class SrcLinkedClass extends AbstractSrcClass<SrcLinkedClass>
{
  @SuppressWarnings("WeakerAccess")
  protected static final String FIELD_FILE_URL = "__FILE_URL_";

  private IFile _linkedFile;

  public SrcLinkedClass( String fqn, Kind kind, IFile linkedFile )
  {
    super( fqn, kind );
    _linkedFile = linkedFile;
    addFileField();
  }

  public SrcLinkedClass( String fqn, AbstractSrcClass enclosingClass, Kind kind )
  {
    super( fqn, enclosingClass, kind );
  }

  @SuppressWarnings("WeakerAccess")
  protected void addFileField()
  {
    IFile linkedFile = getLinkedFile();
    if( linkedFile == null )
    {
      throw new IllegalStateException( "Expecting non-null linkedFile" );
    }

    addField(
      new SrcField( FIELD_FILE_URL, String.class )
        .modifiers( isInterface() ? 0 : Modifier.STATIC | Modifier.FINAL )
        .initializer( new SrcRawExpression( "\"" + linkedFile.toURI().toString() + "\"" ) ) );
  }

  public void addSourcePositionAnnotation( SrcAnnotated srcAnno, String name, int line, int column )
  {
    SrcAnnotationExpression annotation = new SrcAnnotationExpression( SourcePosition.class )
      .addArgument( new SrcArgument( new SrcMemberAccessExpression( FIELD_FILE_URL ) ).name( "url" ) )
      .addArgument( "feature", String.class, name )
      .addArgument( "offset", int.class, IFileUtil.findOffset( getLinkedFile(), line, column ) )
      .addArgument( "length", int.class, name.length() );
    srcAnno.addAnnotation( annotation );
  }

  private IFile getLinkedFile()
  {
    if( _linkedFile == null )
    {
      AbstractSrcClass enclosingClass = getEnclosingClass();
      if( enclosingClass instanceof SrcLinkedClass )
      {
        return ((SrcLinkedClass)enclosingClass).getLinkedFile();
      }
      throw new IllegalStateException( "Expecting non-null _linkedFile" );
    }
    else
    {
      return _linkedFile;
    }
  }

  public static void addActualNameAnnotation( SrcAnnotated srcAnno, String name, boolean capitalize )
  {
    String identifier = makeIdentifier( name, capitalize );
    if( !identifier.equals( name ) )
    {
      srcAnno.addAnnotation( new SrcAnnotationExpression( ActualName.class.getSimpleName() )
        .addArgument( new SrcArgument( new SrcRawExpression( '"'+name+'"' ) ) ) );
    }
  }

  public static String makeIdentifier( String name, boolean capitalize )
  {
    return capitalize ? ManStringUtil.capitalize( JsonUtil.makeIdentifier( name ) ) : JsonUtil.makeIdentifier( name );
  }
}
