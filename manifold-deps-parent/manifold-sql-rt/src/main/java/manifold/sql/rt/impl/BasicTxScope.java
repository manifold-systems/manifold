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

package manifold.sql.rt.impl;

import manifold.rt.api.util.ManClassUtil;
import manifold.sql.rt.api.*;
import manifold.util.concurrent.ConcurrentHashSet;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 */
class BasicTxScope implements OperableTxScope
{
  private final DbConfig _dbConfig;
  private final Set<Entity> _rows;
  private final Set<Entity> _processedRows;
  private final ReentrantReadWriteLock _lock;
  private final List<ScopeConsumer> _sqlChanges;
  private Connection _connection;

  public BasicTxScope( Class<? extends SchemaType> schemaClass )
  {
    _dbConfig = Dependencies.instance().getDbConfigProvider()
      .loadDbConfig( ManClassUtil.getShortClassName( schemaClass ), schemaClass );
    _rows = new LinkedHashSet<>();
    _processedRows = new ConcurrentHashSet<>();
    _sqlChanges = new ArrayList<>();
    _lock = new ReentrantReadWriteLock();
  }

  @Override
  public DbConfig getDbConfig()
  {
    return _dbConfig;
  }

  @Override
  public Set<Entity> getRows()
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
  public void addRow( Entity item )
  {
    if( item == null )
    {
      throw new IllegalArgumentException( "Item is null" );
    }

    if( _processedRows.remove( item ) )
    {
      ((OperableTxBindings)item.getBindings()).reuse();
    }

    if( containsRow( item  ) )
    {
      return;
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
  public void removeRow( Entity item )
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
  public boolean containsRow( Entity item )
  {
    _lock.readLock().lock();
    try
    {
      return _rows.contains( item );
    }
    finally
    {
      _lock.readLock().unlock();
    }
  }

  @Override
  public void addSqlChange( ScopeConsumer change )
  {
    if( change == null )
    {
      throw new IllegalArgumentException( "'change' is null" );
    }

    _lock.writeLock().lock();
    try
    {
      _sqlChanges.add( change );
    }
    finally
    {
      _lock.writeLock().unlock();
    }
  }

  @Override
  public void commit() throws SQLException
  {
    _lock.writeLock().lock();
    try
    {
      if( _rows.isEmpty() && _sqlChanges.isEmpty() )
      {
        // no changes to commit
        return;
      }

      Object commitItem = _rows.isEmpty() ? _sqlChanges.get( 0 ) : _rows.stream().findFirst().get();

      ConnectionProvider cp = Dependencies.instance().getConnectionProvider();
      try( Connection c = cp.getConnection( getDbConfig().getName(), commitItem.getClass() ) )
      {
        _connection = c;
        try
        {
          c.setAutoCommit( false );

          Set<Entity> visited = new HashSet<>();
          for( Entity row : _rows )
          {
            doCrud( c, row, new LinkedHashMap<>(), visited );
          }

          for( ScopeConsumer sqlChange : _sqlChanges )
          {
            executeSqlChange( c, sqlChange );
          }
          c.commit();

          for( Entity row : _rows )
          {
            ((OperableTxBindings)row.getBindings()).commit();
          }

          _rows.clear();
          _sqlChanges.clear();
        }
        catch( SQLException e )
        {
          c.rollback();

          for( Entity row : _rows )
          {
            ((OperableTxBindings)row.getBindings()).failedCommit();
          }

          throw e;
        }
        finally
        {
          _connection = null;
        }
      }
    }
    finally
    {
      _lock.writeLock().unlock();
    }
  }

  @Override
  public void commit( ScopeConsumer change ) throws SQLException
  {
    addSqlChange( change );
    commit();
  }

  private void executeSqlChange( Connection c, ScopeConsumer sqlChange ) throws SQLException
  {
    sqlChange.accept( newSqlChangeCtx( c ) );
  }

  @Override
  public SqlChangeCtx newSqlChangeCtx( Connection c )
  {
    return new SqlChangeCtx()
    {
      @Override
      public TxScope getTxScope()
      {
        return BasicTxScope.this;
      }

      @Override
      public Connection getConnection()
      {
        return c;
      }

      @Override
      public void doCrud() throws SQLException
      {
        Set<Entity> visited = new HashSet<>();
        for( Entity row : _rows )
        {
          BasicTxScope.this.doCrud( c, row, new LinkedHashMap<>(), visited );
        }
      }
    };
  }

  @Override
  public void revert() throws SQLException
  {
    if( _connection != null )
    {
      throw new SQLException( "Revert is not supported within a transaction" );
    }

    _lock.writeLock().lock();
    try
    {
      for( Entity row : _rows )
      {
        ((OperableTxBindings)row.getBindings()).revert();
      }                   
      _rows.clear();
      _sqlChanges.clear();
    }
    finally
    {
      _lock.writeLock().unlock();
    }
  }

  private void doCrud( Connection c, Entity row, Map<Entity, Set<FkDep>> unresolvedDeps, Set<Entity> visited ) throws SQLException
  {
    if( visited.contains( row ) )
    {
      return;
    }
    visited.add( row );

    if( _processedRows.contains( row ) )
    {
      return;
    }
    _processedRows.add( row );

    doFkDependenciesFirst( c, row, unresolvedDeps, visited );

    CrudProvider crud = Dependencies.instance().getCrudProvider();

    TableInfo ti = row.tableInfo();
    UpdateContext<Entity> ctx = new UpdateContext<>( this, row, ti.getDdlTableName(), _dbConfig.getName(),
      ti.getPkCols(), ti.getUkCols(), ti.getAllCols() );

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
    patchUnresolvedFkDeps( c, ctx, crud, unresolvedDeps.get( row ) );
  }

  /**
   * Note, unresolved fk dependencies happen when there are fk cycles e.g., Foo has fk on Bar's pk, Bar has fk on Foo's pk
   * If the database platform supports Deferrable constraints, the fk constraints are not enforced until commit ("Initially Deferred"),
   * which enables the handling of cycles by allowing a null value for an fk before commit.
   */
  private void patchUnresolvedFkDeps( Connection c, UpdateContext<Entity> ctx, CrudProvider crud, Set<FkDep> unresolvedDeps ) throws SQLException
  {
    if( unresolvedDeps == null )
    {
      return;
    }

    for( FkDep dep : unresolvedDeps )
    {
      Object pkId = ((OperableTxBindings)dep.pkRow.getBindings()).getHeldValue( dep.pkName );
      if( pkId == null )
      {
        throw new SQLException( "pk value is null" );
      }

      // update the fk column that was null initially due to fk cycle
      OperableTxBindings fkBindings = (OperableTxBindings)dep.fkRow.getBindings();
      Object priorPkId = fkBindings.put( dep.fkName, pkId );
      try
      {
        // re-update with same values + fkId
        crud.update( c, ctx );
      }
      finally
      {
        // put old value back into changes (s/b Pair<Entity, String>)
        fkBindings.put( dep.fkName, priorPkId );
        // put id into hold values because it should not take effect until commit
        fkBindings.holdValue( dep.fkName, pkId );
      }
    }
  }

  private void doFkDependenciesFirst( Connection c, Entity row, Map<Entity, Set<FkDep>> unresolvedDeps, Set<Entity> visited ) throws SQLException
  {
    for( Map.Entry<String, Object> entry : row.getBindings().entrySet() )
    {
      Object value = entry.getValue();
      if( value instanceof KeyRef )
      {
        KeyRef ref = (KeyRef)value;
        Entity pkEntity = ref.getRef();
        FkDep fkDep = new FkDep( row, entry.getKey(), pkEntity, ref.getKeyColName() );

        doCrud( c, pkEntity, unresolvedDeps, visited );

        // patch fk
        Object pkId = ((OperableTxBindings)pkEntity.getBindings()).getHeldValue( fkDep.pkName );
        if( pkId != null )
        {
          // pkEntity was inserted, assign fk value now, this is assigned as an INSERT param in BasicCrudProvider when
          // the value of the param is a KeyRef
          ((OperableTxBindings)row.getBindings()).holdValue( fkDep.fkName, pkId );
        }
        else
        {
          // pkEntity not inserted yet, presumably due to a fk cycle, assign a temp fk value as INSERT param and resolve
          // the fk assignment later
          unresolvedDeps.computeIfAbsent( pkEntity, __ -> new LinkedHashSet<>() )
            .add( fkDep );
        }
      }
    }
  }

  @Override
  public Connection getActiveConnection()
  {
    return _connection;
  }

  private static class FkDep
  {
    final Entity fkRow;
    final String fkName;
    final Entity pkRow;
    final String pkName;

    public FkDep( Entity fkRow, String fkName, Entity pkRow, String pkName )
    {
      this.fkRow = fkRow;
      this.fkName = fkName;
      this.pkRow = pkRow;
      this.pkName = pkName;
    }
  }
}
