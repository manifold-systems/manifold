package manifold.templates.runtime;

import java.io.IOException;
import manifold.util.ManExceptionUtil;

/**
 * Wraps calls to {@link Appendable} to handle {@link IOException}s that otherwise
 * are tedious to handle inside lambdas.
 */
@SuppressWarnings("unused")
public class WrapAppendable implements Appendable
{
  private final Appendable _appendable;

  public WrapAppendable( Appendable appendable )
  {
    _appendable = appendable;
  }

  @Override
  public Appendable append( CharSequence csq )
  {
    try
    {
      return _appendable.append( csq );
    }
    catch( IOException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @Override
  public Appendable append( CharSequence csq, int start, int end )
  {
    try
    {
      return _appendable.append( csq, start, end );
    }
    catch( IOException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @Override
  public Appendable append( char c )
  {
    try
    {
      return _appendable.append( c );
    }
    catch( IOException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }
}
