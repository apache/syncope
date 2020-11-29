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
package org.apache.syncope.common.lib;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XMLTest extends SerializationTest {

    private static final XmlMapper XML_MAPPER;

    static {
        XML_MAPPER = new XmlMapper();
        XML_MAPPER.configOverride(List.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
        XML_MAPPER.configOverride(Set.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
        XML_MAPPER.configOverride(Map.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
        XML_MAPPER.enable(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL);
    }

    @Override
    protected ObjectMapper objectMapper() {
        return XML_MAPPER;
    }
}
