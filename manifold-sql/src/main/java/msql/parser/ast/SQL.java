package msql.parser.ast;

/**
 * Created by klu on 6/25/2015.
 */
public abstract class SQL {
  private int errCount;

  public void setErrCount(int e){
    errCount = e;
  }

  public int getErrCount(){
    return errCount;
  }
}
