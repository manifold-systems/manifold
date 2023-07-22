/*
 * Copyright (c) 2023 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.sql.rt.connection;

import manifold.rt.api.util.ManClassUtil;
import manifold.sql.rt.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 */
class BasicTxScope implements TxScope
{
  private final DbConfig _dbConfig;
  private final Set<TableRow> _rows;
  private final ReentrantReadWriteLock _lock;

  public BasicTxScope( Class<? extends SchemaType> schemaClass )
  {
    _dbConfig = DbConfigFinder.instance().findConfig( ManClassUtil.getShortClassName( schemaClass ), schemaClass );
    _rows = new LinkedHashSet<>();
    _lock = new ReentrantReadWriteLock();
  }

  @Override
  public DbConfig getDbConfig()
  {
    return _dbConfig;
  }

  @Override
  public Set<TableRow> getRows()
  {
    _lock.readLock().lock();
    try
    {
      return new HashSet<>( _rows );
    }
    finally
    {
      _lock.readLock().unlock();
    }
  }

  @Override
  public void addRow( TableRow item )
  {
    if( item == null )
    {
      throw new IllegalArgumentException( "Item is null" );
    }

    _lock.writeLock().lock();
    try
    {
      _rows.add( item );
    }
    finally
    {
      _lock.writeLock().unlock();
    }
  }

  @Override
  public void removeRow( TableRow item )
  {
    _lock.writeLock().lock();
    try
    {
      _rows.remove( item );
    }
    finally
    {
      _lock.writeLock().unlock();
    }
  }

  @Override
  public boolean commit() throws SQLException
  {
    _lock.writeLock().lock();
    try
    {
      if( _rows.isEmpty() )
      {
        return false;
      }

      TableRow first = _rows.stream().findFirst().get();

      ConnectionProvider cp = ConnectionProvider.findFirst();
      try( Connection c = cp.getConnection( getDbConfig().getName(), first.getClass() ) )
      {
        try
        {
          for( ConnectionNotifier p : ConnectionNotifier.PROVIDERS.get() )
          {
            p.init( c );
          }

          c.setAutoCommit( false );
          CrudProvider crud = CrudProvider.instance();

          for( TableRow row : _rows )
          {
            //todo: also consider adding a logger and start logging stuff, beginning with maybe getting metadata from the
            // result set and comparing jdbc types with schema jdbc types and logging any differences

            TableInfo ti = row.tableInfo();
            UpdateContext<TableRow> ctx = new UpdateContext<>( this, row, ti.getDdlTableName(), _dbConfig.getName(),
              ti.getPkCols(), ti.getAllColsWithJdbcType() );

            if( row.getBindings().isForInsert() )
            {
              crud.create( c, ctx );
            }
            else if( row.getBindings().isForUpdate() )
            {
              crud.update( c, ctx );
            }
            else if( row.getBindings().isForDelete() )
            {
              crud.delete( c, ctx );
            }
            else
            {
              throw new SQLException( "Unexpected bindings kind, neither of insert/update/delete" );
            }
          }

          c.commit();

          for( TableRow row : _rows )
          {
            row.getBindings().commit();
          }

          // reset, but the rows still point to this TxScope should fresh changes occur on them
          _rows.clear();

          return true;
        }
        catch( SQLException e )
        {
          c.rollback();

          for( TableRow row : _rows )
          {
            row.getBindings().dropHeldValues();
          }

          throw e;
        }
      }
    }
    finally
    {
      _lock.writeLock().unlock();
    }
  }
}
