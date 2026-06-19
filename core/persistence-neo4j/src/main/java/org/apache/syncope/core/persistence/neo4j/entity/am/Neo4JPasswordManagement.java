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
package org.apache.syncope.core.persistence.neo4j.entity.am;

import jakarta.validation.constraints.NotNull;
import org.apache.syncope.common.lib.password.PasswordManagementConf;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.persistence.common.validation.PasswordManagementCheck;
import org.apache.syncope.core.persistence.neo4j.converters.PasswordManagementConfConverter;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractProvidedKeyNode;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4JPasswordManagement.NODE)
@PasswordManagementCheck
public class Neo4JPasswordManagement extends AbstractProvidedKeyNode implements PasswordManagement {

    private static final long serialVersionUID = 5457779846065079998L;

    public static final String NODE = "PasswordManagement";

    private String description;

    @NotNull
    private boolean enabled;

    @ConvertWith(converter = PasswordManagementConfConverter.class)
    private PasswordManagementConf jsonConf;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public PasswordManagementConf getConf() {
        return jsonConf;
    }

    @Override
    public void setConf(final PasswordManagementConf conf) {
        jsonConf = conf;
    }
}
