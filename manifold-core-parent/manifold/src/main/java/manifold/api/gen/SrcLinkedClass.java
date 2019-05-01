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

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import manifold.api.fs.IFile;
import manifold.api.type.ActualName;
import manifold.api.type.SourcePosition;
import manifold.util.JsonUtil;
import manifold.util.ManExceptionUtil;
import manifold.util.ManStringUtil;
import manifold.util.StreamUtil;

public class SrcLinkedClass extends AbstractSrcClass<SrcLinkedClass>
{
  @SuppressWarnings("WeakerAccess")
  protected static final String FIELD_FILE_URL = "__FILE_URL_";

  private IFile _linkedFile;
  private Map<IFile, int[]> _resFileToContent;

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
    SrcAnnotationExpression annotation = new SrcAnnotationExpression( SourcePosition.class.getSimpleName() )
      .addArgument( new SrcArgument( new SrcMemberAccessExpression( FIELD_FILE_URL ) ).name( "url" ) )
      .addArgument( "feature", String.class, name )
      .addArgument( "offset", int.class, findOffset( getLinkedFile(), line, column ) )
      .addArgument( "length", int.class, name.length() );
    srcAnno.addAnnotation( annotation );
  }

  private Map<IFile, int[]> getResFileToContent()
  {
    if( _resFileToContent != null )
    {
      return _resFileToContent;
    }

    AbstractSrcClass enclosingClass = getEnclosingClass();
    if( enclosingClass instanceof SrcLinkedClass )
    {
      Map<IFile, int[]> resFileToContent = ((SrcLinkedClass)enclosingClass).getResFileToContent();
      if( resFileToContent != null )
      {
        return resFileToContent;
      }
    }
    return _resFileToContent = new HashMap<>();
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
        .addArgument( new SrcArgument( new SrcRawExpression( '"' + name + '"' ) ) ) );
    }
  }

  public static String makeIdentifier( String name, boolean capitalize )
  {
    return capitalize ? ManStringUtil.capitalize( JsonUtil.makeIdentifier( name ) ) : JsonUtil.makeIdentifier( name );
  }

  public boolean verifyOffset( int line, int column, String startSymbol )
  {
    IFile file = getLinkedFile();
    int offset = findOffset( file, line, column );
    try
    {
      String content = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
      return content.startsWith( startSymbol, offset );
    }
    catch( IOException ioe )
    {
      throw new RuntimeException( ioe );
    }
  }

  private int findOffset( IFile file, int line, int column )
  {
    int[] lineOffsets = getResFileToContent().computeIfAbsent( file,
      f -> {
        try
        {
          String content = StreamUtil.getContent( new InputStreamReader( f.openInputStream() ) );
          ArrayList<Integer> lineOffsetList = new ArrayList<>();
          lineOffsetList.add( 0 );
          for( int index = content.indexOf( '\n' ) + 1; index > 0; index = content.indexOf( '\n', index ) + 1 )
          {
            lineOffsetList.add( index );
          }
          int[] array = new int[lineOffsetList.size()];
          for( int i = 0; i < array.length; i++ )
          {
            array[i] = lineOffsetList.get( i );
          }
          return array;
        }
        catch( IOException ioe )
        {
          throw ManExceptionUtil.unchecked( ioe );
        }
      } );
    return lineOffsets[line - 1] + column - 1;
  }
}
