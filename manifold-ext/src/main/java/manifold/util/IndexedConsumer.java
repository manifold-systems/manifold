package manifold.util;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Indexed version of Consumer
 * @see Consumer
 */
@FunctionalInterface
public interface IndexedConsumer<T>
{
  /**
   * Performs this operation on the given argument with the given index.
   *
   * @param index the index of [t]
   * @param t the input argument
   */
  void accept( int index, T t );

  /**
   * Returns a composed {@code Consumer} that performs, in sequence, this
   * operation followed by the {@code after} operation. If performing either
   * operation throws an exception, it is relayed to the caller of the
   * composed operation.  If performing this operation throws an exception,
   * the {@code after} operation will not be performed.
   *
   * @param after the operation to perform after this operation
   *
   * @return a composed {@code Consumer} that performs in sequence this
   * operation followed by the {@code after} operation
   *
   * @throws NullPointerException if {@code after} is null
   */
  default IndexedConsumer<T> andThen( IndexedConsumer<? super T> after )
  {
    Objects.requireNonNull( after );
    return ( int index, T t ) ->
    {
      accept( index, t );
      after.accept( index, t );
    };
  }

}
