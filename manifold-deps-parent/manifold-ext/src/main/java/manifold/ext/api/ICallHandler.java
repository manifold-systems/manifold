package manifold.ext.api;

import manifold.ext.extensions.java.util.Map.MapStructExt;

/**
 * Facilitates dynamic interface method invocation.
 * <p/>
 * A class can directly or indirectly, via interface extension, implement this
 * interface to support dynamic interface method invocation.  Any class implementing
 * this interface can be cast to <i>any</i> interface.  Manifold cooperates
 * with the Java compiler to transform calls through an interface to
 * ICallHandler.call().
 * <p/>
 * Note unlike a proxy or wrapper a class implementing ICallHandler
 * doesn't wrap anything, therefore it doesn't lose its identity in the process
 * of making calls.
 *
 * @see MapStructExt
 */
@Structural
public interface ICallHandler
{
  /**
   * A value resulting from #call() indicating the call could not be dispatched.
   */
  Object UNHANDLED = new Object()
  {
    @Override
    public String toString()
    {
      return "Unhandled";
    }
  };

  /**
   * Dispatch a call to an interface method.
   *
   * @param iface The extended interface and owner of the method
   * @param name The name of the method
   * @param actualName The actual name of the property associated with the method e.g., a Json name that is not a legal Java identifier, can be null
   * @param returnType The return type of the method
   * @param paramTypes The parameter types of the method
   * @param args The arguments from the call site
   * @return The result of the method call or UNHANDLED if the method is not dispatched.  Null if the method's return type is void.
   */
  Object call( Class iface, String name, String actualName, Class returnType, Class[] paramTypes, Object[] args );
}
