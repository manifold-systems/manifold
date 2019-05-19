package manifold.jdbc.extensions.java.sql.CallableStatement;

import manifold.test.api.ExtensionManifoldTest;

import java.sql.CallableStatement;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ManCallableStatementExtTest extends ExtensionManifoldTest
{

  private static final int COLUMN_INDEX = 1;
  private static final String COLUMN_LABEL = "id";

  @Override
  public void testCoverage()
  {
    testCoverage( ManCallableStatementExt.class );
  }

  public void testGetIntOrNull() throws Exception
  {
    assertThat( callByIndex( CallableStatement::getInt, 1, false, ManCallableStatementExt::getIntOrNull ), equalTo( 1 ) );
    assertThat( callByIndex( CallableStatement::getInt, 0, false, ManCallableStatementExt::getIntOrNull ), equalTo( 0 ) );
    assertThat( callByIndex( CallableStatement::getInt, 0, true, ManCallableStatementExt::getIntOrNull ), nullValue() );

    assertThat( callByLabel( CallableStatement::getInt, 1, false, ManCallableStatementExt::getIntOrNull ), equalTo( 1 ) );
    assertThat( callByLabel( CallableStatement::getInt, 0, false, ManCallableStatementExt::getIntOrNull ), equalTo( 0 ) );
    assertThat( callByLabel( CallableStatement::getInt, 0, true, ManCallableStatementExt::getIntOrNull ), nullValue() );
  }

  public void testGetLongOrNull() throws Exception
  {
    assertThat( callByIndex( CallableStatement::getLong, 1L, false, ManCallableStatementExt::getLongOrNull ), equalTo( 1L ) );
    assertThat( callByIndex( CallableStatement::getLong, 0L, false, ManCallableStatementExt::getLongOrNull ), equalTo( 0L ) );
    assertThat( callByIndex( CallableStatement::getLong, 0L, true, ManCallableStatementExt::getLongOrNull ), nullValue() );

    assertThat( callByLabel( CallableStatement::getLong, 1L, false, ManCallableStatementExt::getLongOrNull ), equalTo( 1L ) );
    assertThat( callByLabel( CallableStatement::getLong, 0L, false, ManCallableStatementExt::getLongOrNull ), equalTo( 0L ) );
    assertThat( callByLabel( CallableStatement::getLong, 0L, true, ManCallableStatementExt::getLongOrNull ), nullValue() );
  }

  public void testGetDoubleOrNull() throws Exception
  {
    assertThat( callByIndex( CallableStatement::getDouble, 1d, false, ManCallableStatementExt::getDoubleOrNull ), equalTo( 1d ) );
    assertThat( callByIndex( CallableStatement::getDouble, 0d, false, ManCallableStatementExt::getDoubleOrNull ), equalTo( 0d ) );
    assertThat( callByIndex( CallableStatement::getDouble, 0d, true, ManCallableStatementExt::getDoubleOrNull ), nullValue() );

    assertThat( callByLabel( CallableStatement::getDouble, 1d, false, ManCallableStatementExt::getDoubleOrNull ), equalTo( 1d ) );
    assertThat( callByLabel( CallableStatement::getDouble, 0d, false, ManCallableStatementExt::getDoubleOrNull ), equalTo( 0d ) );
    assertThat( callByLabel( CallableStatement::getDouble, 0d, true, ManCallableStatementExt::getDoubleOrNull ), nullValue() );
  }

  private static <T> T callByIndex( IndexColumnSupplier<T> rsMapper, T returned, boolean wasNull,
                                    IndexColumnSupplier<T> extMapper ) throws Exception
  {
    CallableStatement rs = mock( CallableStatement.class );
    when( rsMapper.get( rs, COLUMN_INDEX ) )
        .thenReturn( returned );
    when( rs.wasNull() )
        .thenReturn( wasNull );

    T value = extMapper.get( rs, COLUMN_INDEX );

    rsMapper.get( verify( rs ), COLUMN_INDEX );
    verify( rs ).wasNull();

    return value;
  }

  private static <T> T callByLabel( LabelColumnSupplier<T> rsMapper, T returned, boolean wasNull,
                                    LabelColumnSupplier<T> extMapper ) throws Exception
  {
    CallableStatement rs = mock( CallableStatement.class );
    when( rsMapper.get( rs, COLUMN_LABEL ) )
        .thenReturn( returned );
    when( rs.wasNull() )
        .thenReturn( wasNull );

    T value = extMapper.get( rs, COLUMN_LABEL );

    rsMapper.get( verify( rs ), COLUMN_LABEL );
    verify( rs ).wasNull();

    return value;
  }

  @FunctionalInterface
  private interface IndexColumnSupplier<T>
  {
    T get( CallableStatement rs, int index ) throws SQLException;
  }

  @FunctionalInterface
  private interface LabelColumnSupplier<T>
  {
    T get( CallableStatement rs, String label ) throws SQLException;
  }
}
