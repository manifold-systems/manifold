package manifold.io.extensions.java.io.File;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import manifold.ext.api.Extension;
import manifold.ext.api.This;


import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Adapted from kotlin.io.FileReadWrite
 */
@Extension
public class ManFileReadWriteExt
{
  private final static int DEFAULT_BLOCK_SIZE = 4096;

  /**
   * Returns a new [FileReader] for reading the content of this file.
   */
  public static InputStreamReader reader( @This File thiz )
  {
    return thiz.reader( UTF_8 );
  }

  public static InputStreamReader reader( @This File thiz, Charset charset )
  {
    return thiz.inputStream().reader( charset );
  }

  /**
   * Returns a new [BufferedReader] for reading the content of this file.
   *
   * @param bufferSize necessary size of the buffer.
   */
  public static BufferedReader bufferedReader( @This File thiz )
  {
    return thiz.bufferedReader( UTF_8, ManFileExt.DEFAULT_BUFFER_SIZE );
  }

  public static BufferedReader bufferedReader( @This File thiz, Charset charset, int bufferSize )
  {
    return thiz.reader( charset ).buffered( bufferSize );
  }

  /**
   * Returns a new [FileWriter] for writing the content of this file.
   */

  public static OutputStreamWriter writer( @This File thiz )
  {
    return thiz.writer( UTF_8 );
  }

  public static OutputStreamWriter writer( @This File thiz, Charset charset )
  {
    return thiz.outputStream().writer( charset );
  }

  /**
   * Returns a new [BufferedWriter] for writing the content of this file.
   *
   * @param bufferSize necessary size of the buffer.
   */

  public static BufferedWriter bufferedWriter( @This File thiz )
  {
    return thiz.bufferedWriter( UTF_8, ManFileExt.DEFAULT_BUFFER_SIZE );
  }

  public static BufferedWriter bufferedWriter( @This File thiz, Charset charset, int bufferSize )
  {
    return thiz.writer( charset ).buffered( bufferSize );
  }

  /**
   * Returns a new [PrintWriter] for writing the content of this file.
   */
  public static PrintWriter printWriter( @This File thiz )
  {
    return thiz.printWriter( UTF_8 );
  }

  public static PrintWriter printWriter( @This File thiz, Charset charset )
  {
    return new PrintWriter( thiz.bufferedWriter( charset, ManFileExt.DEFAULT_BUFFER_SIZE ) );
  }

  /**
   * Gets the entire content of this file as a byte array.
   * <p>
   * This method is not recommended on huge files. It has an internal limitation of 2 GB byte array size.
   *
   * @return the entire content of this file as a byte array.
   */
  public static byte[] readBytes( @This File thiz )
  {
    try( FileInputStream input = new FileInputStream( thiz ) )
    {
      int offset = 0;
      long remaining = thiz.length();
      if( remaining > Integer.MAX_VALUE )
      {
        throw new OutOfMemoryError( "File $this is too big ($it bytes) to fit in memory." );
      }
      byte[] result = new byte[(int)remaining];
      while( remaining > 0 )
      {
        int count = input.read( result, offset, (int)remaining );
        if( count < 0 )
        {
          break;
        }
        remaining -= count;
        offset += count;
      }
      if( remaining != 0 )
      {
        byte[] copy = new byte[offset];
        System.arraycopy( result, 0, copy, 0, offset );
        result = copy;
      }
      return result;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Sets the content of this file as an [array] of bytes.
   * If this file already exists, it becomes overwritten.
   *
   * @param array byte array to write into this file.
   */
  public static void writeBytes( @This File thiz, byte[] array )
  {
    try( FileOutputStream it = new FileOutputStream( thiz ) )
    {
      it.write( array );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Appends an [array] of bytes to the content of this file.
   *
   * @param array byte array to append to this file.
   */
  public static void appendBytes( @This File thiz, byte[] array )
  {
    try( FileOutputStream it = new FileOutputStream( thiz, true ) )
    {
      it.write( array );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Gets the entire content of this file as a String using UTF-8 or specified [charset].
   * <p>
   * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
   *
   * @param charset character set to use.
   *
   * @return the entire content of this file as a String.
   */
  public static String readText( @This File thiz )
  {
    return thiz.readText( UTF_8 );
  }

  public static String readText( @This File thiz, Charset charset )
  {
    return new String( thiz.readBytes(), charset );
  }

  /**
   * Sets the content of this file as [text] encoded using UTF-8 or specified [charset].
   * If this file exists, it becomes overwritten.
   *
   * @param text    text to write into file.
   * @param charset character set to use.
   */
  public static void writeText( @This File thiz, String text )
  {
    thiz.writeText( text, UTF_8 );
  }

  public static void writeText( @This File thiz, String text, Charset charset )
  {
    thiz.writeBytes( text.getBytes( charset ) );
  }

  /**
   * Appends [text] to the content of this file using UTF-8 or the specified [charset].
   *
   * @param text    text to append to file.
   * @param charset character set to use.
   */
  public static void appendText( @This File thiz, String text )
  {
    thiz.appendText( text, UTF_8 );
  }

  public static void appendText( @This File thiz, String text, Charset charset )
  {
    thiz.appendBytes( text.getBytes( charset ) );
  }

  /**
   * Reads file by byte blocks and calls [action] for each block read.
   * Block has default size which is implementation-dependent.
   * This functions passes the byte array and amount of bytes in the array to the [action] function.
   * <p>
   * You can use this function for huge files.
   *
   * @param action function to process file blocks.
   */
  public static void forEachBlock( @This File thiz, BiConsumer<byte[]/*buffer*/, Integer/*bytesRead*/> action )
  {
    thiz.forEachBlock( DEFAULT_BLOCK_SIZE, action );
  }

  /**
   * Reads file by byte blocks and calls [action] for each block read.
   * This functions passes the byte array and amount of bytes in the array to the [action] function.
   * <p>
   * You can use this function for huge files.
   *
   * @param action    function to process file blocks.
   * @param blockSize size of a block, replaced by 512 if it's less, 4096 by default.
   */
  public static void forEachBlock( @This File thiz, int blockSize, BiConsumer<byte[]/*buffer*/, Integer/*bytesRead*/> action )
  {
    byte[] arr = new byte[blockSize];
    try( FileInputStream fis = new FileInputStream( thiz ) )
    {
      do
      {
        int size = fis.read( arr );
        if( size <= 0 )
        {
          break;
        }
        else
        {
          action.accept( arr, size );
        }
      } while( true );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Reads this file line by line using the specified [charset] and calls [action] for each line.
   * Default charset is UTF-8.
   * <p>
   * You may use this function on huge files.
   *
   * @param charset character set to use.
   * @param action  function to process file lines.
   */
  public static void forEachLine( @This File thiz, Charset charset, Consumer<String/*line*/> action )
  {
    // Note: close is called at forEachLine
    try
    {
      new BufferedReader( new InputStreamReader( new FileInputStream( thiz ), charset ) ).forEachLine( action );
    }
    catch( FileNotFoundException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Constructs a new FileInputStream of this file and returns it as a result.
   */
  public static FileInputStream inputStream( @This File thiz )
  {
    try
    {
      return new FileInputStream( thiz );
    }
    catch( FileNotFoundException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Constructs a new FileOutputStream of this file and returns it as a result.
   */

  public static FileOutputStream outputStream( @This File thiz )
  {
    try
    {
      return new FileOutputStream( thiz );
    }
    catch( FileNotFoundException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Reads the file content as a list of lines.
   * <p>
   * Do not use this function for huge files.
   *
   * @param charset character set to use. By default uses UTF-8 charset.
   *
   * @return list of file lines.
   */
  public static List<String> readLines( @This File thiz, Charset charset )
  {
    ArrayList<String> result = new ArrayList<>();
    thiz.forEachLine( charset, it -> result.add( it ) );
    return result;
  }

  /**
   * Calls the [block] callback giving it a sequence of all the lines in this file and closes the reader once
   * the processing is complete.
   *
   * @param charset character set to use. By default uses UTF-8 charset.
   *
   * @return the value returned by [block].
   */
  public static <T> T useLines( @This File thiz, Charset charset, Function<Iterable<String>, T> block )
  {
    try( BufferedReader it = thiz.bufferedReader( charset, ManFileExt.DEFAULT_BUFFER_SIZE ) )
    {
      return block.apply( it.lineSequence() );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }
}
