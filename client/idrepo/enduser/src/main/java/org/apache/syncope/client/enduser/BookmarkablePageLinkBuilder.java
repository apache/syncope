/*
 *  Copyright (C) 2020 Tirasa (info@tirasa.net)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.syncope.client.enduser;

import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;

public final class BookmarkablePageLinkBuilder {

    public static <T extends BasePage> BookmarkablePageLink<T> build(
            final String key, final Class<T> defaultPageClass) {

        return build(key, key, defaultPageClass);
    }

    public static <T extends BasePage> BookmarkablePageLink<T> build(
            final String key, final String id, final Class<T> defaultPageClass) {

        @SuppressWarnings("unchecked")
        Class<T> pageClass = (Class<T>) SyncopeEnduserApplication.get().getPageClass(key);
        return new BookmarkablePageLink<>(
                id,
                pageClass == null ? defaultPageClass : pageClass);
    }

    private BookmarkablePageLinkBuilder() {
        // private constructor for static utility class
    }
}
