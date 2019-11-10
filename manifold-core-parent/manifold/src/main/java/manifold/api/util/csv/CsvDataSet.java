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

package manifold.api.util.csv;

import java.util.List;

public class CsvDataSet
{
  private final CsvHeader _header;
  private final List<Class> _types;
  private List<CsvRecord> _records;

  public CsvDataSet( CsvHeader header, List<CsvRecord> records, List<Class> types )
  {
    _header = header;
    _records = records;
    _types = types;
  }

  public CsvHeader getHeader()
  {
    return _header;
  }

  public List<CsvRecord> getRecords()
  {
    return _records;
  }

  public List<Class> getTypes()
  {
    return _types;
  }
}
