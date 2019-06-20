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

package manifold.internal.javac;

import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.parser.JavaTokenizer;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.JavadocTokenizer;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.util.Context;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.CharBuffer;
import manifold.util.ReflectUtil;

/**
 * Override ParserFactory to:<br>
 * - facilitate a pluggable Java preprocessor<br>
 * - handle embedded file fragments in comment tokens<br>
 */
public class ManParserFactory extends ParserFactory
{
  private TaskEvent _taskEvent;

  public static ManParserFactory instance( Context ctx )
  {
    ParserFactory parserFactory = ctx.get( parserFactoryKey );
    if( !(parserFactory instanceof ManParserFactory) )
    {
      ctx.put( parserFactoryKey, (ParserFactory)null );
      parserFactory = new ManParserFactory( ctx );
    }

    return (ManParserFactory)parserFactory;
  }

  private ManParserFactory( Context ctx )
  {
    super( ctx );
    ReflectUtil.field( this, "scannerFactory" ).set( ManScannerFactory.instance( ctx, this ) );
  }

  private ThreadLocal<Boolean> chainedCall = new ThreadLocal<>();
  @Override
  public JavacParser newParser( CharSequence input, boolean keepDocComments, boolean keepEndPos, boolean keepLineMap )
  {
    chainedCall.set( true );
    try
    {
      input = new Preprocessor( this ).process( input );
      return super.newParser( input, keepDocComments, keepEndPos, keepLineMap );
    }
    finally
    {
      chainedCall.set( false );
    }
  }

  // Java 9+
  @SuppressWarnings("unused")
  public JavacParser newParser( CharSequence input, boolean keepDocComments, boolean keepEndPos, boolean keepLineMap, boolean parseModuleInfo )
  {
    if( chainedCall.get() == null || !chainedCall.get() )
    {
      // avoid preprocessing 2X
      input = new Preprocessor( this ).process( input );
    }

    try
    {
      // Call super.newParser(...);
      //noinspection JavaLangInvokeHandleSignature
      MethodHandle super_newParser = MethodHandles.lookup().findSpecial( ParserFactory.class, "newParser",
        MethodType.methodType( JavacParser.class, CharSequence.class, boolean.class, boolean.class, boolean.class, boolean.class ),
        ManParserFactory.class );
      return (JavacParser)super_newParser.invoke( this, input, keepDocComments, keepEndPos, keepLineMap, parseModuleInfo );
    }
    catch( Throwable e )
    {
      throw new RuntimeException( e );
    }
  }

  void setTaskEvent( TaskEvent e )
  {
    _taskEvent = e;
  }

  /**
   * Override ScannerFactory so we can examine tokens as they are read.  This is purely a performance measure to avoid
   * having to tokenize each source file twice.
   */
  public static class ManScannerFactory extends ScannerFactory
  {
    private final ManParserFactory _parserFactory;

    public static ScannerFactory instance( Context ctx, ManParserFactory parserFactory )
    {
      ScannerFactory scannerFactory = ctx.get( scannerFactoryKey );
      if( !(scannerFactory instanceof ManScannerFactory) )
      {
        ctx.put( scannerFactoryKey, (ScannerFactory)null );
        scannerFactory = new ManScannerFactory( ctx, parserFactory );
      }

      return scannerFactory;
    }

    private ManScannerFactory( Context ctx, ManParserFactory parserFactory )
    {
      super( ctx );
      _parserFactory = parserFactory;
    }

    public Scanner newScanner( CharSequence input, boolean keepDocComments )
    {
      if( input instanceof CharBuffer )
      {
        CharBuffer buf = (CharBuffer)input;
        if( keepDocComments )
        {
          return new ManScanner( this, new ManJavadocTokenizer( this, buf ) );
        }
        else
        {
          return new ManScanner( this, buf );
        }
      }
      else
      {
        char[] array = input.toString().toCharArray();
        return newScanner( array, array.length, keepDocComments );
      }
    }

    public Scanner newScanner( char[] input, int inputLength, boolean keepDocComments )
    {
      if( keepDocComments )
      {
        return new ManScanner( this, new ManJavadocTokenizer( this, input, inputLength ) );
      }
      else
      {
        return new ManScanner( this, input, inputLength );
      }
    }

    private class ManJavadocTokenizer extends JavadocTokenizer
    {
      private final ManScannerFactory _scannerFactory;

      ManJavadocTokenizer( ManScannerFactory manScannerFactory, CharBuffer buf )
      {
        super( manScannerFactory, buf );
        _scannerFactory = manScannerFactory;
      }

      ManJavadocTokenizer( ManScannerFactory manScannerFactory, char[] input, int inputLength )
      {
        super( manScannerFactory, input, inputLength );
        _scannerFactory = manScannerFactory;
      }

      protected Tokens.Comment processComment( int pos, int endPos, Tokens.Comment.CommentStyle style )
      {
        Tokens.Comment comment = super.processComment( pos, endPos, style );
        char[] buf = reader.getRawCharacters( pos, endPos );
        CommentProcessor.instance().processComment(
          _scannerFactory._parserFactory._taskEvent.getSourceFile(), pos, new String( buf ), style );
        return comment;
      }
    }

    private static class ManScanner extends Scanner
    {
      ManScanner( ManScannerFactory manScannerFactory, JavaTokenizer manJavadocTokenizer )
      {
        super( manScannerFactory, manJavadocTokenizer );
      }

      ManScanner( ManScannerFactory fac, char[] buf, int len )
      {
        this( fac, new ManJavaTokenizer( fac, buf, len ) );
      }

      ManScanner( ManScannerFactory fac, CharBuffer buf )
      {
        this( fac, new ManJavaTokenizer( fac, buf ) );
      }

      private static class ManJavaTokenizer extends JavaTokenizer
      {
        ManJavaTokenizer( ManScannerFactory fac, char[] buf, int len )
        {
          super( fac, buf, len );
        }

        ManJavaTokenizer( ManScannerFactory fac, CharBuffer buf )
        {
          super( fac, buf );
        }

        protected Tokens.Comment processComment( int pos, int endPos, Tokens.Comment.CommentStyle style )
        {
          Tokens.Comment comment = super.processComment( pos, endPos, style );
          char[] buf = reader.getRawCharacters( pos, endPos );
          CommentProcessor.instance().processComment(
            ((ManScannerFactory)fac)._parserFactory._taskEvent.getSourceFile(), pos, new String( buf ), style );
          return comment;
        }
      }
    }
  }
}