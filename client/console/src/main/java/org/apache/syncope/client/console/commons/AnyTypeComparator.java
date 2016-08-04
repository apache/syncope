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
package org.apache.syncope.client.console.commons;

import java.io.Serializable;
import java.util.Comparator;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;

public class AnyTypeComparator implements Comparator<AnyTypeTO>, Serializable {

    private static final long serialVersionUID = -8227715253094467138L;

    @Override
    public int compare(final AnyTypeTO o1, final AnyTypeTO o2) {
        if (o1.getKind() == AnyTypeKind.USER) {
            return -1;
        }
        if (o2.getKind() == AnyTypeKind.USER) {
            return 1;
        }
        if (o1.getKind() == AnyTypeKind.GROUP) {
            return -1;
        }
        if (o2.getKind() == AnyTypeKind.GROUP) {
            return 1;
        }
        return ComparatorUtils.<String>naturalComparator().compare(o1.getKey(), o2.getKey());
    }
}
