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
package org.apache.syncope.common.lib.types;

import javax.xml.bind.annotation.XmlEnum;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;

@XmlEnum
public enum AnyTypeKind {

    USER(UserTO.class, UserUR.class),
    GROUP(GroupTO.class, GroupUR.class),
    ANY_OBJECT(AnyObjectTO.class, AnyObjectUR.class);

    private final Class<? extends AnyTO> toClass;

    private final Class<? extends AnyUR> reqClass;

    AnyTypeKind(final Class<? extends AnyTO> toClass, final Class<? extends AnyUR> reqClass) {
        this.toClass = toClass;
        this.reqClass = reqClass;
    }

    public Class<? extends AnyTO> getTOClass() {
        return toClass;
    }

    public Class<? extends AnyUR> getURClass() {
        return reqClass;
    }

    public static AnyTypeKind fromTOClass(final Class<? extends AnyTO> clazz) {
        return UserTO.class.isAssignableFrom(clazz)
                ? AnyTypeKind.USER
                : GroupTO.class.isAssignableFrom(clazz)
                ? AnyTypeKind.GROUP
                : AnyTypeKind.ANY_OBJECT;
    }
}
