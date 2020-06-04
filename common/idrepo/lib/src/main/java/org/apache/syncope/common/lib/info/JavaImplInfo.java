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
package org.apache.syncope.common.lib.info;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.BaseBean;

public class JavaImplInfo implements BaseBean {

    private static final long serialVersionUID = 4036793959111794959L;

    private String type;

    private final Set<String> classes = new HashSet<>();

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @JacksonXmlElementWrapper(localName = "classes")
    @JacksonXmlProperty(localName = "class")
    public Set<String> getClasses() {
        return classes;
    }
}
