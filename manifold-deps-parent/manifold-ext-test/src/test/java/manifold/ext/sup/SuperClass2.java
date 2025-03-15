package manifold.ext.sup;

import java.util.function.Supplier;

public class SuperClass2
{
  public DeferredJoiner foo()
  {
    return new DeferredJoiner(() -> "foo super");
  }

  public static class DeferredJoiner
  {
    private Supplier<String> joiner;

    public DeferredJoiner( Supplier<String> text )
    {
      this.joiner = text;
    }

    public DeferredJoiner add( Supplier<String> text  )
    {
      String s = joiner.get();
      joiner = () -> s + text.get();
      return this;
    }

    public static DeferredJoiner combine( Supplier<DeferredJoiner> other, Supplier<String> text  )
    {
      return other.get().add( text );
    }

    public String get(){
      return joiner.get();
    }
  }
}
