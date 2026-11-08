/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the License.  You may not use this file except in compliance
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
package org.apache.syncope.core.persistence.api.entity;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.junit.jupiter.api.Test;

class AnyUtilsFactoryTest {

    @Test
    void getInstanceByAnyTypeKind() {
        final AnyUtils userAnyUtils = mock(AnyUtils.class);
        final AnyUtils groupAnyUtils = mock(AnyUtils.class);
        final AnyUtils anyObjectAnyUtils = mock(AnyUtils.class);
        final AnyUtilsFactory factory = new AnyUtilsFactory(userAnyUtils, groupAnyUtils, anyObjectAnyUtils);

        assertSame(userAnyUtils, factory.getInstance(AnyTypeKind.USER));
        assertSame(groupAnyUtils, factory.getInstance(AnyTypeKind.GROUP));
        assertSame(anyObjectAnyUtils, factory.getInstance(AnyTypeKind.ANY_OBJECT));
    }

    @Test
    void getInstanceByAny() {
        final AnyUtils userAnyUtils = mock(AnyUtils.class);
        final AnyUtils groupAnyUtils = mock(AnyUtils.class);
        final AnyUtils anyObjectAnyUtils = mock(AnyUtils.class);
        final AnyUtilsFactory factory = new AnyUtilsFactory(userAnyUtils, groupAnyUtils, anyObjectAnyUtils);

        assertSame(userAnyUtils, factory.getInstance(mock(User.class)));
        assertSame(groupAnyUtils, factory.getInstance(mock(Group.class)));
        assertSame(anyObjectAnyUtils, factory.getInstance(mock(AnyObject.class)));
    }

    @Test
    void getInstanceByInvalidAny() {
        final AnyUtilsFactory factory = new AnyUtilsFactory(
                mock(AnyUtils.class), mock(AnyUtils.class), mock(AnyUtils.class));

        assertThrows(IllegalArgumentException.class, () -> factory.getInstance(mock(Any.class)));
    }
}
