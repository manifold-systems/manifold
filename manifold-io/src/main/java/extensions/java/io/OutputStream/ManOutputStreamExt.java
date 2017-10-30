package extensions.java.io.OutputStream;

import extensions.java.io.File.ManFileExt;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import manifold.ext.api.Extension;
import manifold.ext.api.This;


import static java.nio.charset.StandardCharsets.UTF_8;

/**
 */
@Extension
public class ManOutputStreamExt
{
  /**
   * Creates a buffered output stream wrapping this stream.
   */
  public static BufferedOutputStream buffered( @This OutputStream thiz )
  {
    return thiz.buffered( ManFileExt.DEFAULT_BUFFER_SIZE );
  }
  /**
   * Creates a buffered output stream wrapping this stream.
   * @param bufferSize the buffer size to use.
   */
  public static BufferedOutputStream buffered( @This OutputStream thiz, int bufferSize )
  {
    return thiz instanceof BufferedOutputStream
           ? (BufferedOutputStream)thiz
           : new BufferedOutputStream( thiz, bufferSize );
  }

  /** Creates a writer on this output stream using UTF-8 or the specified [charset]. */
  public static OutputStreamWriter writer( @This OutputStream thiz )
  {
    return thiz.writer( UTF_8 );
  }
  public static OutputStreamWriter writer( @This OutputStream thiz, Charset charset )
  {
    return new OutputStreamWriter( thiz, charset );
  }

  /** Creates a buffered writer on this output stream using UTF-8 or the specified [charset]. */
  public static BufferedWriter bufferedWriter( @This OutputStream thiz )
  {
    return thiz.bufferedWriter( UTF_8 );
  }
  public static BufferedWriter bufferedWriter( @This OutputStream thiz, Charset charset )
  {
    return thiz.writer( charset ).buffered();
  }

}
