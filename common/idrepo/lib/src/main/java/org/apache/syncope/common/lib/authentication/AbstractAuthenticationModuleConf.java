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
package org.apache.syncope.common.lib.authentication;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.StringUtils;

@XmlType
@XmlSeeAlso({ DefaultAuthenticationModuleConf.class })
public abstract class AbstractAuthenticationModuleConf implements Serializable, AuthenticationModuleConf {

    private static final long serialVersionUID = 4153200197344709778L;

    private String name;
    
    private int authenticationLevel;

    public AbstractAuthenticationModuleConf() {
        this(StringUtils.EMPTY);
        setName(getClass().getName());
    }

    public AbstractAuthenticationModuleConf(final String name) {
        super();
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }

    public final void setName(final String name) {
        this.name = name;
    }

    @Override
    public int getAuthenticationLevel() {
        return authenticationLevel;
    }

    public void setAuthenticationLevel(final int authenticationLevel) {
        this.authenticationLevel = authenticationLevel;
    }
}
