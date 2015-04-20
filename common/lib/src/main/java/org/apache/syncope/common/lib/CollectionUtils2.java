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
package org.apache.syncope.common.lib;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.Transformer;

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

    /**
     * Transforms all elements from inputCollection with the given transformer
     * and adds them to the outputCollection if the provided predicate is verified.
     * <p/>
     * If the input collection or transformer is null, there is no change to the
     * output collection.
     *
     * @param <I> the type of object in the input collection
     * @param <O> the type of object in the output collection
     * @param <R> the output type of the transformer - this extends O.
     * @param inputCollection the collection to get the input from, may be null
     * @param transformer the transformer to use, may be null
     * @param predicate the predicate to use, may be null
     * @param outputCollection the collection to output into, may not be null if the inputCollection
     * and transformer are not null
     * @return the outputCollection with the transformed input added
     * @throws NullPointerException if the output collection is null and both, inputCollection and
     * transformer are not null
     */
    public static <I, O, R extends Collection<? super O>> R collect(final Iterable<? extends I> inputCollection,
            final Transformer<? super I, ? extends O> transformer, final Predicate<? super I> predicate,
            final R outputCollection) {

        if (inputCollection != null) {
            return collect(inputCollection.iterator(), transformer, predicate, outputCollection);
        }
        return outputCollection;
    }

    /**
     * Transforms all elements from the inputIterator with the given transformer
     * and adds them to the outputCollection.
     * <p/>
     * If the input iterator or transformer is null, there is no change to the
     * output collection.
     *
     * @param inputIterator the iterator to get the input from, may be null
     * @param transformer the transformer to use, may be null
     * @param predicate the predicate to use, may be null
     * @param outputCollection the collection to output into, may not be null if the inputCollection
     * and transformer are not null
     * @param <I> the type of object in the input collection
     * @param <O> the type of object in the output collection
     * @param <R> the output type of the transformer - this extends O.
     * @return the outputCollection with the transformed input added
     * @throws NullPointerException if the output collection is null and both, inputCollection and
     * transformer are not null
     */
    public static <I, O, R extends Collection<? super O>> R collect(final Iterator<? extends I> inputIterator,
            final Transformer<? super I, ? extends O> transformer, final Predicate<? super I> predicate,
            final R outputCollection) {

        if (inputIterator != null && transformer != null) {
            while (inputIterator.hasNext()) {
                final I item = inputIterator.next();
                final O value = transformer.transform(item);
                if (predicate == null || predicate.evaluate(item)) {
                    outputCollection.add(value);
                }
            }
        }
        return outputCollection;
    }

    /**
     * Gets elements in the input collection that match the predicate.
     * <p/>
     * A <code>null</code> collection or predicate matches no elements.
     *
     * @param <C> the type of object the {@link Iterable} contains
     * @param input the {@link Iterable} to get the input from, may be null
     * @param predicate the predicate to use, may be null
     * @return the matches for the predicate in the collection
     */
    public static <C> Collection<C> find(final Iterable<C> input, final Predicate<? super C> predicate) {
        Set<C> result = SetUtils.predicatedSet(new HashSet<C>(), predicate);
        if (input != null && predicate != null) {
            for (final C o : input) {
                if (predicate.evaluate(o)) {
                    result.add(o);
                }
            }
        }
        return SetUtils.unmodifiableSet(result);
    }

    private CollectionUtils2() {
        // private constructor for static utility class
    }
}
