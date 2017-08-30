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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.ImplementationType;

@XmlRootElement(name = "javaImplInfo")
@XmlType
public class JavaImplInfo extends AbstractBaseBean {

    private static final long serialVersionUID = 4036793959111794959L;

    private ImplementationType type;

    private final Set<String> classes = new HashSet<>();

    public ImplementationType getType() {
        return type;
    }

    public void setType(final ImplementationType type) {
        this.type = type;
    }

    @XmlElementWrapper(name = "classes")
    @XmlElement(name = "class")
    @JsonProperty("classes")
    public Set<String> getClasses() {
        return classes;
    }

}
