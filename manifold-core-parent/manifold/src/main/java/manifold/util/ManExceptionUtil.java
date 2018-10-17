package manifold.util;

public class ManExceptionUtil
{
  /**
   * Throws an unchecked exception without having to declare or catch it.
   *
   * @param t Any exception
   * @return The {@link RuntimeException} return type is here so you can do this:<br>
   * {@code throw ManExceptionUtil.uncheck(new SomeException())}
   */
  public static RuntimeException unchecked( Throwable t )
  {
    _unchecked( t );

    // above statement always throws, this is unreachable
    throw new IllegalStateException();
  }

  private static <T extends Throwable> void _unchecked( Throwable t ) throws T
  {
    //noinspection unchecked
    throw (T)t;
  }
}
