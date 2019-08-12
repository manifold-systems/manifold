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

package manifold.api.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Predicate;

public class StreamUtil
{
  // Using a relatively large block size to reduce the number of (very slow)
  // native calls into the file system
  private static final int BLOCK_SIZE = 8192;

  private StreamUtil()
  {
    /* disable construction */
  }

  /**
   * Converts the specified character sequence to bytes using UTF-8.
   *
   * @param seq the character sequence to convert
   *
   * @return the UTF-8 encoded result
   */
  public static byte[] toBytes( CharSequence seq )
  {
    try
    {
      return seq.toString().getBytes( "UTF-8" );
    }
    catch( UnsupportedEncodingException ex )
    {
      throw new RuntimeException( ex ); // shouldn't happen since UTF-8 is supported by all JVMs per spec
    }
  }

  /**
   * Converts the specified byte array to a String using UTF-8.
   *
   * @param bytes the bytes to convert
   *
   * @return the resulting string
   */
  public static String toString( byte[] bytes )
  {
    try
    {
      return new String( bytes, "UTF-8" );
    }
    catch( UnsupportedEncodingException ex )
    {
      throw new RuntimeException( ex ); // shouldn't happen since UTF-8 is supported by all JVMs per spec
    }
  }

  /**
   * Converts the specified byte array to a String using UTF-8.
   *
   * @param bytes  the bytes to convert
   * @param offset the index of the first byte to decode
   * @param length the number of bytes to decode
   *
   * @return the resulting string
   */
  public static String toString( byte[] bytes, int offset, int length )
  {
    try
    {
      return new String( bytes, offset, length, "UTF-8" );
    }
    catch( UnsupportedEncodingException ex )
    {
      throw new RuntimeException( ex ); // shouldn't happen since UTF-8 is supported by all JVMs per spec
    }
  }

  /**
   * Converts the specified property file text to a Properties object.
   *
   * @param propFileText the property file text in standard property file format
   *
   * @return the resulting Properties object
   *
   * @throws CharacterCodingException if invalid encoding
   */
  public static Properties toProperties( String propFileText ) throws CharacterCodingException
  {
    CharsetEncoder encoder = Charset.forName( "ISO-8859-1" ).newEncoder().onUnmappableCharacter( CodingErrorAction.REPORT );
    byte[] bytes = encoder.encode( CharBuffer.wrap( propFileText ) ).array();
    Properties props = new Properties();
    try
    {
      props.load( new ByteArrayInputStream( bytes ) );
    }
    catch( IOException ex )
    {
      throw new RuntimeException( ex ); // shouldn't happen with BAIS
    }
    return props;
  }

  /**
   * Returns a reader for the specified input stream, using UTF-8 encoding.
   *
   * @param in the input stream to wrap
   *
   * @return a reader for this input stream
   */
  public static Reader getInputStreamReader( InputStream in )
  {
    return getInputStreamReader( in, "UTF-8" );
  }

  /**
   * Returns a reader for the specified input stream, using specified encoding.
   *
   * @param in      the input stream to wrap
   * @param charset the input stream to wrap
   *
   * @return a reader for this input stream
   */
  public static Reader getInputStreamReader( InputStream in, String charset )
  {
    try
    {
      return new InputStreamReader( in, charset );
    }
    catch( UnsupportedEncodingException ex )
    {
      throw new RuntimeException( ex ); // shouldn't happen since UTF-8 is supported by all JVMs per spec
    }
  }

  /**
   * Returns a writer for the specified output stream, using UTF-8 encoding.
   *
   * @param out the output stream to wrap
   *
   * @return a writer for this output stream
   */
  public static Writer getOutputStreamWriter( OutputStream out )
  {
    try
    {
      return new OutputStreamWriter( out, "UTF-8" );
    }
    catch( UnsupportedEncodingException ex )
    {
      throw new RuntimeException( ex ); // shouldn't happen since UTF-8 is supported by all JVMs per spec
    }
  }

  /**
   * Returns an input stream for the specified character sequence, using UTF-8 encoding.
   *
   * @param cs the character sequence to wrap
   *
   * @return an input stream for reading the specified character sequence
   */
  public static InputStream getStringInputStream( CharSequence cs )
  {
    return new ByteArrayInputStream( toBytes( cs ) );
  }

  /**
   * Returns the content of the specified input stream. The stream will be closed after calling this method.
   *
   * @param in the input stream to read
   *
   * @return the content of the input stream
   *
   * @throws IOException if an I/O error occurs
   */
  public static byte[] getContent( InputStream in ) throws IOException
  {
    try
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      copy( in, baos );
      return baos.toByteArray();
    }
    finally
    {
      in.close();
    }
  }

  /**
   * Returns the content of the specified reader. The reader will be closed after calling this method.
   *
   * @param in the reader to read
   *
   * @return the content of the reader
   *
   * @throws IOException if an I/O error occurs
   */
  public static String getContent( Reader in ) throws IOException
  {
    try
    {
      StringWriter sw = new StringWriter();
      copy( in, sw );
      return sw.toString();
    }
    finally
    {
      in.close();
    }
  }

  /**
   * Copies the content of an input stream to an output stream.
   *
   * @param in  the input stream to read
   * @param out the output stream to write
   *
   * @throws IOException if an I/O error occurs
   */
  public static void copy( InputStream in, OutputStream out ) throws IOException
  {
//    if( in instanceof FileInputStream && out instanceof FileOutputStream )
//    {
//      copy( (FileInputStream)in, (FileOutputStream)out );
//    }
//    else
//    {
    byte[] buf = new byte[BLOCK_SIZE];
    while( true )
    {
      int count = in.read( buf );
      if( count < 0 )
      {
        break;
      }
      out.write( buf, 0, count );
    }
    out.flush();
//    }
  }

  /**
   * Copies the content of an input stream to a writer.
   *
   * @param in     the input stream to read
   * @param writer the writer to write
   *
   * @throws IOException if an I/O error occurs
   */
  public static void copy( InputStream in, Writer writer ) throws IOException
  {
    copy( getInputStreamReader( in ), writer );
    writer.flush();
  }

  /**
   * Copies the content of a reader to an output stream.
   *
   * @param reader the reader to read
   * @param out    the output stream to write
   *
   * @throws IOException if an I/O error occurs
   */
  public static void copy( Reader reader, OutputStream out ) throws IOException
  {
    copy( reader, getOutputStreamWriter( out ) );
    out.flush();
  }

  /**
   * Copies the content of a reader to a writer.
   *
   * @param in  the reader to read
   * @param out the writer to write
   *
   * @throws IOException if an I/O error occurs
   */
  public static void copy( Reader in, Writer out ) throws IOException
  {
    char[] buf = new char[BLOCK_SIZE];
    while( true )
    {
      int count = in.read( buf );
      if( count < 0 )
      {
        break;
      }
      out.write( buf, 0, count );
    }
    out.flush();
  }

  /**
   * Recursively copy a file or directory to a directory.
   */
  public static void copy( File fileOrDirectory, File toDir )
  {
    copy( fileOrDirectory, toDir, null );
  }

  public static void copy( File fileOrDirectory, File toDir, Predicate<File> filter )
  {
    if( filter == null || filter.test( fileOrDirectory ) )
    {
      File copy = new File( toDir, fileOrDirectory.getName() );
      if( fileOrDirectory.isDirectory() )
      {
        //noinspection ResultOfMethodCallIgnored
        copy.mkdir();
        for( File child : fileOrDirectory.listFiles() )
        {
          copy( child, copy, filter );
        }
      }
      else
      {
        //noinspection ResultOfMethodCallIgnored
        try (InputStream is = new FileInputStream( fileOrDirectory );
             OutputStream os = new FileOutputStream( copy ))
        {
          StreamUtil.copy( is, os );
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }
    }
  }

  /**
   * Recursively copy a file or directory to a directory.
   */
  public static void copy( Path fileOrDirectory, Path toDir )
  {
    Path copy = PathUtil.create( toDir, PathUtil.getName( fileOrDirectory ) );
    if( PathUtil.isDirectory( fileOrDirectory ) )
    {
      //noinspection ResultOfMethodCallIgnored
      try
      {
        PathUtil.mkdir( copy );
        Files.list( fileOrDirectory ).forEach( child -> copy( child, copy ) );
      }
      catch( IOException e )
      {
        throw new RuntimeException( e );
      }
    }
    else
    {
      //noinspection ResultOfMethodCallIgnored
      try (InputStream is = Files.newInputStream( fileOrDirectory );
           OutputStream os = Files.newOutputStream( copy ))
      {
        StreamUtil.copy( is, os );
      }
      catch( Exception e )
      {
        throw new RuntimeException( e );
      }
    }
  }

  /**
   * Close and swallow exception the exception.  For use in finally blocks
   * where the other io exceptions is what is wanted to be thrown.
   *
   * @param stream the streams to close
   */
  public static void closeNoThrow( Closeable stream )
  {
    if( stream != null )
    {
      try
      {
        stream.close();
      }
      catch( IOException ignore )
      {
      }
    }
  }

  /**
   * Closes the specified streams, suppressing any IOExceptions for inputstreams and readers.
   * Even if an I/O exception is thrown, all streams can be considered closed.
   *
   * @param streams the streams to close
   *
   * @throws IOException if an i/o exception occurs while closing any outputstream or writer
   */
  public static void close( Closeable... streams ) throws IOException
  {
    close( streams, 0 );
  }

  private static void close( Closeable[] streams, int idx ) throws IOException
  {
    if( idx >= streams.length )
    {
      return; // done
    }
    Closeable stream = streams[idx];
    try
    {
      if( stream != null )
      {
        if( stream instanceof Flushable )
        {
          ((Flushable)stream).flush();
        }
        stream.close();
      }
    }
    catch( IOException ex )
    {
      if( !(stream instanceof InputStream || stream instanceof Reader) )
      {
        throw ex; // ignore io exceptions for input streams and readers
      }
    }
    finally
    {
      close( streams, idx + 1 );
    }
  }

//  /**
//   * Recursively copy a file or directory to a directory.
//   */
//  public static void copy( File fileOrDirectory, File toDir )
//  {
//    copy( fileOrDirectory, toDir, null );
//  }
//  /**
//   * Recursively copy a file or directory to a directory.
//   */
//  public static void copy( File fileOrDirectory, File toDir, Predicate<Path> filter )
//  {
//    copy( fileOrDirectory.toPath(), toDir.toPath(), filter );
//  }
//
//  /**
//   * Recursively copy a file or directory to a directory.
//   */
//  public static void copy( Path fileOrDirectory, Path toDir )
//  {
//    copy( fileOrDirectory, toDir, null );
//  }
//  /**
//   * Recursively copy a file or directory to a directory.
//   */
//  public static void copy( Path fileOrDirectory, Path toDir, Predicate<Path> filter )
//  {
//    try
//    {
//      Files.walkFileTree( fileOrDirectory,
//                          EnumSet.of( FileVisitOption.FOLLOW_LINKS ), Integer.MAX_VALUE,
//                          new CopyFileVisitor( toDir, filter ) );
//    }
//    catch( IOException ioe )
//    {
//      throw new RuntimeException( ioe );
//    }
//  }
//
//  private static class CopyFileVisitor extends SimpleFileVisitor<Path>
//  {
//    private Path _from;
//    private Path _to;
//    private Predicate<Path> _filter;
//
//    CopyFileVisitor( Path to, Predicate<Path> filter )
//    {
//      _to = to;
//      _filter = filter;
//    }
//
//    @Override
//    public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException
//    {
//      if( _filter != null && !_filter.test( dir ) )
//      {
//        return FileVisitResult.SKIP_SUBTREE;
//      }
//
//      if( _from == null )
//      {
//        _from = dir;
//      }
//      else
//      {
//        Files.createDirectories( _to.resolve( _from.relativize( dir ) ) );
//      }
//      return FileVisitResult.CONTINUE;
//    }
//
//    @Override
//    public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException
//    {
//      if( _filter == null || _filter.test( file ) )
//      {
//        Files.copy( file, _to.resolve( _from.relativize( file ) ) );
//      }
//      return FileVisitResult.CONTINUE;
//    }
//  }
}