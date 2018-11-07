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

import java.util.Date;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "abstractHistoryConf")
@XmlType
@XmlSeeAlso({ ConnInstanceHistoryConfTO.class, ResourceHistoryConfTO.class })
public abstract class AbstractHistoryConf implements EntityTO {

    private static final long serialVersionUID = -8001640160293506651L;

    private String key;

    private String creator;

    private Date creation;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(final String creator) {
        this.creator = creator;
    }

    public Date getCreation() {
        return creation == null
                ? null
                : new Date(creation.getTime());
    }

    public void setCreation(final Date creation) {
        this.creation = creation == null
                ? null
                : new Date(creation.getTime());
    }
}
