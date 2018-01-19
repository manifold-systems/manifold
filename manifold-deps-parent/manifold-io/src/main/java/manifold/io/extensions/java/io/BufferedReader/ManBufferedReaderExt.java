package manifold.io.extensions.java.io.BufferedReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

/**
 */
@Extension
public class ManBufferedReaderExt
{
  /**
   * Returns a sequence of corresponding file lines.
   * <p>
   * *Note*: the caller must close the underlying `BufferedReader` when the iteration is finished
   *
   * @return a sequence of corresponding file lines. The sequence returned can be iterated only once.
   */
  public static Iterable<String> lineSequence( @This BufferedReader thiz )
  {
    return new LinesSequence( thiz );
  }

  private static class LinesSequence implements Iterable<String>
  {
    private final BufferedReader _reader;

    LinesSequence( BufferedReader reader )
    {
      _reader = reader;
    }

    public Iterator<String> iterator()
    {
      return new Iterator<String>()
      {
        private String nextValue;
        private boolean done;

        public boolean hasNext()
        {
          if( nextValue == null && !done )
          {
            try
            {
              nextValue = _reader.readLine();
            }
            catch( IOException e )
            {
              throw new RuntimeException( e );
            }
            if( nextValue == null )
            {
              done = true;
            }
          }
          return nextValue != null;
        }

        public String next()
        {
          if( !hasNext() )
          {
            throw new NoSuchElementException();
          }
          String answer = nextValue;
          nextValue = null;
          return answer;
        }
      };
    }
  }

}
