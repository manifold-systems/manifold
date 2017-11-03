package manifold.sql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 7/15/2015.
 */
public abstract class Statement extends SQL{
  public abstract ArrayList<ResultColumn> getResultColumns();

  public abstract ArrayList<JavaVar> getVariables();

  public abstract ArrayList<String> getTables();

}
