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
import org.apache.syncope.common.lib.types.ClientExceptionType;

public class ErrorTO implements BaseBean {

    private static final long serialVersionUID = 2435764161719225927L;

    private int status;

    private ClientExceptionType type;

    private final List<String> elements = new ArrayList<>();

    public int getStatus() {
        return status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public ClientExceptionType getType() {
        return type;
    }

    public void setType(final ClientExceptionType type) {
        this.type = type;
    }

    @JacksonXmlElementWrapper(localName = "elements")
    @JacksonXmlProperty(localName = "element")
    public List<String> getElements() {
        return elements;
    }
}
