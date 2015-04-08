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
package org.apache.syncope.core.misc;

import java.util.Iterator;

public final class CollectionUtils2 {

    /**
     * Returns the next element in <tt>iterator</tt> or <tt>defaultValue</tt> if the iterator is empty.
     *
     * @param defaultValue the default value to return if the iterator is empty
     * @return the next element of <tt>iterator</tt> or the default value
     */
    public static <T> T getNext(final Iterator<? extends T> iterator, final T defaultValue) {
        return iterator.hasNext() ? iterator.next() : defaultValue;
    }

    /**
     * Returns the first element in <tt>iterable</tt> or <tt>defaultValue</tt> if the iterable is empty.
     *
     * <p/>
     * If no default value is desired (and the caller instead wants a {@link java.util.NoSuchElementException} to be
     * thrown), it is recommended that <tt>iterable.iterator().next()}</tt> is used instead.
     *
     * @param defaultValue the default value to return if the iterable is empty
     * @return the first element of <tt>iterable</tt> or the default value
     */
    public static <T> T getFirst(final Iterable<? extends T> iterable, final T defaultValue) {
        return getNext(iterable.iterator(), defaultValue);
    }

    /**
     * Returns the first element in <tt>iterable</tt> or <tt>null</tt> if the iterable is empty.
     *
     * @return the first element of <tt>iterable</tt> or <tt>null</tt>
     */
    public static <T> T getFirstOrNull(final Iterable<? extends T> iterable) {
        return getNext(iterable.iterator(), null);
    }

    private CollectionUtils2() {
        // private constructor for static utility class
    }
}
