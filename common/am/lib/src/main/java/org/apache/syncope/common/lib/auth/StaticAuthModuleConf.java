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
package org.apache.syncope.common.lib.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.xml.bind.annotation.XmlType;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;

@XmlRootElement(name = "staticAuthModuleConf")
@XmlType
public class StaticAuthModuleConf extends AbstractAuthModuleConf {

    private static final long serialVersionUID = -7775771400318503131L;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    private final Map<String, String> users = new HashMap<>();

    @XmlElementWrapper(name = "users")
    @XmlElement(name = "user")
    @JsonProperty("users")
    public Map<String, String> getUsers() {
        return users;
    }

}
