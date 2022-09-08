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
package org.apache.syncope.core.provisioning.livesync;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebeziumMessageDeserializer implements Deserializer<DebeziumMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(DebeziumMessageDeserializer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {
    }

    @Override
    public DebeziumMessage deserialize(final String topic, final byte[] data) {
        DebeziumMessage message = new DebeziumMessage();
        try {
            message = MAPPER.readValue(new ByteArrayInputStream(data), DebeziumMessage.class);
        } catch (IOException e) {
            LOG.debug("Unable to parse kafka message ", e);
        }
        return message;
    }
}
