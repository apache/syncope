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
package org.apache.syncope.client.enduser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.syncope.client.enduser.pages.Dashboard;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.junit.jupiter.api.Test;

public class BookmarkablePageLinkBuilderTest extends AbstractTest {

    @Test
    void buildWithKeyAndDefaultPageClass() {
        BookmarkablePageLink<Dashboard> link = BookmarkablePageLinkBuilder.build("nonexistent", Dashboard.class);

        assertNotNull(link);
        assertSame(Dashboard.class, link.getPageClass());
    }

    @Test
    void buildWithKeyIdAndDefaultPageClass() {
        BookmarkablePageLink<Dashboard> link = BookmarkablePageLinkBuilder.
                build("nonexistent", "myId", Dashboard.class);

        assertNotNull(link);
        assertEquals("myId", link.getId());
        assertSame(Dashboard.class, link.getPageClass());
    }
}
