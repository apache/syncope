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
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SortedSetList implements List<Neo4jImplementation> {

    private static class SortedSetListIterator implements Iterator<Neo4jImplementation> {

        private final Iterator<Neo4jImplementationRelationship> ditor;

        SortedSetListIterator(final Iterator<Neo4jImplementationRelationship> ditor) {
            this.ditor = ditor;
        }

        @Override
        public boolean hasNext() {
            return ditor.hasNext();
        }

        @Override
        public Neo4jImplementation next() {
            return ditor.next().getImplementation();
        }

        @Override
        public void remove() {
            ditor.remove();
        }
    }

    private static class SortedSetListSplitIterator implements Spliterator<Neo4jImplementation> {

        private final Spliterator<Neo4jImplementationRelationship> ditor;

        SortedSetListSplitIterator(final Spliterator<Neo4jImplementationRelationship> ditor) {
            this.ditor = ditor;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super Neo4jImplementation> action) {
            return ditor.tryAdvance(t -> action.accept(t.getImplementation()));
        }

        @Override
        public Spliterator<Neo4jImplementation> trySplit() {
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
        public Comparator<? super Neo4jImplementation> getComparator() {
            throw new UnsupportedOperationException("NOT FOR NOW");
        }
    }

    private final SortedSet<Neo4jImplementationRelationship> delegate;

    public SortedSetList(final SortedSet<Neo4jImplementationRelationship> delegate) {
        this.delegate = delegate;
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
        return delegate.stream().anyMatch(e -> e.getImplementation().equals(o));
    }

    @Override
    public Iterator<Neo4jImplementation> iterator() {
        return new SortedSetListIterator(delegate.iterator());
    }

    @Override
    public Spliterator<Neo4jImplementation> spliterator() {
        return new SortedSetListSplitIterator(delegate.spliterator());
    }

    @Override
    public Object[] toArray() {
        return delegate.stream().
                sorted(Comparator.comparing(Neo4jImplementationRelationship::getIndex)).
                map(Neo4jImplementationRelationship::getImplementation).toList().toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return delegate.stream().
                sorted(Comparator.comparing(Neo4jImplementationRelationship::getIndex)).
                map(Neo4jImplementationRelationship::getImplementation).toList().toArray(a);
    }

    @Override
    public boolean add(final Neo4jImplementation e) {
        return delegate.add(new Neo4jImplementationRelationship(
                delegate.stream().map(Neo4jImplementationRelationship::getIndex).
                        max(Comparator.naturalOrder()).orElse(0) + 1,
                e));
    }

    @Override
    public boolean remove(final Object o) {
        if (o instanceof Neo4jImplementation impl) {
            return delegate.removeIf(impl::equals);
        }
        return false;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final Collection<? extends Neo4jImplementation> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends Neo4jImplementation> c) {
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
    public Neo4jImplementation get(final int index) {
        if (index < 0 || index >= delegate.size()) {
            throw new IndexOutOfBoundsException();
        }

        int idx = 0;
        for (Iterator<Neo4jImplementationRelationship> itor = delegate.iterator();
                idx <= index && itor.hasNext();
                idx++) {

            Neo4jImplementationRelationship next = itor.next();
            if (idx == index) {
                return next.getImplementation();
            }
        }

        throw new IndexOutOfBoundsException();
    }

    @Override
    public Stream<Neo4jImplementation> stream() {
        return delegate.stream().map(Neo4jImplementationRelationship::getImplementation);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public Neo4jImplementation set(final int index, final Neo4jImplementation element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(final int index, final Neo4jImplementation element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Neo4jImplementation remove(final int index) {
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
    public ListIterator<Neo4jImplementation> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<Neo4jImplementation> listIterator(final int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Neo4jImplementation> subList(final int fromIndex, final int toIndex) {
        throw new UnsupportedOperationException();
    }
}
