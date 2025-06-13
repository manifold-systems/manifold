package abc;

import java.io.IOException;

public class ComplexGenerics
{
  // this is the real test -- make sure the intersection type is retained in the stubbed class
  public <T extends Runnable & Appendable> T addWidget( T arg) {
    return (T)arg;
  }

  public static class Widge implements Runnable, Appendable {

    @Override
    public Appendable append( CharSequence csq ) throws IOException
    {
      return null;
    }

    @Override
    public Appendable append( CharSequence csq, int start, int end ) throws IOException
    {
      return null;
    }

    @Override
    public Appendable append( char c ) throws IOException
    {
      return null;
    }

    @Override
    public void run()
    {

    }
  }
}
