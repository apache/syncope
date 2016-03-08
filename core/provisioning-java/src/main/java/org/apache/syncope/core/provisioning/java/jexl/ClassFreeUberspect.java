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
package org.apache.syncope.core.provisioning.java.jexl;

import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;

class ClassFreeUberspect extends Uberspect {

    ClassFreeUberspect() {
        super(null, null);
    }

    @Override
    public JexlPropertyGet getPropertyGet(final Object obj, final Object identifier) {
        return "class".equals(identifier) ? null : super.getPropertyGet(obj, identifier);
    }

    @Override
    public JexlMethod getMethod(final Object obj, final String method, final Object... args) {
        return "getClass".equals(method) ? null : super.getMethod(obj, method, args);
    }

}
