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
package org.apache.syncope.core.persistence.jpa.openjpa;

import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.meta.strats.AbstractValueHandler;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.meta.JavaTypes;

public class LocaleValueHandler extends AbstractValueHandler {

    private static final long serialVersionUID = 487849441377630981L;

    private static final LocaleValueHandler INSTANCE = new LocaleValueHandler();

    public static LocaleValueHandler getInstance() {
        return INSTANCE;
    }

    @Override
    @Deprecated
    public Column[] map(final ValueMapping vm, final String name, final ColumnIO io, final boolean adapt) {
        DBDictionary dict = vm.getMappingRepository().getDBDictionary();
        DBIdentifier colName = DBIdentifier.newColumn(
                name, Optional.ofNullable(dict).filter(DBDictionary::delimitAll).isPresent());
        return map(colName);
    }

    public static Column[] map(final DBIdentifier name) {
        Column col = new Column();
        col.setIdentifier(name);
        col.setJavaType(JavaTypes.STRING);
        return new Column[] { col };
    }

    @Override
    public Object toDataStoreValue(final ValueMapping vm, final Object val, final JDBCStore store) {
        return Optional.ofNullable(val).map(o -> ((Locale) o).toString()).orElse(null);
    }

    @Override
    public Object toObjectValue(final ValueMapping vm, final Object val) {
        return Optional.ofNullable(val).map(o -> LocaleUtils.toLocale((String) o)).orElse(null);
    }
}
