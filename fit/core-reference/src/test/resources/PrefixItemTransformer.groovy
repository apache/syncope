/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.util.ArrayList
import java.util.List
import org.apache.commons.lang3.StringUtils
import org.apache.syncope.common.lib.to.EntityTO
import org.apache.syncope.common.lib.to.Item
import org.apache.syncope.common.lib.types.AttrSchemaType
import org.apache.syncope.core.persistence.api.entity.Attributable
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue
import org.apache.syncope.core.provisioning.api.MappingManager
import org.apache.syncope.core.provisioning.api.data.ItemTransformer

class PrefixItemTransformer implements ItemTransformer {
	
  public static String PREFIX = "PREFIX_";

  @Override
  MappingManager.IntValues beforePropagation(
    Item item,
    Attributable attributable,
    AttrSchemaType schemaType,
    List<PlainAttrValue> values) {

    if (values == null || values.isEmpty() || values.get(0).getStringValue() == null) {
      return new MappingManager.IntValues(schemaType, values);
    }

    String value = values.get(0).getStringValue();
    values.get(0).setStringValue(PREFIX + value);

    return new MappingManager.IntValues(schemaType, values);
  }

  @Override
  List<Object> beforePull(
    Item item,
    EntityTO entityTO,
    List<Object> values) {

    if (values == null || values.isEmpty() || values.get(0) == null) {
      return values;
    }
    
    List<Object> newValues = new ArrayList<>(values);
    newValues.set(0, StringUtils.substringAfter(values.get(0).toString(), PREFIX));

    return newValues;
  }
}
