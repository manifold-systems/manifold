package manifold.util;

import java.util.Objects;

/**
 */
@FunctionalInterface
public interface IndexedPredicate<T>
{
  /**
   * Evaluates this predicate on the given argument.
   *
   * @param index the index of [t]
   * @param t     the input argument
   *
   * @return {@code true} if the input argument matches the predicate,
   * otherwise {@code false}
   */
  boolean test( int index, T t );

  /**
   * Returns a composed predicate that represents a short-circuiting logical
   * AND of this predicate and another.  When evaluating the composed
   * predicate, if this predicate is {@code false}, then the {@code other}
   * predicate is not evaluated.
   * <p>
   * <p>Any exceptions thrown during evaluation of either predicate are relayed
   * to the caller; if evaluation of this predicate throws an exception, the
   * {@code other} predicate will not be evaluated.
   *
   * @param other a predicate that will be logically-ANDed with this
   *              predicate
   *
   * @return a composed predicate that represents the short-circuiting logical
   * AND of this predicate and the {@code other} predicate
   *
   * @throws NullPointerException if other is null
   */
  default IndexedPredicate<T> and( IndexedPredicate<? super T> other )
  {
    Objects.requireNonNull( other );
    return ( index, t ) -> test( index, t ) && other.test( index, t );
  }

  /**
   * Returns a predicate that represents the logical negation of this
   * predicate.
   *
   * @return a predicate that represents the logical negation of this
   * predicate
   */
  default IndexedPredicate<T> negate()
  {
    return ( index, t ) -> !test( index, t );
  }

  /**
   * Returns a composed predicate that represents a short-circuiting logical
   * OR of this predicate and another.  When evaluating the composed
   * predicate, if this predicate is {@code true}, then the {@code other}
   * predicate is not evaluated.
   * <p>
   * <p>Any exceptions thrown during evaluation of either predicate are relayed
   * to the caller; if evaluation of this predicate throws an exception, the
   * {@code other} predicate will not be evaluated.
   *
   * @param other a predicate that will be logically-ORed with this
   *              predicate
   *
   * @return a composed predicate that represents the short-circuiting logical
   * OR of this predicate and the {@code other} predicate
   *
   * @throws NullPointerException if other is null
   */
  default IndexedPredicate<T> or( IndexedPredicate<? super T> other )
  {
    Objects.requireNonNull( other );
    return ( index, t ) -> test( index, t ) || other.test( index, t );
  }
}
