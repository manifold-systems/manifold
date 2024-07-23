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

package xyz;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static xyz.OtherAnno.Local.*;

@Retention( RetentionPolicy.CLASS )
public @interface OtherAnno
{
  String value() default "foo";
  boolean booleanValue() default true;
  int intValue() default 9;
  double doubleValue() default 9.9;
  String stringValue() default "hi";
  Class<?> classValue() default Local.class;
  Local enumValue() default B;
  InnerAnno annoValue() default @InnerAnno(stringValue = "abc");
  boolean[] booleanArrayValue() default {true,false};
  int[] intArrayValue() default {1,2};
  double[] doubleArrayValue() default {1.2,2.1};
  String[] stringArrayValue() default {"a", "b"};
  Class[] classArrayValue() default {String.class, int.class};
  Local[] enumArrayValue() default {A, B};
  InnerAnno[] annoArrayValue() default {@InnerAnno, @InnerAnno(value=7)};


  enum Local {A, B, C}

  @Retention( RetentionPolicy.CLASS )
  @interface InnerAnno
  {
    int value() default 0;
    String stringValue() default "bar";
  }
}
