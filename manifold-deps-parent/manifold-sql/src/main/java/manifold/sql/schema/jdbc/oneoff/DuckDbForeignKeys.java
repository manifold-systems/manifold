package manifold.sql.schema.jdbc.oneoff;

import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.sql.DatabaseMetaData.importedKeyNoAction;
import static java.sql.DatabaseMetaData.importedKeyNotDeferrable;

/**
 * An indirect implementation for {@link DatabaseMetaData#getImportedKeys(String, String, String)} for the DuckDB JDBC
 * driver.
 */
public class DuckDbForeignKeys
{
  private static final String PKTABLE_CAT = "PKTABLE_CAT";
  private static final String PKTABLE_SCHEM = "PKTABLE_SCHEM";
  private static final String PKTABLE_NAME = "PKTABLE_NAME";
  private static final String PKCOLUMN_NAME = "PKCOLUMN_NAME";
  private static final String FKTABLE_CAT = "FKTABLE_CAT";
  private static final String FKTABLE_SCHEM = "FKTABLE_SCHEM";
  private static final String FKTABLE_NAME = "FKTABLE_NAME";
  private static final String FKCOLUMN_NAME = "FKCOLUMN_NAME";
  private static final String KEY_SEQ = "KEY_SEQ";
  private static final String UPDATE_RULE = "UPDATE_RULE";
  private static final String DELETE_RULE = "DELETE_RULE";
  private static final String FK_NAME = "FK_NAME";
  private static final String PK_NAME = "PK_NAME";
  private static final String DEFERRABILITY = "DEFERRABILITY";

  private static final List<String> HEADER = Arrays.asList(
    PKTABLE_CAT,
    PKTABLE_SCHEM,
    PKTABLE_NAME,
    PKCOLUMN_NAME,
    FKTABLE_CAT,
    FKTABLE_SCHEM,
    FKTABLE_NAME,
    FKCOLUMN_NAME,
    KEY_SEQ,
    UPDATE_RULE,
    DELETE_RULE,
    FK_NAME,
    PK_NAME,
    DEFERRABILITY );

  public static ResultSet getImportedKeys( DatabaseMetaData metaData, String catalog, String schema, String table ) throws SQLException
  {
    String query =
      "SELECT * FROM duckdb_constraints " +
        "WHERE constraint_type = 'FOREIGN KEY' " +
        (schema == null ? "" : "AND schema_name " + (schema.isEmpty() ? "IS NULL " : "= ? ")) +
        "AND table_name = ?";

    try( PreparedStatement ps = metaData.getConnection().prepareStatement( query ) )
    {
      int param = 0;
      if( schema != null && !schema.isEmpty() )
      {
        ps.setString( ++param, schema );
      }
      ps.setString( ++param, table );

      try( ResultSet resultSet = ps.executeQuery() )
      {
        int fkCount = 0;
        LightResultSet results = new LightResultSet( HEADER );
        while( resultSet.next() )
        {
          List<String> fkColumns = getColumnNames( resultSet );
          String databaseName = resultSet.getString( "database_name" );
          String schemaName = resultSet.getString( "schema_name" );
          PkInfo pkInfo = parsePkInfo( resultSet, schemaName, fkColumns );
          String fkName = makeFkName( pkInfo, ++fkCount );
          for( int i = 0; i < fkColumns.size(); i++ )
          {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put( PKTABLE_CAT, databaseName );
            row.put( PKTABLE_SCHEM, pkInfo.pkSchema );
            row.put( PKTABLE_NAME, pkInfo.pkTable );
            row.put( PKCOLUMN_NAME, pkInfo.pkColumns.get( i ) );
            row.put( FKTABLE_CAT, databaseName );
            row.put( FKTABLE_SCHEM, schemaName );
            row.put( FKTABLE_NAME, table );
            row.put( FKCOLUMN_NAME, fkColumns.get( i ) );
            row.put( KEY_SEQ, i + 1 );
            row.put( UPDATE_RULE, importedKeyNoAction );
            row.put( DELETE_RULE, importedKeyNoAction );
            row.put( FK_NAME, fkName );
            row.put( PK_NAME, null );
            row.put( DEFERRABILITY, importedKeyNotDeferrable );
            results.addRow( row );
          }
        }
        return results;
      }
    }
  }

  @NotNull
  private static List<String> getColumnNames( ResultSet resultSet ) throws SQLException
  {
    Array array = resultSet.getArray( "constraint_column_names" );
    List<String> fkColumns;
    try
    {
      fkColumns = Arrays.stream( (Object[])array.getArray() )
        .map( v -> (String)v )
        .collect( Collectors.toList() );
    }
    finally
    {
      array.free();
    }
    return fkColumns;
  }

  private static String makeFkName( PkInfo pkInfo, int fkCount )
  {
    return "fk_" +
      (pkInfo.pkSchema == null
       ? ""
       : "pk_" + pkInfo.pkSchema + "_") +
      pkInfo.pkTable + "_" + fkCount;
  }

  @NotNull
  private static PkInfo parsePkInfo( ResultSet resultSet, String schemaName, List<String> fkColumns ) throws SQLException
  {
    String sql = resultSet.getString( "constraint_text" );
    int iRef = sql.toLowerCase().indexOf( "references" );
    String afterRef = sql.substring( iRef + "references".length() ).trim();
    int iLParen = afterRef.indexOf( '(' );
    String pkTable = afterRef.substring( 0, iLParen );
    int iPkSchemaDot = pkTable.indexOf( '.' );
    String pkSchema = iPkSchemaDot > 0 ? pkTable.substring( 0, iPkSchemaDot ) : schemaName;
    String pkColumnsStart = afterRef.substring( iLParen + 1 ).trim();
    List<String> pkColumns = new ArrayList<>();
    for( StringTokenizer tokenizer = new StringTokenizer( pkColumnsStart, ",)" ); tokenizer.hasMoreTokens(); )
    {
      String pkColumn = tokenizer.nextToken().trim();
      pkColumns.add( pkColumn );
    }
    if( pkColumns.size() != fkColumns.size() )
    {
      throw new SQLException( "pkColumns and fkColumns have different sizes" );
    }
    return new PkInfo( pkTable, pkSchema, pkColumns );
  }

  private static class PkInfo
  {
    final String pkTable;
    final String pkSchema;
    final List<String> pkColumns;

    private PkInfo( String pkTable, String pkSchema, List<String> pkColumns )
    {
      this.pkTable = pkTable;
      this.pkSchema = pkSchema;
      this.pkColumns = pkColumns;
    }
  }
}
