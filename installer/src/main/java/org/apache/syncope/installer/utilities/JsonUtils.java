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
package org.apache.syncope.installer.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.syncope.installer.containers.jboss.JBossAddResponse;
import org.apache.syncope.installer.containers.jboss.JBossDeployRequestContent;

public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static JBossAddResponse jBossAddResponse(final String responseBodyAsString) {
        JBossAddResponse jBossAddResponse = null;
        try {
            jBossAddResponse = OBJECT_MAPPER.readValue(responseBodyAsString, JBossAddResponse.class);
        } catch (IOException ioe) {
            // ignore
        }

        return jBossAddResponse;
    }

    public static String jBossDeployRequestContent(final JBossDeployRequestContent jBossDeployRequestContent) {
        String jBossDeployRequestContentString = "";
        try {
            jBossDeployRequestContentString = OBJECT_MAPPER.writeValueAsString(jBossDeployRequestContent);
        } catch (JsonProcessingException ioe) {
            // ignore
        }

        return jBossDeployRequestContentString;
    }

    private JsonUtils() {
        // private constructor for static utility class
    }
}
