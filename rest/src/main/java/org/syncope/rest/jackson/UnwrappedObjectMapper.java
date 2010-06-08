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
package org.syncope.rest.jackson;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 * TODO: there should be a simpler way to achieve this!
 */
public class UnwrappedObjectMapper extends ObjectMapper {

    /**
     * Unwraps the given value if it implements the Map interface and contains
     * only a single entry. Otherwise the value is returned unmodified.
     *
     * @param value
     *          the potential Map to unwrap
     * @return the unwrapped map or the original value
     */
    private Object unwrapMap(Object value) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.size() == 1) {
                return map.values().iterator().next();
            }
        }

        return value;
    }

    @Override
    public void writeValue(JsonGenerator jgen, Object value)
            throws IOException, JsonGenerationException, JsonMappingException {

        super.writeValue(jgen, unwrapMap(value));
    }

    @Override
    public void writeValue(JsonGenerator jgen, Object value,
            SerializationConfig config)
            throws IOException, JsonGenerationException, JsonMappingException {

        super.writeValue(jgen, unwrapMap(value), config);
    }

    @Override
    public void writeValue(File resultFile, Object value)
            throws IOException, JsonGenerationException, JsonMappingException {

        super.writeValue(resultFile, unwrapMap(value));
    }

    @Override
    public void writeValue(OutputStream out, Object value)
            throws IOException, JsonGenerationException, JsonMappingException {

        super.writeValue(out, unwrapMap(value));
    }

    @Override
    public void writeValue(Writer w, Object value)
            throws IOException, JsonGenerationException, JsonMappingException {

        super.writeValue(w, unwrapMap(value));
    }

    @Override
    public byte[] writeValueAsBytes(Object value)
            throws IOException, JsonGenerationException, JsonMappingException {

        return super.writeValueAsBytes(unwrapMap(value));
    }

    @Override
    public String writeValueAsString(Object value)
            throws IOException, JsonGenerationException, JsonMappingException {

        return super.writeValueAsString(unwrapMap(value));
    }
}
