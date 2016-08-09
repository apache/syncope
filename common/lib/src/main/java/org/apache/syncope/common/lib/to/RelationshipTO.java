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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "relationship")
@XmlType
public class RelationshipTO extends AbstractBaseBean {

    private static final long serialVersionUID = 360672942026613929L;

    public static class Builder {

        private final RelationshipTO instance = new RelationshipTO();

        public Builder type(final String type) {
            instance.setType(type);
            return this;
        }

        public Builder right(final String rightType, final String rightKey) {
            instance.setRightType(rightType);
            instance.setRightKey(rightKey);
            return this;
        }

        public RelationshipTO build() {
            return instance;
        }
    }

    private String type;

    private String rightType;

    private String rightKey;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getRightType() {
        return rightType;
    }

    public void setRightType(final String rightType) {
        this.rightType = rightType;
    }

    public String getRightKey() {
        return rightKey;
    }

    public void setRightKey(final String rightKey) {
        this.rightKey = rightKey;
    }

}
