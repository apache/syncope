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
package org.apache.syncope.core.persistence.neo4j.entity;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.syncope.core.persistence.api.entity.Entity;

public class SortedSetList<E extends Entity, R extends Neo4jSortedRelationsihip<E>> implements List<E> {

    private class SortedSetListIterator implements Iterator<E> {

        private final Iterator<R> ditor;

        SortedSetListIterator(final Iterator<R> ditor) {
            this.ditor = ditor;
        }

        @Override
        public boolean hasNext() {
            return ditor.hasNext();
        }

        @Override
        public E next() {
            return ditor.next().getEntity();
        }

        @Override
        public void remove() {
            ditor.remove();
        }
    }

    private class SortedSetListSplitIterator implements Spliterator<E> {

        private final Spliterator<R> ditor;

        SortedSetListSplitIterator(final Spliterator<R> ditor) {
            this.ditor = ditor;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super E> action) {
            return ditor.tryAdvance(t -> action.accept(t.getEntity()));
        }

        @Override
        public Spliterator<E> trySplit() {
            return new SortedSetListSplitIterator(ditor.trySplit());
        }

        @Override
        public long estimateSize() {
            return ditor.estimateSize();
        }

        @Override
        public int characteristics() {
            return ditor.characteristics();
        }

        @Override
        public Comparator<? super E> getComparator() {
            throw new UnsupportedOperationException("NOT FOR NOW");
        }
    }

    private final SortedSet<R> delegate;

    private final BiFunction<Integer, E, R> builder;

    public SortedSetList(
            final SortedSet<R> delegate,
            final BiFunction<Integer, E, R> builder) {

        this.delegate = delegate;
        this.builder = builder;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return delegate.stream().anyMatch(e -> e.getEntity().equals(o));
    }

    @Override
    public Iterator<E> iterator() {
        return new SortedSetListIterator(delegate.iterator());
    }

    @Override
    public Spliterator<E> spliterator() {
        return new SortedSetListSplitIterator(delegate.spliterator());
    }

    @Override
    public Object[] toArray() {
        return delegate.stream().
                sorted(Comparator.comparing(Neo4jSortedRelationsihip::getIndex)).
                map(Neo4jSortedRelationsihip::getEntity).toList().toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return delegate.stream().
                sorted(Comparator.comparing(Neo4jSortedRelationsihip::getIndex)).
                map(Neo4jSortedRelationsihip::getEntity).toList().toArray(a);
    }

    @Override
    public boolean add(final E e) {
        return delegate.add(builder.apply(
                delegate.stream().map(Neo4jSortedRelationsihip::getIndex).
                        max(Comparator.naturalOrder()).orElse(0) + 1,
                e));
    }

    @Override
    public boolean remove(final Object o) {
        return delegate.removeIf(o::equals);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public E get(final int index) {
        if (index < 0 || index >= delegate.size()) {
            throw new IndexOutOfBoundsException();
        }

        int idx = 0;
        for (Iterator<R> itor = delegate.iterator();
                idx <= index && itor.hasNext(); idx++) {

            Neo4jSortedRelationsihip<E> next = itor.next();
            if (idx == index) {
                return next.getEntity();
            }
        }

        throw new IndexOutOfBoundsException();
    }

    @Override
    public Stream<E> stream() {
        return delegate.stream().map(Neo4jSortedRelationsihip::getEntity);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public E set(final int index, final E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(final int index, final E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(final int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lastIndexOf(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<E> listIterator(final int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<E> subList(final int fromIndex, final int toIndex) {
        throw new UnsupportedOperationException();
    }
}
