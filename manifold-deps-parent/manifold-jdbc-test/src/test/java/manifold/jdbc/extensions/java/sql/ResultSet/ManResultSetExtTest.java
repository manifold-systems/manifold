package manifold.jdbc.extensions.java.sql.ResultSet;

import manifold.test.api.ExtensionManifoldTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ManResultSetExtTest extends ExtensionManifoldTest
{

  private static final int COLUMN_INDEX = 1;
  private static final String COLUMN_LABEL = "id";

  @Override
  public void testCoverage()
  {
    testCoverage( ManResultSetExt.class );
  }

  public void testGetIntOrNull() throws Exception
  {
    assertThat( callByIndex( ResultSet::getInt, 1, false, ManResultSetExt::getIntOrNull ), equalTo( 1 ) );
    assertThat( callByIndex( ResultSet::getInt, 0, false, ManResultSetExt::getIntOrNull ), equalTo( 0 ) );
    assertThat( callByIndex( ResultSet::getInt, 0, true, ManResultSetExt::getIntOrNull ), nullValue() );

    assertThat( callByLabel( ResultSet::getInt, 1, false, ManResultSetExt::getIntOrNull ), equalTo( 1 ) );
    assertThat( callByLabel( ResultSet::getInt, 0, false, ManResultSetExt::getIntOrNull ), equalTo( 0 ) );
    assertThat( callByLabel( ResultSet::getInt, 0, true, ManResultSetExt::getIntOrNull ), nullValue() );
  }

  public void testGetLongOrNull() throws Exception
  {
    assertThat( callByIndex( ResultSet::getLong, 1L, false, ManResultSetExt::getLongOrNull ), equalTo( 1L ) );
    assertThat( callByIndex( ResultSet::getLong, 0L, false, ManResultSetExt::getLongOrNull ), equalTo( 0L ) );
    assertThat( callByIndex( ResultSet::getLong, 0L, true, ManResultSetExt::getLongOrNull ), nullValue() );

    assertThat( callByLabel( ResultSet::getLong, 1L, false, ManResultSetExt::getLongOrNull ), equalTo( 1L ) );
    assertThat( callByLabel( ResultSet::getLong, 0L, false, ManResultSetExt::getLongOrNull ), equalTo( 0L ) );
    assertThat( callByLabel( ResultSet::getLong, 0L, true, ManResultSetExt::getLongOrNull ), nullValue() );
  }

  public void testGetDoubleOrNull() throws Exception
  {
    assertThat( callByIndex( ResultSet::getDouble, 1d, false, ManResultSetExt::getDoubleOrNull ), equalTo( 1d ) );
    assertThat( callByIndex( ResultSet::getDouble, 0d, false, ManResultSetExt::getDoubleOrNull ), equalTo( 0d ) );
    assertThat( callByIndex( ResultSet::getDouble, 0d, true, ManResultSetExt::getDoubleOrNull ), nullValue() );

    assertThat( callByLabel( ResultSet::getDouble, 1d, false, ManResultSetExt::getDoubleOrNull ), equalTo( 1d ) );
    assertThat( callByLabel( ResultSet::getDouble, 0d, false, ManResultSetExt::getDoubleOrNull ), equalTo( 0d ) );
    assertThat( callByLabel( ResultSet::getDouble, 0d, true, ManResultSetExt::getDoubleOrNull ), nullValue() );
  }

  private static <T> T callByIndex( IndexColumnSupplier<T> rsMapper, T returned, boolean wasNull,
                                    IndexColumnSupplier<T> extMapper ) throws Exception
  {
    ResultSet rs = mock( ResultSet.class );
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
    ResultSet rs = mock( ResultSet.class );
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
    T get( ResultSet rs, int index ) throws SQLException;
  }

  @FunctionalInterface
  private interface LabelColumnSupplier<T>
  {
    T get( ResultSet rs, String label ) throws SQLException;
  }
}
