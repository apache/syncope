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
package org.apache.syncope.core.persistence.jpa.attrvalue.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.tika.Tika;

public class BinaryValidator extends AbstractValidator {

    private static final long serialVersionUID = 1344152444666540361L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Tika TIKA = new Tika();

    static {
        TIKA.setMaxStringLength(-1);
    }

    @Override
    protected void doValidate(final PlainAttrValue attrValue) {
        // check Binary schemas MIME Type mismatches
        if (attrValue.getBinaryValue() != null) {
            byte[] binaryValue = attrValue.getBinaryValue();
            String mimeType = TIKA.detect(binaryValue);

            boolean valid = true;
            if (!mimeType.equals(attrValue.getAttr().getSchema().getMimeType())) {
                if (MediaType.TEXT_PLAIN.equals(mimeType)
                        && MediaType.APPLICATION_JSON.equals(attrValue.getAttr().getSchema().getMimeType())) {

                    String decoded = new String(binaryValue).trim();
                    valid = (decoded.startsWith("{") && decoded.endsWith("}"))
                            || (decoded.startsWith("[") && decoded.endsWith("]"))
                            && isValidJSON(decoded);
                } else {
                    valid = false;
                }
            }
            if (!valid) {
                throw new InvalidPlainAttrValueException(
                        "Found MIME type: '" + mimeType + "', expecting: '"
                        + attrValue.getAttr().getSchema().getMimeType() + '\'');
            }
        }
    }

    private static boolean isValidJSON(final String value) {
        try {
            MAPPER.readTree(value);
            return true;
        } catch (IOException e) {
            LOG.debug("Invalid JSON string: {}", value, e);
            return false;
        }
    }
}
