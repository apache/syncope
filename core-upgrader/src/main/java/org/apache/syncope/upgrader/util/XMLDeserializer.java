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
package org.apache.syncope.upgrader.util;

import com.thoughtworks.xstream.XStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for deserialization of configuration objects, empowering
 * <a href="http://xstream.codehaus.org">XStream</a>.
 */
public final class XMLDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(XMLDeserializer.class);

    @SuppressWarnings("unchecked")
    public static <T extends Object> T deserialize(final String serialized) {
        T result = null;

        final XStream xstream = new XStream();
        xstream.registerConverter(new GuardedStringConverter());
        try {
            result = (T) xstream.fromXML(URLDecoder.decode(serialized, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOG.error("During deserialization: Bad serialized input string", e);
        }
        return result;
    }

    private XMLDeserializer() {
    }
}
