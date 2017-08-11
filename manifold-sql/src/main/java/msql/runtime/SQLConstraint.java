package msql.runtime;

import msql.api.IColumnEnum;
import msql.api.ISQLMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class SQLConstraint
{

  IColumnEnum _columnInfo;

  public static SQLConstraint isComparator( IColumnEnum pr, Object o, String s )
  {
    return new ComparatorConstraint( pr, o , s );
  }

  public static SQLConstraint isIn( IColumnEnum pr, List<Object> l )
  {
    return new IsInConstraint( pr, l );
  }

  public static SQLConstraint isIn( IColumnEnum pr, SQLQuery<Object> s ) {
    return new IsInConstraint(pr, s);
  }

  public static SQLConstraint isLike( IColumnEnum pr, String s )
  {
    return new IsLikeConstraint( pr, s );
  }

  public static SQLConstraint join( Class clazz , String joinName )
  {
    try {
      ISQLMetadata metadata = (ISQLMetadata) clazz.getField("METADATA").get(null);
      return new JoinConstraint( metadata, joinName );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static SQLConstraint on( SQLConstraint s) {
    return new OnConstraint(s);
  }

  protected static SQLConstraint direction( String s, IColumnEnum p) {
    return new DirectionConstraint(s,p);
  }

  protected static SQLConstraint limit( int i) {
    return new LimitConstraint(i);
  }

  protected static SQLConstraint offset( int i) {
    return new OffsetConstraint(i);
  }

  protected static SQLConstraint orderBy( SQLConstraint ... constraints) {
    return new OrderByConstraint(constraints);
  }

  public static SQLConstraint raw( String sql, List<Object> args) {
    return new RawConstraint(sql,args);
  }

  public static SQLConstraint isNull(IColumnEnum p) {
  return new RawConstraint(" " + p.getName() + " IS NULL",new ArrayList<>());
}

  public static SQLConstraint isNotNull(IColumnEnum p) {
    return new RawConstraint(" " + p.getName() + " IS NOT NULL",new ArrayList<>());
  }



  abstract String getSQL( ISQLMetadata metadata );

  abstract List<Object> getArgs();

  public SQLConstraint addOn(SQLConstraint sql){
    return new CombinedConstraint( this, sql);
  }



  public SQLConstraint andAlso(SQLConstraint sql){
    return new AndConstraint(_columnInfo, this, sql);
  }

  public SQLConstraint orElse(SQLConstraint sql){
    return new OrConstraint(_columnInfo, this, sql);
  }

  public static SQLConstraint not(SQLConstraint sql){
    return new NotConstraint(sql);
  }


  private static class CombinedConstraint extends SQLConstraint
  {
    SQLConstraint c1;
    SQLConstraint c2;

    CombinedConstraint( SQLConstraint _c1 , SQLConstraint _c2)
    {
      c1 = _c1;
      c2 = _c2;
    }


    public String getSQL( ISQLMetadata metadata  )
    {
      return c1.getSQL(metadata) + " " + c2.getSQL(metadata);
    }

    List<Object> getArgs()
    {
      List answer = new ArrayList();
      answer.addAll(c1.getArgs());
      answer.addAll(c2.getArgs());
      return answer;
    }
  }

  private static class DirectionConstraint extends SQLConstraint
  {
    String direction;
    IColumnEnum prop;

    DirectionConstraint( String _direction, IColumnEnum _prop )
    {
      direction = _direction;
      prop = _prop;
    }


    public String getSQL( ISQLMetadata metadata )
    {
      return metadata.getColumnForProperty(prop) + " " + direction;
    }

    List<Object> getArgs()
    {
      return new ArrayList<>();
    }
  }

  private static class AddConstraint extends SQLConstraint
  {
    SQLConstraint constraint;
    String prepend;
    String append;

    AddConstraint( String _prepend, SQLConstraint _constraint, String _append )
    {
      constraint = _constraint;
      prepend = _prepend;
      append = _append;
    }


    public String getSQL( ISQLMetadata metadata  )
    {
      return prepend + constraint.getSQL(metadata) + append;
    }

    List<Object> getArgs()
    {
      return constraint.getArgs();
    }
  }

  private static class OrderByConstraint extends SQLConstraint
  {
    SQLConstraint[] constraints;
    SQLConstraint constraint1;


    OrderByConstraint( SQLConstraint ... _constraints  )
    {
      constraints = _constraints;
    }


    public String getSQL( ISQLMetadata metadata  )
    {
      boolean isFirstTime = true;
      String ans = "ORDER BY ";
      for(SQLConstraint cons : constraints){
        if(isFirstTime){
          ans += cons.getSQL(metadata);
          isFirstTime = false;
        }
        else{
          ans += " , " + cons.getSQL(metadata);
        }
      }
      return  ans;
    }

    List<Object> getArgs()
    {
      List answer = new ArrayList();
      for(SQLConstraint cons : constraints){
        answer.addAll(cons.getArgs());
      }
      return answer;
    }
  }





  private static class JoinConstraint extends SQLConstraint
  {
    String _obj;
    String _joinType;

    JoinConstraint(ISQLMetadata o , String joinType)
    {
      _obj = o.getTableName();
      _joinType = joinType;
    }


    public String getSQL( ISQLMetadata metadata  )
    {
      return " " + _joinType + " ( " + _obj
         + " ) ";
    }

    List<Object> getArgs()
    {
      return Collections.emptyList();
    }
  }

  private static class AndConstraint extends SQLConstraint
  {

    SQLConstraint constraint1;
    SQLConstraint constraint2;
    String result;

    AndConstraint( IColumnEnum pi, SQLConstraint _constraint1, SQLConstraint _constraint2 )
    {
      _columnInfo = pi;
      constraint1 = _constraint1;
      constraint2 = _constraint2;
    }

    public String getSQL( ISQLMetadata metadata ){
      result = " ( " +   constraint1.getSQL(metadata)  + " AND " +  constraint2.getSQL(metadata) + " ) ";
      return result;
    }

    List<Object> getArgs()
    {
      List answer = new ArrayList();
      answer.addAll(constraint1.getArgs());
      answer.addAll(constraint2.getArgs());
      return answer;
    }
  }

  private static class OnConstraint extends SQLConstraint
  {

    SQLConstraint constraint1;


    OnConstraint( SQLConstraint _constraint1 )
    {
      constraint1 = _constraint1;
    }

    public String getSQL( ISQLMetadata metadata ){

      return " ON " + constraint1.getSQL( metadata);
    }

    List<Object> getArgs()
    {
      return constraint1.getArgs();
    }
  }

  private static class RawConstraint extends SQLConstraint
  {

    String _sql;
    List<Object> _args;


    RawConstraint( String sql, List<Object> args )
    {

      _sql = sql;
      _args = args;
    }

    public String getSQL( ISQLMetadata metadata ){

      return _sql;
    }

    List<Object> getArgs()
    {
      return _args;
    }
  }

  private static class LimitConstraint extends SQLConstraint
  {

    int limit;


    LimitConstraint( int _limit )
    {
      limit = _limit;
    }

    public String getSQL( ISQLMetadata metadata ){

      return new StringBuilder().append(" LIMIT ").append(limit).append(" ").toString();
    }

    List<Object> getArgs()
    {
      return new ArrayList<>();
    }
  }

  private static class OffsetConstraint extends SQLConstraint
  {

    int limit;


    OffsetConstraint( int _limit )
    {
      limit = _limit;
    }

    public String getSQL( ISQLMetadata metadata ){
      return new StringBuilder().append(" OFFSET ").append(limit).append(" ").toString();
    }

    List<Object> getArgs()
    {
      return new ArrayList<>();
    }
  }

  private static class OrConstraint extends SQLConstraint
  {

    SQLConstraint constraint1;
    SQLConstraint constraint2;
    String result;

    OrConstraint( IColumnEnum pi, SQLConstraint _constraint1, SQLConstraint _constraint2 )
    {
      _columnInfo = pi;
      constraint1 = _constraint1;
      constraint2 = _constraint2;
    }

    public String getSQL( ISQLMetadata metadata ){
      result = " ( " +   constraint1.getSQL(metadata)  + " OR " +  constraint2.getSQL(metadata) + " ) ";
      return result;
    }

    List<Object> getArgs()
    {
      List answer = new ArrayList();
      answer.addAll(constraint1.getArgs());
      answer.addAll(constraint2.getArgs());
      return answer;
    }
  }


  private static class NotConstraint extends SQLConstraint
  {


    private SQLConstraint _original;

    NotConstraint(SQLConstraint original)
    {
      _original = original;
    }

    public String getSQL( ISQLMetadata metadata )
    {
      return " NOT " + _original.getSQL(metadata);
    }

    List<Object> getArgs()
    {
      return _original.getArgs();
    }
  }

  private static class ComparatorConstraint extends SQLConstraint
  {
    List<Object> _objs;
    String RHS;
    String _comparator;

    ComparatorConstraint( IColumnEnum pi, Object o, String comparator )
    {
      _columnInfo = pi;
      _comparator = comparator;
      if( o instanceof IColumnEnum){
        IColumnEnum info = (IColumnEnum) o;
        RHS = info.getTableName() + "." + info.getName();
        _objs = new ArrayList<>();
      } else {
        _objs = Arrays.asList( o );
        RHS = " ? ";
      }
    }

    public String getSQL( ISQLMetadata metadata )
    {
      return metadata.getColumnForProperty(_columnInfo) + _comparator + RHS;
    }

    List<Object> getArgs()
    {
      return _objs;
    }
  }

  private static class IsInConstraint extends SQLConstraint
  {
    List _list;
    SQLQuery _query;

    IsInConstraint( IColumnEnum pi, List list )
    {
      _columnInfo = pi;
      _list = list;
    }

    IsInConstraint( IColumnEnum pi, SQLQuery query )
    {
      _columnInfo = pi;
      _list = query.getArgs();
      _query = query;

    }

    public String getSQL( ISQLMetadata metadata )
    {
      if(_query!=null){
        return  metadata.getColumnForProperty(_columnInfo)
        + " IN (" + _query.getSQLString() + ") ";
      }
      else {
        String ans = "";
        ans += metadata.getColumnForProperty(_columnInfo);
        ans += " IN (";
        if (_list.size() != 0) {
          for (int x = 0; x < _list.size() - 1; x++) {
            ans += " ? ,";
          }
          ans += " ? ";
        }
        ans += " ) ";
        return ans;
      }
    }

    List<Object> getArgs()
    {
      return _list;
    }
  }

  private static class IsLikeConstraint extends SQLConstraint
  {
    String _str;

    IsLikeConstraint( IColumnEnum pi, String str )
    {
      _columnInfo = pi;
      _str = str;
    }

    public String getSQL( ISQLMetadata metadata )
    {
      return (metadata.getColumnForProperty(_columnInfo) + " LIKE ?");
    }

    List<Object> getArgs()
    {
      return Arrays.asList( _str );
    }
  }
}
