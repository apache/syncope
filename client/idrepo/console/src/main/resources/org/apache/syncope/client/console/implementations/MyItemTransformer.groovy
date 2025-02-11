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
import groovy.transform.CompileStatic
import java.util.List;
import org.apache.commons.lang3.tuple.Pair
import org.apache.syncope.common.lib.to.EntityTO
import org.apache.syncope.common.lib.to.Item
import org.apache.syncope.common.lib.types.AttrSchemaType
import org.apache.syncope.core.persistence.api.entity.Any
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue
import org.apache.syncope.core.provisioning.api.data.ItemTransformer

@CompileStatic
class MyItemTransformer implements ItemTransformer {
	
  @Override
  Pair<AttrSchemaType, List<PlainAttrValue>> beforePropagation(
    Item item,
    Any any,
    AttrSchemaType schemaType,
    List<PlainAttrValue> values) {

    return Pair.of(schemaType, values);
  }
    
  @Override
  List<Object> beforePull(
    Item item,
    EntityTO entityTO,
    List<Object> values) {

    return values;
  }
}
