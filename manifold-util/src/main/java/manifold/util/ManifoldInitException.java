package manifold.util;

class ManifoldInitException extends RuntimeException
{
  public Throwable fillInStackTrace()
  {
    return this;
  }
}
