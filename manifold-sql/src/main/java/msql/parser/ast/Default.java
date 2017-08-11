package msql.parser.ast;

/**
 * Created by klu on 7/7/2015.
 */
public class Default  extends Expression{
  /*
  * Literally the only purpose of this class is to contain cases where 'DEFAULT' is used in lieu of an expression
  **/

  @Override
  protected String toString(String initial){
    return initial+"\t<DEFAULT>\n";
  }

}
