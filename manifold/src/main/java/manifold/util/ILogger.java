/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.util;

/**
 * Gosu logger-independent logging system.
 */
public interface ILogger
{
  String getName();

  void debug( Object o );

  void debug( Object o, Throwable throwable );

  boolean isDebugEnabled();

  void trace( Object o );

  void trace( Object o, Throwable throwable );

  boolean isTraceEnabled();

  void info( Object o );

  void info( Object o, Throwable throwable );

  boolean isInfoEnabled();

  void warn( Object o );

  void warn( Object o, Throwable throwable );

  void error( Object o );

  void error( Object o, Throwable throwable );

  void fatal( Object o );

  void fatal( Object o, Throwable throwable );
}
