/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.internal.javac;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import manifold.util.JdkAccessUtil;

/**
 * The {@link JavacPlugin} can't be used directly with Java 16+ because stricter accessibility changes were added to
 * internal modules (JEP 396: Strongly Encapsulate JDK Internals by Default). For instance, {@code jdk.compiler} exports
 * fewer packages. This means we must make the necessary module changes via bypassJava9Security() <i>before</i> {@code
 * JavacPlugin} is constructed, which is the purpose of this bootstrap plugin; it makes dynamic module changes before
 * constructing and delegating to {@code JavacPlugin}.
 */
public class JavacPluginBootstrap implements Plugin
{
  private final Plugin _delegate;

  public JavacPluginBootstrap()
  {
    JdkAccessUtil.openModules();
    _delegate = new JavacPlugin();
  }

  @Override
  public String getName()
  {
    return "Manifold";
  }

  @Override
  public void init( JavacTask task, String... args )
  {
    _delegate.init( task, args );
  }
}
