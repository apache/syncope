/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.util;

import com.thoughtworks.xstream.XStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for serialization and deserialization of configuration objects,
 * empowering XStream.
 * @see http://xstream.codehaus.org/
 */
public class XMLSerializer {

    private static final Logger LOG =
            LoggerFactory.getLogger(XMLSerializer.class);

    public static String serialize(final Object object) {
        String result = null;

        XStream xstream = new XStream();
        try {
            result = URLEncoder.encode(
                    xstream.toXML(object), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("During serialization", e);
        }

        return result;
    }

    public static <T extends Object> T deserialize(
            final String serialized) {

        T result = null;

        XStream xstream = new XStream();
        try {
            result = (T) xstream.fromXML(
                    URLDecoder.decode(serialized, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOG.error("During deserialization", e);
        }

        return result;
    }
}
