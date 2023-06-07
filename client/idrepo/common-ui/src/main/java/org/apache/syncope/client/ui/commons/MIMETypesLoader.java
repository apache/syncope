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
package org.apache.syncope.client.ui.commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MIMETypesLoader implements Serializable {

    private static final long serialVersionUID = -1865866358622817499L;

    protected static final Logger LOG = LoggerFactory.getLogger(MIMETypesLoader.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected Map<String, String> mimeTypesMap;

    protected List<String> mimeTypes;

    public void load() {
        mimeTypesMap = new HashMap<>();
        try {
            JsonNode jsonNode = MAPPER.readTree(IOUtils.toString(getClass().getResourceAsStream("/MIMETypes.json")));
            for (JsonNode node : jsonNode) {
                JsonNode type = node.path("name");
                JsonNode ext = node.path("extension");
                if (!type.isMissingNode()) {
                    mimeTypesMap.put(type.asText(), ext.isMissingNode() ? "" : ext.asText());
                }
            }

            mimeTypesMap = Collections.unmodifiableMap(mimeTypesMap);
            LOG.debug("MIME types loaded: {}", mimeTypesMap);

            mimeTypes = new ArrayList<>(mimeTypesMap.keySet());
            Collections.sort(mimeTypes);
            mimeTypes = Collections.unmodifiableList(mimeTypes);
        } catch (Exception e) {
            LOG.error("Error reading file MIMETypes from resources", e);
        }
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public String getFileExt(final String mimeType) {
        return mimeTypesMap.get(mimeType);
    }
}
