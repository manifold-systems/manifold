package msql.runtime;

import msql.api.IColumnEnum;
import msql.api.ISQLMetadata;
import msql.util.DBSupport;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by carson on 7/1/15.
 */
public class SQLQuery<T> implements Iterable<T> {

  protected ISQLMetadata _metadata;
  
  protected Class _rootType;
  protected Class _replaceType;
  
  private String _manualSelect;
  private SQLConstraint _whereExpr;
  private SQLConstraint _joinExpr; // Includes On expressions as well!
  private String _groupBy;
  private SQLConstraint _orderByExpr;
  private SQLConstraint _limitExpr;
  private SQLConstraint _offsetExpr;
  private IColumnEnum _pick;

  private void setManualSelect(String manualSelect){
    _manualSelect = manualSelect;
  }

  protected void setGroupBy(String str){
    _groupBy = str;
  }

  protected void setType(Class type){
    _replaceType = type;
  }

  private void addJoin(SQLConstraint cons){
    if(_joinExpr == null){
      _joinExpr = cons;
    }
    else{
      _joinExpr = _joinExpr.addOn(cons);
    }
  }

  public SQLQuery(ISQLMetadata md, Class rootType )
  {
    _metadata = md;
    _rootType = rootType;
  }

  public SQLQuery<T> where(SQLConstraint constraint) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery._whereExpr = (this._whereExpr == null) ? constraint : this._whereExpr.andAlso( constraint );
    return newQuery;
  }

  public SQLQuery<T> crossJoin( Class type) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.addJoin(SQLConstraint.join(type, "CROSS JOIN"));
    return newQuery;
  }

  public SQLQuery<T> join( Class type) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.addJoin(SQLConstraint.join(type, "JOIN"));
    return newQuery;
  }

  public SQLQuery<T> innerJoin( Class type) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.addJoin(SQLConstraint.join(type, "INNER JOIN"));
    return newQuery;
  }

  public SQLQuery<T> leftOuterJoin( Class type) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.addJoin(SQLConstraint.join(type, "LEFT OUTER JOIN"));
    return newQuery;
  }

  public SQLQuery<T> rightOuterJoin( Class type) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.addJoin(SQLConstraint.join(type, "RIGHT OUTER JOIN"));
    return newQuery;
  }

  public SQLQuery<T> rightJoin( Class type) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.addJoin(SQLConstraint.join(type, "RIGHT JOIN"));
    return newQuery;
  }

  public SQLQuery<T> leftJoin( Class type) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.addJoin(SQLConstraint.join(type, "LEFT JOIN"));
    return newQuery;
  }

  public SQLQuery<T> naturalJoin( Class type) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.addJoin(SQLConstraint.join(type, "NATURAL JOIN"));
    return newQuery;
  }


  public SQLQuery<T> on( SQLConstraint constraint) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.addJoin(SQLConstraint.on(constraint));
    return newQuery;
  }

  public SQLQuery<T> orderBy(SQLConstraint ... constraints){
    SQLQuery<T> newQuery = cloneMe();
    newQuery._orderByExpr = SQLConstraint.orderBy(constraints);
    return newQuery;
  }

  public SQLQuery<T> limit(int i){
    SQLQuery<T> newQuery = cloneMe();
    newQuery._limitExpr = SQLConstraint.limit(i);
    return newQuery;
  }

  public SQLQuery<T> offset(int i){
    SQLQuery<T> newQuery = cloneMe();
    newQuery._offsetExpr = SQLConstraint.offset(i);
    return newQuery;
  }

  public SQLQuery<T> union( SQLQuery query) {
    return new SetOpQuery(this, query, " UNION " , _metadata , _rootType);
  }

  public SQLQuery<T> unionAll( SQLQuery query) {
    return new SetOpQuery(this, query, " UNION ALL " , _metadata , _rootType);
  }

  public SQLQuery<T> intersect( SQLQuery query) {
    return new SetOpQuery(this, query, " INTERSECT " , _metadata , _rootType);
  }

  public SQLQuery<T> except( SQLQuery query) {
    return new SetOpQuery(this, query, " EXCEPT " , _metadata , _rootType);
  }

  public boolean delete() throws SQLException
  {
    String from = "DELETE FROM " + _metadata.getTableForType(_rootType);
    String where = _whereExpr == null ? "" : "WHERE " + _whereExpr.getSQL( _metadata );
    PreparedStatement delete = DBSupport.prepareStatement(from + " " + where, getArgs());
    return delete.execute();
  }

  public <U> SQLQuery<U> pick( IColumnEnum ref)
  {
    SQLQuery<U> sqlQuery = (SQLQuery<U>) cloneMe();
    sqlQuery._pick = ref;
    return sqlQuery;
  }

  public SQLQuery<T> groupBy(IColumnEnum ref){
    SQLQuery<T> sqlQuery = (SQLQuery<T>) cloneMe();
    sqlQuery.setGroupBy( " GROUP BY( " + ref.getColumnName() + ") ");
    return sqlQuery;
  }

  public SQLQuery<T> count() {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " COUNT(*) " );
    return newQuery;
  }

  public SQLQuery<T> count( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " COUNT( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> max( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " MAX( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> min( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " MIN( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> sum( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " SUM( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> avg( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " AVG( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> countDistinct( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " COUNT( DISTINCT " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> maxDistinct( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " MAX( DISTINCT " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> minDistinct( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " MIN( DISTINCT " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> sumDistinct( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " SUM( DISTINCT " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> avgDistinct( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " AVG( DISTINCT " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> groupConcat( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " GROUP_CONCAT( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> groupConcatDistinct( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " GROUP_CONCAT( DISTINCT " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> listAgg( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " LISTAGG( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> listAgg( IColumnEnum ref, String str) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " LISTAGG( " + ref.getColumnName() +
      " , " + str + " ) " );
    return newQuery;
  }

  public SQLQuery<T> median( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " MEDIAN( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> stddevPop( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " STDDEVPOP( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> stddevSamp( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " STDDEVSAMP( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> varPop( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " VARPOP( " + ref.getColumnName() + " ) " );
    return newQuery;
  }

  public SQLQuery<T> varSamp( IColumnEnum ref) {
    SQLQuery<T> newQuery = cloneMe();
    newQuery.setManualSelect( " VARSAMP( " + ref.getColumnName() + " ) " );
    return newQuery;
  }





  public Iterator<T> iterator()
  {
    Iterator<T> result;
    try
    {
      if( (_pick != null) || (_manualSelect != null) ) {
        result = SQLRecord.selectSingleColumn( getSQLString(), getArgs() );
      } else {
        result = SQLRecord.select( getSQLString(), getArgs(), _rootType );
      }
    }
    catch( SQLException e )
    {
      throw new RuntimeException( e );
    }
    return result;
  }

  public String  getSQLString() {
    String select =  "SELECT " + getSelect();
    String from = "FROM " + _metadata.getTableForType(_rootType);
    String join = _joinExpr == null ? "" : _joinExpr.getSQL( _metadata);
    String where = _whereExpr == null ? "" : "WHERE " + _whereExpr.getSQL( _metadata );
    String groupBy = _groupBy == null ? "" : _groupBy;
    String orderBy =  _orderByExpr == null ? "" :  _orderByExpr.getSQL(_metadata);
    String limit =  _limitExpr == null ? "" :  _limitExpr.getSQL(_metadata);
    String offset =  _offsetExpr == null ? "" :  _offsetExpr.getSQL(_metadata);
    return select + " " +  from + " "  + join + " " + " " + where + " " + groupBy + " " + orderBy + " " + limit + " " + offset;
  }



  //--------------------------------------------------------------------------------
  // Implementation
  //--------------------------------------------------------------------------------


  private static class SetOpQuery extends SQLQuery
  {


    private final SQLQuery query1;
    private final SQLQuery query2;
    private final String opString;

    SetOpQuery(SQLQuery query1, SQLQuery query2, String opString, ISQLMetadata metadata, Class rootType)
    {
      super(metadata,rootType);
      this.query1 = query1;
      this.query2 = query2;
      this.opString = opString;
    }

    @Override
    public String getSQLString()
    {
      return query1.getSQLString() + opString + query2.getSQLString();
    }

    @Override
    public List<Object> getArgs()
    {
      List ans = new ArrayList<>();
      ans.addAll(query1.getArgs());
      ans.addAll(query2.getArgs());
      return ans;

    }
  }

  private String getSelect()
  {
    if( _pick != null )
    {
      return _pick.getColumnName();
    }
    else if ( _manualSelect != null){
      return _manualSelect;
    }
    else
    {
      return _metadata.getTableForType( _rootType ) + ".* ";
    }
  }

  public List<Object> getArgs()
  {
    List answer = new ArrayList();
    if(_joinExpr!=null) {
      answer.addAll(_joinExpr.getArgs());
    }
    if(_whereExpr!=null) {
      answer.addAll(_whereExpr.getArgs());
    }
    return answer;
  }

  private SQLQuery<T> cloneMe()
  {
    SQLQuery<T> child = new SQLQuery<T>( _metadata, _rootType );
    child._metadata = this._metadata;
    child._rootType = this._rootType;
    child._whereExpr = this._whereExpr;
    child._joinExpr = this._joinExpr;
    child._pick = this._pick;
    child._groupBy = this._groupBy;
    child._orderByExpr = this._orderByExpr;
    child._limitExpr = this._limitExpr;
    child._offsetExpr = this._offsetExpr;
    child._manualSelect = this._manualSelect;
    return child;
  }

}