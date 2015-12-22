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
package org.apache.syncope.client.console.init;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MIMETypesLoader {

    private static final Logger LOG = LoggerFactory.getLogger(MIMETypesLoader.class);

    private List<String> mimeTypes;

    public void load() {
        if (CollectionUtils.isEmpty(mimeTypes)) {
            Set<String> mediaTypes = new HashSet<>();
            this.mimeTypes = new ArrayList<>();
            try {
                final String mimeTypesFile = IOUtils.toString(getClass().getResourceAsStream("/MIMETypes"));
                for (String fileRow : mimeTypesFile.split("\n")) {
                    if (StringUtils.isNotBlank(fileRow) && !fileRow.startsWith("#")) {
                        mediaTypes.add(fileRow);
                    }
                }
                this.mimeTypes.addAll(mediaTypes);
                Collections.sort(this.mimeTypes);
            } catch (Exception e) {
                LOG.error("Error reading file MIMETypes from resources", e);
            }
        }
    }

    public List<String> getMimeTypes() {
        LOG.debug("Returning loaded MIME types list {}", mimeTypes);
        return mimeTypes;
    }
}
