/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.internal.host;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import manifold.api.fs.IFile;
import manifold.api.host.ITypeSystemListener;
import manifold.api.host.RefreshKind;
import manifold.api.host.RefreshRequest;

/**
 * For compilation, supports only create events
 */
public class TypeRefreshListener
{
  private final SingleModuleManifoldHost _host;
  private final CopyOnWriteArrayList<WeakReference<ITypeSystemListener>> _listeners;

  TypeRefreshListener( SingleModuleManifoldHost host )
  {
    _host = host;
    _listeners = new CopyOnWriteArrayList<>();
  }

  /**
   * Maintains weak refs to listeners. This is primarily so that tests don't
   * accumulate a bunch of listeners over time. Otherwise this is a potential
   * memory gobbler in tests.
   * <p>
   * Note! Callers must manage the lifecycle of the listener, otherwise since this
   * method creates a weak ref, it will be collected when it goes out of scope.
   *
   * @param l Your type loader listener
   */
  void addTypeSystemListenerAsWeakRef( ITypeSystemListener l )
  {
    if( !hasListener( l ) )
    {
      _listeners.add( new WeakReference<>( l ) );
    }
  }

  @SuppressWarnings("unused")
  public void removeTypeSystemListener( ITypeSystemListener l )
  {
    for( WeakReference<ITypeSystemListener> ref: _listeners )
    {
      if( ref.get() == l )
      {
        _listeners.remove( ref );
        break;
      }
    }
  }

  private List<ITypeSystemListener> getListeners()
  {
    List<ITypeSystemListener> listeners = new ArrayList<>( _listeners.size() );
    List<WeakReference<ITypeSystemListener>> obsoleteListeners = null;
    for( WeakReference<ITypeSystemListener> ref: _listeners )
    {
      ITypeSystemListener typeSystemListener = ref.get();
      if( typeSystemListener != null )
      {
        listeners.add( typeSystemListener );
      }
      else
      {
        if( obsoleteListeners == null )
        {
          obsoleteListeners = new ArrayList<>();
        }
        obsoleteListeners.add( ref );
      }
    }
    if( obsoleteListeners != null )
    {
      _listeners.removeAll( obsoleteListeners );
    }

    return listeners;
  }

  private boolean hasListener( ITypeSystemListener l )
  {
    for( WeakReference<ITypeSystemListener> ref: _listeners )
    {
      if( ref.get() == l )
      {
        return true;
      }
    }
    return false;
  }

  //  void modified( IResource file )
//  {
//    notify( file, RefreshKind.MODIFICATION );
//  }
  void created( IFile file, String[] fqns )
  {
    notify( file, fqns, RefreshKind.CREATION );
  }
//  void deleted( IResource file )
//  {
//    notify( file, RefreshKind.DELETION );
//  }

  private void notify( IFile file, String[] fqns, @SuppressWarnings("SameParameterValue") RefreshKind kind )
  {
    RefreshRequest request = new RefreshRequest( file, fqns, _host.getSingleModule(), kind );
    List<ITypeSystemListener> listeners = getListeners();
    switch( kind )
    {
      case CREATION:
      case MODIFICATION:
        // for creation the file system needs to be updated *before* other listeners
        notifyEarlyListeners( request, listeners );
        notifyNonearlyListeners( request, listeners );
        break;

      case DELETION:
        // for deletion the file system needs to be updated *after* other listeners
        notifyNonearlyListeners( request, listeners );
        notifyEarlyListeners( request, listeners );
        break;
    }
  }

  private void notifyNonearlyListeners( RefreshRequest request, List<ITypeSystemListener> listeners )
  {
    for( ITypeSystemListener listener: listeners )
    {
      if( !listener.notifyEarly() )
      {
        listener.refreshedTypes( request );
      }
    }
  }

  private void notifyEarlyListeners( RefreshRequest request, List<ITypeSystemListener> listeners )
  {
    for( ITypeSystemListener listener: listeners )
    {
      if( listener.notifyEarly() )
      {
        listener.refreshedTypes( request );
      }
    }
  }
}
