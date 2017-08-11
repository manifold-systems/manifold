package msql.parser.ast;

/**
 * Created by klu on 6/25/2015.
 */
public class ResultColumn {
  private String _name;

  public ResultColumn(String n){
    _name = n;
  }

  public String getName(){
    return _name;
  }

  public void setName(String n){
    _name = n;
  }

  @Override
  public String toString(){
    return "<Result Column>" + _name + "\n";
  }

  protected String toString(String initial){
    return initial+"<Result Column>" + _name + "\n";
  }

}
