package manifold.api.type;

import com.sun.tools.javac.api.BasicJavacTask;

public interface ICompilerComponent
{
  void init( BasicJavacTask javacTask );
}
