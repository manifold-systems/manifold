package msql.parser.ast;

/**
 * Created by klu on 6/25/2015.
 */
public class VariableTerm extends Term{
  private JavaVar variable;

  public VariableTerm(){variable = null;}

  public VariableTerm(JavaVar v){variable = v;}

  public void setNegative(boolean b){}

  public String toString(){
    return("<Term> @"+variable.getVarName()+"\n");
  }

  protected String toString(String initial){
    return(initial+"<Term> @"+variable.getVarName()+"\n");
  }

  public String getName(){return variable.getVarName();}

  public String getType(){return variable.getVarType();}
}
