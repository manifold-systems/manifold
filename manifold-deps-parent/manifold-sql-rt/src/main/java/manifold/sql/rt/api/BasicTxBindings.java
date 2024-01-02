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

package manifold.sql.rt.api;

import manifold.ext.rt.api.IBindingsBacked;
import manifold.json.rt.api.DataBindings;
import manifold.rt.api.Bindings;
import manifold.util.concurrent.LockingLazyVar;

import java.sql.SQLException;
import java.util.*;

public class BasicTxBindings implements OperableTxBindings
{
  /**
   * Persisted state
   */
  private final Bindings _persistedState;

  /**
   * Uncommitted changes
   */
  private final Map<String, Object> _changes;

  /**
   * On hold state is assigned during {@link #commit()} such as generated keys.
   * This state is assigned to persisted state only after {@link TxScope#commit()}.
   */
  private final Map<String, Object> _onHold;

  private final LockingLazyVar<Bindings> _metadata;

  private Entity _owner;
  private final OperableTxScope _txScope;
  private TxKind _txKind;
  private boolean _delete;

  /**
   * Creates a new bindings for an entity instance.
   * <p/>
   * The {@code txKind} not only indicates the operational context of the entity, it also signals the type of initial
   * state of the bindings. For instance, the {@code Insert} kind implies the object has no persisted state because it is
   * entirely new, thus the {@code initialState} is a set of uncommitted changes. Similarly, the {@code Update} kind must
   * involve an existing entity, the state of which reflects its persisted state from the data source.
   */
  public BasicTxBindings( TxScope txScope, TxKind txKind, Bindings initialState )
  {
    if( initialState == null )
    {
      throw new NullPointerException( "initialState is null" );
    }

    _txScope = (OperableTxScope)txScope;
    _txKind = txKind;
    _changes = new LinkedHashMap<>();
    switch( _txKind )
    {
      case Insert:
        _persistedState = new DataBindings();
        _changes.putAll( initialState );
        break;
      case Update:
        _persistedState = initialState;
        break;
      case Unknown:
      default:
        throw new IllegalArgumentException( "TxKind '" + txKind + "' not supported here" );
    }
    _onHold = new LinkedHashMap<>();
    _metadata = LockingLazyVar.make( () -> new DataBindings() );
  }

  @Override
  public Entity getOwner()
  {
    return _owner;
  }
  @Override
  public void setOwner( Entity owner )
  {
    _owner = owner;
  }

  public TxScope getTxScope()
  {
    return _txScope;
  }

  @Override
  public boolean isForInsert()
  {
    return _txKind == TxKind.Insert && !_delete;
  }

  @Override
  public boolean isForUpdate()
  {
    return (_txKind == TxKind.Update || _txKind == TxKind.InsertUpdate) && !_delete;
  }

  @Override
  public boolean isForDelete()
  {
    return _delete;
  }

  @Override
  public void setDelete( boolean value )
  {
    if( _delete == value )
    {
      return;
    }

    _delete = value;

    switch( _txKind )
    {
      case Update:
      case InsertUpdate:
        if( _delete )
        {
          // add for deletion
          _txScope.addRow( getOwner() );
        }
        else if( _changes.isEmpty() )
        {
          // no changes for update, was only there for deletion, no reason for it to remain
          _txScope.removeRow( getOwner() );
        }
        break;

      case Insert:
        if( _delete )
        {
          // newly created row is not in db, just remove it from txScope
          _txScope.removeRow( getOwner() );
        }
        else
        {
          // add created row back to txScope
          _txScope.addRow( getOwner() );
        }
        break;

      default:
        throw new UnsupportedOperationException( "Can't delete, TxKind is " + _txKind );
    }
  }

  @Override
  public void holdValues( Bindings valuesToHold )
  {
    _onHold.putAll( valuesToHold );
  }

  public void holdValue( String name, Object value )
  {
    _onHold.put( name, value );
  }

  @Override
  public Object getHeldValue( String name )
  {
    return _onHold.get( name );
  }

  @Override
  public void dropHeldValues()
  {
    _onHold.clear();
  }

  /**
   * Commit is called _after_ a successful commit on the TxScope.
   * @throws SQLException
   */
  @Override
  public void commit() throws SQLException
  {
    if( _txKind == TxKind.Unknown )
    {
      throw new SQLException( "Cannot commit bindings in 'Unknown' state. Bindings were likely deleted or rolled back from creation." );
    }

    if( _delete )
    {
      _persistedState.clear();
      _changes.clear();
      _onHold.clear();
      _txKind = TxKind.Unknown;
      return;
    }

    _persistedState.putAll( _changes );
    _changes.clear();
    _persistedState.putAll( _onHold );
    _onHold.clear();

    _txKind = TxKind.Update;
  }

  @Override
  public void failedCommit() throws SQLException
  {
    if( _txKind == TxKind.InsertUpdate )
    {
      _txKind = TxKind.Insert;
    }
    dropHeldValues();
  }

  @Override
  public void reuse()
  {
    if( _txKind == TxKind.Insert )
    {
      _txKind = TxKind.InsertUpdate;
    }
  }

  /** For internal use, only to be called from {@link TxScope#revert()} */
  public void revert() throws SQLException
  {
    switch( _txKind )
    {
      case Insert:
        _changes.clear();
        _txKind = TxKind.Unknown;
        break;
      case Update:
        _changes.clear();
        _delete = false;
        break;
      case Unknown:
      default:
        throw new SQLException( "Cannot rollback bindings in 'Unknown' state." );
    }
  }

  @Override
  public Bindings getMetadata()
  {
    return _metadata.get();
  }

  public Object put( String name, Object value )
  {
    if( value instanceof IBindingsBacked )
    {
      throw new IllegalArgumentException( "Non-raw bindings: " + value );
    }

    if( isForDelete() )
    {
      throw new RuntimeException( "Illegal operation, instance pending deletion" );
    }

    if( _txKind == TxKind.Unknown )
    {
      throw new RuntimeException( "Cannot modify bindings in 'Unknown' state. Bindings were likely deleted or rolled back from creation." );
    }

    checkKey( name );
    Object existing;
    if( _persistedState.containsKey( name ) &&
      Objects.equals( existing = _persistedState.get( name ), value ) )
    {
      // remove the key from changes if same as persisted state, avoids unnecessary updates
      _changes.remove( name );
      if( _changes.isEmpty() )
      {
        // if no changes remain, remove the item from the txScope
        _txScope.removeRow( getOwner() );
      }
      return existing;
    }

    _txScope.addRow( getOwner() );
    return _changes.put( name, value );
  }

  @SuppressWarnings( "NullableProblems" )
  public void putAll( Map<? extends String, ?> toMerge )
  {
    if( toMerge == null )
    {
      throw new NullPointerException( "toMerge map is null" );
    }
    for( Map.Entry<? extends String, ?> entry: toMerge.entrySet() )
    {
      String key = entry.getKey();
      checkKey( key );
      put( key, entry.getValue() );
    }
  }

  public void clear()
  {
    _changes.clear();
  }

  public boolean containsKey( Object key )
  {
    checkKey( key );
    return _changes.containsKey( key ) ||
      _persistedState.containsKey( key );
  }

  public boolean containsValue( Object value )
  {
    return _changes.containsValue( value ) || _persistedState.containsValue( value );
  }

  public Set<Entry<String, Object>> entrySet()
  {
    Set<Entry<String, Object>> entrySet = new HashSet<>( _changes.entrySet() );
    _persistedState.entrySet().stream()
      .filter( e -> !_changes.containsKey( e.getKey() ) )
      .forEach( e -> entrySet.add( e ) );
    return entrySet;
  }

  public Map<String, Object> uncommittedChangesEntrySet()
  {
    return new LinkedHashMap<>( _changes );
  }

  public Map<String, Object> persistedStateEntrySet()
  {
    return new LinkedHashMap<>( _persistedState );
  }

  @Override
  public Object getPersistedStateValue( String name )
  {
    return _persistedState.get( name );
  }

  public Object get( Object key )
  {
    checkKey( key );
    if( _changes.containsKey( (String)key ) )
    {
      Object value = _changes.get( key );
      if( value instanceof KeyRef )
      {
        value = ((KeyRef)value).getRef();
        if( ((Entity)value).getBindings().isForDelete() )
        {
          value = null;
        }
      }
      return value;
    }
    return _persistedState.get( key );
  }

  public boolean isEmpty()
  {
    return _changes.isEmpty() && _persistedState.isEmpty();
  }

  public Set<String> keySet()
  {
    Set<String> keySet = new LinkedHashSet<>( _persistedState.keySet() );
    keySet.addAll( _changes.keySet() );
    return keySet;
  }

  /**
   * Note, sets the key's value to null in the changes bindings, to distinguish null values from unset values.
   */
  public Object remove( Object key )
  {
    checkKey( key );
    Object priorValue;
    if( _changes.containsKey( (String)key ) )
    {
      priorValue = _changes.get( key );
    }
    else
    {
      priorValue = _persistedState.get( key );
    }
    _changes.put( (String)key, null );
    return priorValue;
  }

  public int size()
  {
    return keySet().size();
  }

  public Collection<Object> values()
  {
    List<Object> values = new ArrayList<>( _changes.values() );
    _persistedState.entrySet().stream()
      .filter( e -> !_changes.containsKey( e.getKey() ) )
      .forEach( e -> values.add( e.getValue() ) );
    return values;
  }

  private void checkKey( Object key )
  {
    if( key == null )
    {
      throw new NullPointerException( "key can not be null" );
    }
    if( !(key instanceof String) )
    {
      throw new ClassCastException( "key should be a String" );
    }
    if( key.equals( "" ) )
    {
      throw new IllegalArgumentException( "key can not be empty" );
    }
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o ) return true;
    if( !(o instanceof BasicTxBindings) ) return false;
    BasicTxBindings that = (BasicTxBindings)o;
    return entrySet().equals( that.entrySet() );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( entrySet() );
  }
}
