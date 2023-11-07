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
import com.sun.tools.javac.parser.*;
import com.sun.tools.javac.util.Context;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.JavaFileObject;

import manifold.util.JreUtil;
import manifold.util.ReflectUtil;


import static com.sun.tools.javac.parser.Tokens.TokenKind.STRINGLITERAL;

/**
 * Override ParserFactory to:<br>
 * - facilitate a pluggable Java preprocessor<br>
 * - handle inlined file fragments in comment tokens<br>
 */
public class ManParserFactory_8 extends ParserFactory implements ParserFactoryFiles
{
  private TaskEvent _taskEvent;
  private final Preprocessor _preprocessor;

  public static ManParserFactory_8 instance( Context ctx )
  {
    ParserFactory parserFactory = ctx.get( parserFactoryKey );
    if( !(parserFactory instanceof ManParserFactory_8) )
    {
      ctx.put( parserFactoryKey, (ParserFactory)null );
      parserFactory = new ManParserFactory_8( ctx );
    }

    return (ManParserFactory_8)parserFactory;
  }

  private ManParserFactory_8( Context ctx )
  {
    super( ctx );
    _preprocessor = Preprocessor.instance( ctx );
    ReflectUtil.field( this, "scannerFactory" ).set( ManScannerFactory.instance( ctx, this ) );
  }

  @Override
  public JavacParser newParser( CharSequence input, boolean keepDocComments, boolean keepEndPos, boolean keepLineMap )
  {
    return newParser( input, keepDocComments, keepEndPos, keepLineMap, false );
  }

  //override Java 9+
  @SuppressWarnings("unused")
  public JavacParser newParser( CharSequence input, boolean keepDocComments, boolean keepEndPos, boolean keepLineMap, boolean parseModuleInfo )
  {
    input = _preprocessor.process( _taskEvent.getSourceFile(), input );
    mapInput( _taskEvent.getSourceFile(), input );
    Lexer lexer = ((ScannerFactory)ReflectUtil.field( this, "scannerFactory" ).get()).newScanner(input, keepDocComments);
    return (JavacParser)ReflectUtil.constructor( "com.sun.tools.javac.parser.ManJavacParser",
      ParserFactory.class, Lexer.class, boolean.class, boolean.class, boolean.class, boolean.class )
      .newInstance( this, lexer, keepDocComments, keepLineMap, keepEndPos, parseModuleInfo );
  }

  private void mapInput( JavaFileObject sourceFile, CharSequence input )
  {
    fileToProcessedInput.put( sourceFile.getName(), input );
  }

  @Override
  public void setTaskEvent( TaskEvent e )
  {
    _taskEvent = e;
  }

  /**
   * Override ScannerFactory so we can examine tokens as they are read.  This is purely a performance measure to avoid
   * having to tokenize each source file twice.
   */
  public static class ManScannerFactory extends ScannerFactory
  {
    private final ManParserFactory_8 _parserFactory;

    public static ScannerFactory instance( Context ctx, ManParserFactory_8 parserFactory )
    {
      ScannerFactory scannerFactory = ctx.get( scannerFactoryKey );
      if( !(scannerFactory instanceof ManScannerFactory) )
      {
        ctx.put( scannerFactoryKey, (ScannerFactory)null );
        scannerFactory = new ManScannerFactory( ctx, parserFactory );
      }

      return scannerFactory;
    }

    private ManScannerFactory( Context ctx, ManParserFactory_8 parserFactory )
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
      private final UnicodeReader _reader;

      ManJavadocTokenizer( ManScannerFactory manScannerFactory, CharBuffer buf )
      {
        super( manScannerFactory, buf );
        _scannerFactory = manScannerFactory;
        //noinspection ConstantConditions
        _reader = JreUtil.isJava16orLater() ? (UnicodeReader)(Object)this : reader();
      }

      private UnicodeReader reader()
      {
        return (UnicodeReader)ReflectUtil.field( this, "reader" ).get();
      }

      ManJavadocTokenizer( ManScannerFactory manScannerFactory, char[] input, int inputLength )
      {
        super( manScannerFactory, input, inputLength );
        _scannerFactory = manScannerFactory;
        //noinspection ConstantConditions
        _reader = JreUtil.isJava16orLater() ? (UnicodeReader)(Object)this : reader();
      }

      protected Tokens.Comment processComment( int pos, int endPos, Tokens.Comment.CommentStyle style )
      {
        Tokens.Comment comment = super.processComment( pos, endPos, style );
        char[] buf = _reader.getRawCharacters( pos, endPos );
        FragmentProcessor.instance().processComment(
          _scannerFactory._parserFactory._taskEvent.getSourceFile(), pos, new String( buf ), style );
        return comment;
      }

      public Tokens.Token readToken()
      {
        Tokens.Token token = super.readToken();
        if( token.kind == STRINGLITERAL )
        {
          // todo: passing raw characters means we must parse string literal escaped chars esp. '"', '\n', unicode
          char[] buf = _reader.getRawCharacters( token.pos, token.endPos );
          FragmentProcessor.instance().processString(
            ((ManScannerFactory)ReflectUtil.field( this, "fac" ).get())._parserFactory._taskEvent.getSourceFile(), token.pos, new String( buf ) );
        }
        return token;
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
        private final UnicodeReader _reader;

        ManJavaTokenizer( ManScannerFactory fac, char[] buf, int len )
        {
          super( fac, buf, len );
          //noinspection ConstantConditions
          _reader = JreUtil.isJava16orLater() ? (UnicodeReader)(Object)this : reader();
        }

        ManJavaTokenizer( ManScannerFactory fac, CharBuffer buf )
        {
          super( fac, buf );
          //noinspection ConstantConditions
          _reader = JreUtil.isJava16orLater() ? (UnicodeReader)(Object)this : reader();
        }

        private UnicodeReader reader()
        {
          return (UnicodeReader)ReflectUtil.field( this, "reader" ).get();
        }

        protected Tokens.Comment processComment( int pos, int endPos, Tokens.Comment.CommentStyle style )
        {
          Tokens.Comment comment = super.processComment( pos, endPos, style );
          char[] buf = _reader.getRawCharacters( pos, endPos );
          FragmentProcessor.instance().processComment(
            ((ManScannerFactory)fac)._parserFactory._taskEvent.getSourceFile(), pos, new String( buf ), style );
          return comment;
        }

        public Tokens.Token readToken()
        {
          Tokens.Token token = super.readToken();
          if( token.kind == STRINGLITERAL )
          {
            char[] buf = _reader.getRawCharacters( token.pos, token.endPos );
            FragmentProcessor.instance().processString(
              ((ManScannerFactory)fac)._parserFactory._taskEvent.getSourceFile(), token.pos, new String( buf ) );
          }
          return token;
        }
      }
    }
  }
}