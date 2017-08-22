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
package org.apache.syncope.common.lib.to;

import org.apache.syncope.common.lib.AbstractBaseBean;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "attribute")
@XmlType
public class AttrTO extends AbstractBaseBean {

    private static final long serialVersionUID = 4941691338796323623L;

    public static class Builder {

        private final AttrTO instance = new AttrTO();

        public Builder schema(final String schema) {
            instance.setSchema(schema);
            return this;
        }

        public Builder schemaInfo(final AbstractSchemaTO schemaInfo) {
            instance.schemaInfo = schemaInfo;
            return this;
        }

        public Builder value(final String value) {
            instance.getValues().add(value);
            return this;
        }

        public Builder values(final String... values) {
            for (String value : values) {
                instance.getValues().add(value);
            }
            return this;
        }

        public Builder values(final Collection<String> values) {
            instance.getValues().addAll(values);
            return this;
        }

        public AttrTO build() {
            return instance;
        }
    }

    /**
     * (Optional) schema information for this attribute.
     */
    private AbstractSchemaTO schemaInfo;

    /**
     * Name of the schema that this attribute is referring to.
     */
    private String schema;

    /**
     * Set of (string) values of this attribute.
     */
    private final List<String> values = new ArrayList<>();

    /**
     * @return schema information for this attribute; may be {@code NULL}
     */
    public AbstractSchemaTO getSchemaInfo() {
        return schemaInfo;
    }

    public void setSchemaInfo(final AbstractSchemaTO schemaInfo) {
        this.schemaInfo = schemaInfo;
    }

    /**
     * @return the name of the schema that this attribute is referring to
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @param schema name to be set
     */
    @PathParam("schema")
    public void setSchema(final String schema) {
        this.schema = schema;

    }

    /**
     * @return attribute values as strings
     */
    @XmlElementWrapper(name = "values")
    @XmlElement(name = "value")
    @JsonProperty("values")
    public List<String> getValues() {
        return values;
    }
}
