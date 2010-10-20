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

package org.syncope.console.commons;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.syncope.client.to.SchemaTO;

/**
 * Wrapper for User's Schema - Attribute.
 */
public class SchemaWrapper {

    SchemaTO schemaTO;
    List<String> values;

    public SchemaWrapper(SchemaTO schemaTO) {
        this.schemaTO = schemaTO;
        values = new ArrayList<String>();

        values.add("");
    }

    public SchemaTO getSchemaTO() {
        return schemaTO;
    }

    public void setSchemaTO(SchemaTO schemaTO) {
        this.schemaTO = schemaTO;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public void setValues(Set<String> values) {
        this.values = new ArrayList<String>();
        for (String value : values) {
            this.values.add(value);
        }
    }
}
