# What if adjacency were a *binary operator*?
                                                  
What if this were a real, compile time type-safe expression:
```java
2025 July 19   // → LocalDate 
```

That’s the idea behind *binding expressions* -- a compiler plugin for Java that explores what it would be like if adjacency were a binary operator. In a nutshell, it lets adjacent expressions bind based on their static types, to form a new expression.

---

## Type-directed expression binding

With binding expressions, adjacency is used as a syntactic trigger for a process called *expression binding*, where adjacent expressions are resolved through methods defined on their types.

Here are some examples of binding expressions in Java with the Manifold compiler plugin:

```java
2025 July 19        // → LocalDate
299.8M m/s          // → Velocity
1 to 10             // → Range<Integer>
Meet Alice Tuesday at 3pm  // → CalendarEvent
```

A pair of adjacent expressions is a candidate for binding. If the LHS type defines:

```java
<R> LR prefixBind(R right);
```

...or the RHS type defines:

```java
<L> RL postfixBind(L left);
```

...then the compiler applies the appropriate binding. These bindings *nest and compose*, and the compiler attempts to reduce the entire series of expressions into a single, type-safe expression.

---

## Example: LocalDates as composable expressions

Consider the expression:

```java
LocalDate date = 2025 July 19;
```

The compiler reduces this expression by evaluating adjacent pairs. Let’s say `July` is an enum:

```java
public enum Month {
  January, February, March, /* ... */

  public LocalMonthDay prefixBind(Integer day) {
    return new LocalMonthDay(this, day);
  }

  public LocalYearMonth postfixBind(Integer year) {
    return new LocalYearMonth(this, year);
  }
}
```

Now suppose `LocalMonthDay` defines:

```java
public LocalDate postfixBind(Integer year) {
  return LocalDate.of(year, this.month, this.day);
}
```

The expression reduces like this:
```java
2025 July 19
⇒ July.postfixBind(2025) // → LocalYearMonth
⇒ [retreat]              // → error: No binding with `19`
⇒ July.prefixBind(19)    // → LocalMonthDay
⇒ .postfixBind(2025)     // → LocalDate
```
Although the reduction algorithm favors left-to-right binding, it systematically **retreats** from failed paths and continues exploring alternative reductions until a valid one is found. This isn’t parser-style backtracking — instead, it's a structured search that reduces adjacent operand pairs using available binding methods. In this case, the initial attempt to bind `2025 July` succeeds, but the resulting intermediate expression cannot bind with `19`, forcing the algorithm to retreat and try a different reduction. Binding `July 19` succeeds, yielding a `LocalMonthDay`, which can then bind with `2025` to produce a `LocalDate`.

---

## Why bother?

Binding expressions give you a *type-safe* and *non-invasive* way to define DSLs or literal grammars directly in Java, without modifying base types or introducing macros.

Going back to the date example:

```java
LocalDate date = 2025 July 19;
```

The `Integer` type (`2025`) doesn’t need to know anything about `LocalMonthDay` or `LocalDate`. Instead, the logic lives in the `Month` and `LocalMonthDay` types via `pre/postfixBind` methods. This keeps your *core types clean* and allows you to add domain-specific semantics via *adjacent types*.

You can build:

* Unit systems (e.g., `299.8M m/s`)
* Natural-language DSLs
* Domain-specific literal syntax (e.g., currencies, time spans, ranges)

All of these are possible with static type safety and zero runtime magic.
                                                                                                                
---

## Experimental usage

The Manifold project makes interesting use of binding expressions. Here are some examples:

* **Science:** The [manifold-science](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-science/README.md) library implements units using [binding expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions) and [arithmetic & relational operators](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
  across the full spectrum of SI quantities, providing strong type safety, clearer code, and prevention of unit-related errors.

* **Ranges:** The [Range API](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections#ranges) uses binding expressions with binding constants like `to`, enabling more natural representations of ranges and sequences.

* **Vectors:** Experimental vector classes in the `manifold.science.vector` package support vector math directly within expressions, e.g., `1.2m E + 5.7m NW`.

**Tooling note:** The [IntelliJ plugin](https://github.com/manifold-systems/manifold) for Manifold supports binding expressions natively, with live feedback and resolution as you type.

---

## Downsides

Binding expressions are powerful and flexible, but there are trade-offs to consider:

* *Parsing complexity:* Adjacency is a two-stage parsing problem. The initial, untyped stage parses with static precedence rules. Because binding is type-directed, expression grouping isn't fully resolved until attribution. The algorithm for solving a binding series is nontrivial.

* *Flexibility vs. discipline:* Allowing types to define how adjacent values compose shifts the boundary between syntax and semantics in a way that may feel a little unsafe. The key distinction here is that binding expressions are grounded in static types -- the compiler decides what can bind based on concrete, declared rules. But yes, in the wrong hands, it could get a bit sporty.

* *Cognitive overhead:* While binding expressions can produce more natural, readable syntax, combining them with a conventional programming language can initially cause confusion -- much like when lambdas were first introduced to Java. They challenged familiar patterns, but eventually settled in.

---

## Still Experimental

Binding expressions have been part of Manifold for several years, but they remain somewhat experimental. There’s still room to grow. For example, compile-time formatting rules could verify compile-time constant expressions, such as validating that `July 19` is a real date in `2025`. Future improvements might include support for separators and punctuation, binding *statements*, specialization of the reduction algorithm, and more.

If you're curious, you can explore the implementation in the [Manifold repo](https://github.com/manifold-systems/manifold).
