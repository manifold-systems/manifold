package manifold.templates.runtime;

import java.io.IOException;

public interface ILayout
{
  ILayout EMPTY = new ILayout()
  {
    @Override
    public void header( Appendable buffer )
    {
    }

    @Override
    public void footer( Appendable buffer )
    {
    }
  };

  void header( Appendable buffer ) throws IOException;

  void footer( Appendable buffer ) throws IOException;
}
