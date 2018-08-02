package manifold.util.testClasses;

class TestClass1
{
  private static final String STATIC_FINAL_STRING = value();

  private final String FINAL_STRING = value();

  private TestClass1() {}

  private static String value()
  {
    return "hi";
  }
}
