package manifold.ext.delegation.rt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface in an implements clause or an interface method as {@code internal} to a composition graph.
 * <p>
 * {@code @internal} is the {@code protected} modifier for the world of composition.
 * It provides the same encapsulation benefits as {@code protected} but without
 * the legacy "leakiness" of {@code package-private} access.
 *
 * <h4>Usage on Interface Types (Implements Clause)</h4>
 * When applied to an interface in a delegate class's {@code implements} clause,
 * it prevents that interface from being "inherited" by delegators. The interface becomes
 * a private capability of the delegate, often for internal implementation details.
 * <pre><code>
 * class FooPart implements Foo, @internal Bar { ... }
 *
 * class MyRoot implements Foo, Bar {
 *   // ERROR: Bar is internal to FooPart. It cannot be delegated to MyRoot.
 *   {@literal @}link Bar bar = new FooPart();
 *
 *   // OK: Foo is not internal to FooPart.
 *   {@literal @}link Foo foo = new FooPart();
 *
 *   // OK: Bar must be provided by another delegate or implemented directly.
 *   {@literal @}link Bar bar = new BarPart();
 * }
 * </code></pre>
 *
 * <h4>Usage on Methods</h4>
 * When applied to an interface method, it restricts access to the compositional scope. The method is part of the internal
 * contract shared between a host and its delegates, but is hidden from external consumers. It is visible only to:
 * <ul>
 *   <li><b>The Interface:</b> Default methods within the same interface.</li>
 *   <li><b>Implementors:</b> Any class that implements the interface can override or call the method. Calls are limited
 *   to "self" calls: calls that dereference {@code this} or the {@code @link} field that provides the interface implementation.</li>
 * </ul>
 *
 * <h4>Additionally:</h4>
 * <ul>
 *   <li><b>Inherited:</b> Implementors automatically inherit {@code @internal} status for
 *       overridden methods; it does not need to be reapplied.</li>
 *   <li><b>Compiler Enforced:</b> Manifold produces compile errors for {@code @internal} access violations.
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.METHOD})
public @interface internal
{
}
