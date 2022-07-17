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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.BaseBean;

/**
 * Metadata description of ConnId ObjectClass.
 */
public class ConnIdObjectClass implements BaseBean {

    private static final long serialVersionUID = -3719658595689434648L;

    private String type;

    private boolean container;

    private boolean auxiliary;

    private final List<PlainSchemaTO> attributes = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public boolean isContainer() {
        return container;
    }

    public void setContainer(final boolean container) {
        this.container = container;
    }

    public boolean isAuxiliary() {
        return auxiliary;
    }

    public void setAuxiliary(final boolean auxiliary) {
        this.auxiliary = auxiliary;
    }

    @JacksonXmlElementWrapper(localName = "attributes")
    @JacksonXmlProperty(localName = "attribute")
    public List<PlainSchemaTO> getAttributes() {
        return attributes;
    }
}
