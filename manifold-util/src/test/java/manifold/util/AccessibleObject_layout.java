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

package manifold.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import sun.misc.Unsafe;

// Duplicate the field layout of AccessibleObject so we can get the offset of the `override` field
@SuppressWarnings({"unused", "WeakerAccess"})
public class AccessibleObject_layout implements AnnotatedElement
{
  static final private String ACCESS_PERMISSION = "";

  boolean override;

  static final String reflectionFactory = "";

  volatile Object securityCheckCache;


  protected AccessibleObject_layout() {}

  @Override
  public <T extends Annotation> T getAnnotation( Class<T> annotationClass )
  {
    return null;
  }

  @Override
  public Annotation[] getAnnotations()
  {
    return new Annotation[0];
  }

  @Override
  public Annotation[] getDeclaredAnnotations()
  {
    return new Annotation[0];
  }

  static long getOverrideOffset( Unsafe unsafe )
  {
    try
    {
      return unsafe.objectFieldOffset( AccessibleObject_layout.class.getDeclaredField( "override" ) );
    }
    catch( NoSuchFieldException e )
    {
      throw new RuntimeException( e );
    }
  }
}
