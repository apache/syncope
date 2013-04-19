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
package org.apache.syncope.common.to;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.AbstractBaseBean;

/**
 * Mimic ConnId's ObjectClass information.
 */
@XmlRootElement(name = "connIdObjectClass")
@XmlType
public class ConnIdObjectClassTO extends AbstractBaseBean {

    private static final long serialVersionUID = 5802458031138859994L;

    @XmlEnum
    public enum DefaultType {

        ACCOUNT("__ACCOUNT__"),
        GROUP("__GROUP__");

        private String specialName;

        private DefaultType(final String specialName) {
            this.specialName = specialName;
        }

        public String getSpecialName() {
            return specialName;
        }
    }

    public static ConnIdObjectClassTO ACCOUNT = new ConnIdObjectClassTO(DefaultType.ACCOUNT.getSpecialName());

    public static ConnIdObjectClassTO GROUP = new ConnIdObjectClassTO(DefaultType.GROUP.getSpecialName());

    private String type;

    public ConnIdObjectClassTO() {
    }

    public ConnIdObjectClassTO(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
