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

package manifold.csv.api;


import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.Test;
import abc.csv.Nnndss;
import abc.csv.Nnndss.NnndssItem;
import abc.csv.insurance_sample_comma;
import abc.csv.insurance_sample_comma.insurance_sample_commaItem;


import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class CsvTest
{
  @Test
  public void testManifold()
  {
    insurance_sample_comma items = insurance_sample_comma.fromSource();
    assertEquals(17, items.size());
    for (insurance_sample_commaItem item : items) {
      assertNotNull(item.getPolicyID());
      assertNotNull(item.getTiv_2012());
    }
  }

  @Test
  public void testStuff() throws IOException, URISyntaxException
  {
    Nnndss nnndss = Nnndss.fromSource();
    for( NnndssItem item: nnndss) {
      Integer value = item.getInvasive_pneumococcal_disease__age___5___Confirmed__Current_week();
    }
  }

}
