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

package manifold.internal.javac;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import manifold.api.fs.IFileUtil;

import static manifold.util.ManExceptionUtil.unchecked;

/**
 * Manifold currently compiles with Java 8, however there are some files that
 * must compile to Java 9, 10, 11, etc.  To facilitate a single build, we
 * maintain a naming convention where if a Java 8 source file has a Java 9 or
 * later counterpart we encode the Java version in the source file name e.g.,
 * <pre>
 *   ManAttr_8.java
 *   ManAttr_9.java9
 * </pre>
 * If we change a java9 file, we need to temporarily rename the Java 8 version to
 * '*.java8' and rename the java9 version to '*.java', compile with Java 9, copy
 * the corresponding Java 9 .class files to the resource dir, reverse the renaming,
 * and recompile back to Java 8.  It's messy, but is infrequent enough to justify
 * the process.
 * <p/>
 * Renames java source files:
 * <pre>
 * MyClass_X.javaX -> MyClass_X.java
 * MyClass_Y.java -> MyClass_Y.javaY
 * </pre>
 * Where X and Y are supplied as local variables {@code javaNum} and {$code textNum}, respectively.
 */
public class RenameSourceFilesForJava9Build
{
  public static class _8_will_become_java_files
  {
    public static void main( String[] args ) throws IOException, URISyntaxException
    {
      doIt( 8, 9 );
    }
  }
  public static class _9_will_become_java_files
  {
    public static void main( String[] args ) throws IOException, URISyntaxException
    {
      doIt( 9, 8 );
    }
  }
  private static void doIt( int javaNum, int textNum ) throws URISyntaxException, IOException
  {
//    int javaNum = 8; // will become .java files
//    int textNum = 9; // wil become .javaX files

    URI uri = RenameSourceFilesForJava9Build.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    Path target_classes_ = Paths.get( uri );
    Path manifold_ = target_classes_.getParent().getParent();
    Path src_ = manifold_.resolve( "src" ).resolve( "main" ).resolve( "java" );
    Files.walk( src_ )
      .forEach( pathJavaFile -> {
        if( Files.isRegularFile( pathJavaFile ) )
        {
          String fileName = pathJavaFile.getFileName().toString();
          if( fileName.endsWith( ".java"+javaNum ) )
          {
            String baseJavaFile = IFileUtil.getBaseName( fileName );
            if( baseJavaFile.endsWith( "_"+javaNum ) )
            {
              String baseTextFile = baseJavaFile.substring( 0, baseJavaFile.length()-1 ) + textNum;
              Path pathTextFile = pathJavaFile.getParent().resolve( baseTextFile + ".java" );
              if( Files.isRegularFile( pathTextFile ) )
              {
                try
                {
                  Files.move( pathTextFile, pathJavaFile.getParent().resolve( baseTextFile + ".java"+textNum ) );
                }
                catch( IOException e )
                {
                  throw unchecked( e );
                }
              }

              try
              {
                Files.move( pathJavaFile, pathJavaFile.getParent().resolve( baseJavaFile + ".java" ) );
              }
              catch( IOException e )
              {
                throw unchecked( e );
              }
            }
            else
            {
              System.err.println( "Found file without '_" + javaNum + "' base suffix: " + fileName );
            }
          }
        }
      } );
  }
}
