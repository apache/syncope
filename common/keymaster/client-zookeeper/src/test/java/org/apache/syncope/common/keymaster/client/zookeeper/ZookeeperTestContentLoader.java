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
package org.apache.syncope.common.keymaster.client.zookeeper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Iterator;
import java.util.Map;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class ZookeeperTestContentLoader implements InitializingBean {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @Autowired
    private ConfParamOps confParamOps;

    @Override
    public void afterPropertiesSet() throws Exception {
        JsonNode content = MAPPER.readTree(getClass().getResourceAsStream("/testKeymasterConfParams.json"));
        for (Iterator<Map.Entry<String, JsonNode>> itor = content.fields(); itor.hasNext();) {
            Map.Entry<String, JsonNode> param = itor.next();
            Object value = MAPPER.treeToValue(param.getValue(), Object.class);
            if (value != null) {
                confParamOps.set(ZookeeperConfParamOpsTest.DOMAIN, param.getKey(), value);
            }
        }
    }
}
