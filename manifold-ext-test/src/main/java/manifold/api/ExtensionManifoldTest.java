package manifold.api;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 */
public abstract class ExtensionManifoldTest extends TestCase {
  /**
   * Subclasses must override for coverage.  Delegate to testCoverage( Class ):
   * <pre>
   *   public void testCoverage() {
   *     testCoverage( MyExtensionClass.class );
   *   }
   * </pre>
   */
  public abstract void testCoverage();

  protected void testCoverage( Class extensionClass ) {
    Method[] methods = extensionClass.getMethods();
    ArrayList<String> untested = new ArrayList<>();
    for (Method m : methods) {
      if (Modifier.isStatic(m.getModifiers()) && !Modifier.isPrivate(m.getModifiers())) {
        try {
          String name = m.getName();
          getClass().getMethod("test" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
        } catch (Exception e) {
          String fullDesc = m.toString();
          untested.add(new StringBuilder("Untested: ").append(fullDesc.substring(fullDesc.indexOf('.' + m.getName() + '(') + 1)).toString());
        }
      }
    }
    if (!untested.isEmpty()) {
      StringBuilder msg = new StringBuilder();
      untested.stream().sorted().forEach(e -> msg.append(e).append("\n"));
      msg.append("Expecting at least one test method per extension method of the form: testXxx().\n");
      TestCase.fail( msg.toString());
    }
  }
}
