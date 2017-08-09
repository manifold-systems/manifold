package extensions.java.io.Writer;

import java.io.BufferedWriter;
import java.io.Writer;
import manifold.ext.api.Extension;
import manifold.ext.api.This;


import static extensions.java.io.File.ManFileExt.DEFAULT_BUFFER_SIZE;

/**
 */
@Extension
public class ManWriterExt
{
  /** Returns a buffered reader wrapping this Writer, or this Writer itself if it is already buffered. */
  public static BufferedWriter buffered( @This Writer thiz )
  {
    return thiz.buffered( DEFAULT_BUFFER_SIZE );
  }
  public static BufferedWriter buffered( @This Writer thiz, int bufferSize )
  {
    return thiz instanceof BufferedWriter ? (BufferedWriter)thiz : new BufferedWriter( thiz, bufferSize );
  }
}
