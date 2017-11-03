package manifold.sql.parser.ast;

/**
 * Created by klu on 7/27/2015.
 */
public class DateTimeTerm extends Term{
  private String type;
  private String text;

  public DateTimeTerm(String type, String text){
    this.type = type;
    this.text = text;
  }

  @Override
  public String toString() {
    return "<Term>" + type.toUpperCase() + " '" + text + "'\n";
  }

  @Override
  public void setNegative(boolean isNeg) {}

  @Override
  protected String toString(String initial) {
    return initial + "<Term>" + type.toUpperCase() + " '" + text + "'\n";
  }
}
