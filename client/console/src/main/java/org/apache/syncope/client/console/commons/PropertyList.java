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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;

public abstract class PropertyList<T> implements List<String> {

    @Override
    public boolean add(final String item) {
        final List<String> list = getEnumValuesAsList(getValues());
        final boolean res = list.add(item);
        setValues(list);
        return res;
    }

    @Override
    public int size() {
        return getEnumValuesAsList(getValues()).size();
    }

    @Override
    public boolean isEmpty() {
        return getEnumValuesAsList(getValues()).isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return getEnumValuesAsList(getValues()).contains(o);
    }

    @Override
    public Iterator<String> iterator() {
        return getEnumValuesAsList(getValues()).iterator();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean remove(final Object o) {
        final List<String> list = getEnumValuesAsList(getValues());
        final boolean res = list.remove(o);
        setValues(list);
        return res;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return getEnumValuesAsList(getValues()).containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends String> c) {
        final List<String> list = getEnumValuesAsList(getValues());
        boolean res = list.addAll(c);
        setValues(list);
        return res;
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends String> c) {
        final List<String> list = getEnumValuesAsList(getValues());
        final boolean res = list.addAll(index, c);
        setValues(list);
        return res;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        final List<String> list = getEnumValuesAsList(getValues());
        final boolean res = list.removeAll(c);
        setValues(list);
        return res;
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        final List<String> list = getEnumValuesAsList(getValues());
        final boolean res = list.retainAll(c);
        setValues(list);
        return res;
    }

    @Override
    public void clear() {
        final List<String> list = getEnumValuesAsList(getValues());
        list.clear();
        setValues(list);
    }

    @Override
    public String get(final int index) {
        final List<String> list = getEnumValuesAsList(getValues());
        return list.get(index);
    }

    @Override
    public String set(final int index, final String element) {
        final List<String> list = getEnumValuesAsList(getValues());
        final String res = list.set(index, element);
        setValues(list);
        return res;
    }

    @Override
    public void add(final int index, final String element) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String remove(final int index) {
        final List<String> list = getEnumValuesAsList(getValues());
        final String res = list.remove(index);
        setValues(list);
        return res;
    }

    @Override
    public int indexOf(final Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int lastIndexOf(final Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ListIterator<String> listIterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ListIterator<String> listIterator(final int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> subList(final int fromIndex, final int toIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static String getEnumValuesAsString(final List<String> enumerationValues) {
        return enumerationValues.stream().
                map(v -> StringUtils.isEmpty(v) ? StringUtils.EMPTY : v.trim()).
                collect(Collectors.joining(SyncopeConstants.ENUM_VALUES_SEPARATOR));
    }

    public static List<String> getEnumValuesAsList(final String enumerationValues) {
        final List<String> values = new ArrayList<>();
        if (StringUtils.isNotBlank(enumerationValues)) {
            for (String value : enumerationValues.split(SyncopeConstants.ENUM_VALUES_SEPARATOR)) {
                values.add(value.trim());
            }
            if (enumerationValues.trim().endsWith(SyncopeConstants.ENUM_VALUES_SEPARATOR)) {
                values.add(StringUtils.EMPTY);
            }
        } else {
            values.add(StringUtils.EMPTY);
        }

        return values;
    }

    public abstract String getValues();

    public abstract void setValues(List<String> list);
}
