package manifold.sql.model;

import java.sql.Types;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColumnDefinition {
  private String _columnName;
  private String _propertyName;
  private int _sqlType; // see java.sql.Types: http://docs.oracle.com/javase/8/docs/api/index.html?java/sql/Types.html
  private boolean notNull;
  private boolean _null;
  private boolean auto_Increment;
  private boolean identity;
  private int startInt;
  private int incrementInt;
  private boolean primaryKey;
  private boolean hash;
  private boolean unique;
  private int line;
  private int col;
  private int _offset;

  static final public HashMap<String,Integer> lookUp = new HashMap<String,Integer>();

  static{
   lookUp.put("int", Types.INTEGER);
    lookUp.put("integer", Types.INTEGER);
    lookUp.put("mediumint", Types.INTEGER);
    lookUp.put("int4", Types.INTEGER);
    lookUp.put("signed", Types.INTEGER);
    lookUp.put("boolean", Types.BOOLEAN);
    lookUp.put("bit", Types.BOOLEAN);
    lookUp.put("bool", Types.BOOLEAN);
    lookUp.put("tinyint", Types.TINYINT);
    lookUp.put("smallint", Types.SMALLINT);
    lookUp.put("int2", Types.SMALLINT);
    lookUp.put("year", Types.SMALLINT);
    lookUp.put("bigint", Types.BIGINT);
    lookUp.put("int8", Types.BIGINT);
    lookUp.put("identity", Types.BIGINT);
    lookUp.put("decimal", Types.DECIMAL);
    lookUp.put("numeric", Types.DECIMAL);
    lookUp.put("dec", Types.DECIMAL);
    lookUp.put("number", Types.DECIMAL);
    lookUp.put("double", Types.DOUBLE);
    lookUp.put("float8", Types.DOUBLE);
    lookUp.put("float8", Types.DOUBLE);
    lookUp.put("float4", Types.FLOAT);
    lookUp.put("real", Types.FLOAT);
    lookUp.put("time", Types.TIME);
    lookUp.put("date", Types.DATE);
    lookUp.put("timestamp", Types.TIMESTAMP);
    lookUp.put("datetime", Types.TIMESTAMP);
    lookUp.put("smalldatetime", Types.TIMESTAMP);
    lookUp.put("other", Types.OTHER);
    lookUp.put("varchar", Types.NVARCHAR);
    lookUp.put("longvarchar", Types.NVARCHAR);
    lookUp.put("varchar2", Types.NVARCHAR);
    lookUp.put("nvarchar", Types.NVARCHAR);
    lookUp.put("nvarchar2", Types.NVARCHAR);
    lookUp.put("varchar_casesenstitive", Types.NVARCHAR);
    lookUp.put("varchar_ignorecase", Types.NVARCHAR);
    lookUp.put("char", Types.NCHAR);
    lookUp.put("character", Types.NCHAR);
    lookUp.put("nchar", Types.NCHAR);
    lookUp.put("blob", Types.BLOB);
    lookUp.put("tinyblob", Types.BLOB);
    lookUp.put("mediumblob", Types.BLOB);
    lookUp.put("longblob", Types.BLOB);
    lookUp.put("image", Types.BLOB);
    lookUp.put("oid", Types.BLOB);
    lookUp.put("clob", Types.CLOB);
    lookUp.put("tinytext", Types.CLOB);
    lookUp.put("text", Types.CLOB);
    lookUp.put("mediumtext", Types.CLOB);
    lookUp.put("longtext", Types.CLOB);
    lookUp.put("ntext", Types.CLOB);
    lookUp.put("nclob", Types.CLOB);

  }

  private int _length;

  public ColumnDefinition(String columnName, int sqlType) {
    _columnName = columnName;
    _propertyName = toCapitalizedCamelCase( _columnName );
    _sqlType = sqlType;
  }

  public String getColumnName() {
    return _columnName;
  }

  public String getPropertyName() {
    return _propertyName;
  }

  public int getSQLType() {
    return _sqlType;
  }

  public void setSQLType(int sqlType) {
    _sqlType = sqlType;
  }
  public void setNotNull(boolean in){
    notNull = in;
  }

  public void setNull(boolean in){
    _null = in;
  }
  public void setAutoIncrement(boolean in){
    auto_Increment = in;
  }
  public void setIdentity(boolean in){
    identity = in;
  }
  public void setStartInt(int in){
    startInt = in;
  }
  public void setIncrementInt(int in){
    incrementInt = in;
  }
  public void setPrimaryKey(boolean in){
    primaryKey = in;
  }
  public void setHash(boolean in){
    hash = in;
  }
  public void setUnique(boolean in){
    unique = in;
  }

  public boolean getNotNull(){
    return notNull;
  }
  public boolean getNull(){
    return _null;
  }
  public boolean getAutoIncrement(){
    return auto_Increment;
  }
  public boolean getIdentity(){
    return identity;
  }
  public int getStartInt(){
    return startInt;
  }
  public int getIncrementInt(){
    return incrementInt;
  }
  public boolean getPrimaryKey(){
    return primaryKey;
  }
  public boolean getHash(){
    return hash;
  }
  public boolean getUnique(){
    return unique;
  }

  public void setLoc(int _line, int _col, int offset, int length) {
    line = _line;
    col = _col;
    _offset = offset;
    _length = length;
  }

  public int getLine(){
    return line;
  }

  public int getCol(){
    return col;
  }

  public int getOffset() {
      return _offset;
  }

  public int getLength() {
    return _length;
  }

  private String toCapitalizedCamelCase( String str ){
    Pattern p = Pattern.compile("_(.)");
    Matcher m = p.matcher(str);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      m.appendReplacement(sb, m.group(1).toUpperCase());
    }
    m.appendTail(sb);
    String returnStr = sb.toString();
    return returnStr.substring(0, 1).toUpperCase() + returnStr.substring(1);
  }
}
