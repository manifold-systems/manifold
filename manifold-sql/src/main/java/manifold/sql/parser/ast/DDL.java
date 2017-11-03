package manifold.sql.parser.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pjennings on 6/23/2015.
 */
public class DDL extends SQL {
  private List<CreateTable> statements;
  public DDL(){
    statements = new ArrayList<CreateTable>();
  }
  public void append(CreateTable t){
    statements.add(t);
  }
  public List<CreateTable> getList(){
    return statements;
  }

}
