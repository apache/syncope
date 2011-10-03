/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for serialization and deserialization of configuration objects.
 * It uses the JDK XMLEncoder and XMLDecoder.
 * 
 */
public class XmlConfiguration {

    private static final Logger LOG =
            LoggerFactory.getLogger(XmlConfiguration.class);

    public static String serialize(final Serializable object) {
        try {
            OutputStream os = new ByteArrayOutputStream();
            XMLEncoder encoder = new XMLEncoder(os);
            encoder.writeObject(object);
            encoder.flush();
            encoder.close();

            return URLEncoder.encode(os.toString(), "UTF-8");
        } catch (Throwable t) {
            LOG.error("During serialization", t);
            return null;
        }
    }

    public static <T extends Serializable> T deserialize(
            final String serialized) {

        try {
            InputStream is = new ByteArrayInputStream(
                    URLDecoder.decode(serialized, "UTF-8").getBytes());

            XMLDecoder decoder = new XMLDecoder(is);
            T object = (T) decoder.readObject();
            decoder.close();
            return object;
        } catch (Throwable t) {
            LOG.error("During deserialization", t);
            return null;
        }
    }
}
