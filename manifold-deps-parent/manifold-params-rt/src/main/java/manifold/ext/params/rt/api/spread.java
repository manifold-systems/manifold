package manifold.ext.params.rt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use {@code spread} to make a method or constructor with optional parameters behave as a set of conventional
 * "telescoping" methods without having to write or maintain them. Applying this annotation suppresses named
 * argument support to instead generate telescoping methods reflecting the optional parameters.
 * <p/>
 * Note, <i>not</i> using {@code spread} with optional parameters still supports {@code spread} functionality,
 * but also supports named arguments, thus another way to describe {@code spread} is to say it turns off named
 * arguments when applied to methods with optional parameters. This is to accommodate those who prefer overrides
 * over named arguments.
 *
 * <pre><code>
 * {@code @spread} Person create(int id, String name, String address = null, String phone = null) {...}
 * </code></pre>
 * Results in the following additional methods.
 * <pre><code>
 *   Person create(int id, String name) { return create(id, name, null, null); }
 *   Person create(int id, String name, String address) { return create(id, name, address, null); }
 * </code></pre>
 * Note, <i>without</i> using {@code @spread} the method may be called as if telescoping methods existed, and
 * may also use named arguments for more options.
 * <pre><code>
 *   Person p = create(100, "Scott");
 *   Person p = create(id:100, name:"Scott", phone:"555-555-5555");
 * </code></pre>
 */
@Retention( RetentionPolicy.SOURCE )
@Target( {ElementType.CONSTRUCTOR, ElementType.METHOD} )
public @interface spread
{
}
