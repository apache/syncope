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
package org.apache.syncope.core.persistence.jpa.upgrade;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;

/**
 * Compared to the original {@link SchemaTool}, this class' methods keep consistent behavior when either {@code _writer}
 * is null or not.
 */
public class WiserSchemaTool extends SchemaTool {

    public WiserSchemaTool(final JDBCConfiguration conf) {
        super(conf);
    }

    public WiserSchemaTool(final JDBCConfiguration conf, final String action) {
        super(conf, action);
    }

    @Override
    protected void buildSchema(
            final SchemaGroup db,
            final SchemaGroup repos,
            final boolean considerDatabaseState) throws SQLException {

        // add sequences
        if (getSequences()) {
            for (Schema schema : repos.getSchemas()) {
                for (Sequence seq : schema.getSequences()) {
                    if (considerDatabaseState && db.findSequence(schema, seq.getQualifiedPath()) != null) {
                        continue;
                    }

                    if (createSequence(seq)) {
                        Schema dbSchema = Optional.ofNullable(db.getSchema(seq.getSchemaIdentifier())).
                                orElseGet(() -> db.addSchema(seq.getSchemaIdentifier()));
                        dbSchema.importSequence(seq);
                    } else {
                        _log.warn(_loc.get("add-seq", seq));
                    }
                }
            }
        }

        // order is important in this method; start with columns
        DBIdentifier defaultSchemaName = DBIdentifier.newSchema(_dict.getDefaultSchemaName());
        for (Schema schema : repos.getSchemas()) {
            for (Table tab : schema.getTables()) {
                Table dbTable = considerDatabaseState
                        ? db.findTable(schema, tab.getQualifiedPath(), defaultSchemaName)
                        : null;
                if (dbTable != null) {
                    for (Column col : tab.getColumns()) {
                        Column dbCol = dbTable.getColumn(col.getIdentifier());
                        if (dbCol == null) {
                            if (addColumn(col)) {
                                dbTable.importColumn(col);
                            } else {
                                _log.warn(_loc.get("add-col", col, tab));
                            }
                        } else if (!col.equalsColumn(_dict, dbCol)) {
                            _log.warn(_loc.get("bad-col",
                                    new Object[] { dbCol, dbTable, dbCol.getDescription(), col.getDescription() }));
                        }
                    }
                }
            }
        }

        // primary keys
        if (getPrimaryKeys()) {
            for (Schema schema : repos.getSchemas()) {
                for (Table tab : schema.getTables()) {
                    PrimaryKey pk = tab.getPrimaryKey();
                    Table dbTable = considerDatabaseState
                            ? db.findTable(schema, tab.getQualifiedPath())
                            : null;
                    if (pk != null && !pk.isLogical() && dbTable != null) {
                        if (dbTable.getPrimaryKey() == null && addPrimaryKey(pk)) {
                            dbTable.importPrimaryKey(pk);
                        } else if (dbTable.getPrimaryKey() == null) {
                            _log.warn(_loc.get("add-pk", pk, tab));
                        } else if (!pk.equalsPrimaryKey(dbTable.getPrimaryKey())) {
                            _log.warn(_loc.get("bad-pk",
                                    dbTable.getPrimaryKey(), dbTable));
                        }
                    }
                }
            }
        }

        // tables
        Set<Table> newTables = new HashSet<>();
        for (Schema schema : repos.getSchemas()) {
            for (Table tab : schema.getTables()) {
                if (considerDatabaseState && db.findTable(schema, tab.getQualifiedPath()) != null) {
                    continue;
                }

                if (createTable(tab)) {
                    newTables.add(tab);
                    Schema dbSchema = Optional.ofNullable(db.getSchema(tab.getSchemaIdentifier())).
                            orElseGet(() -> db.addSchema(tab.getSchemaIdentifier()));
                    dbSchema.importTable(tab);
                } else {
                    _log.warn(_loc.get("add-table", tab));
                }
            }
        }

        // indexes
        for (Schema schema : repos.getSchemas()) {
            Table[] tabs = schema.getTables();
            for (Table tab : tabs) {
                // create indexes on new tables even if indexes have been turned off
                if (!getIndexes() && !newTables.contains(tab)) {
                    continue;
                }

                Table dbTable = considerDatabaseState
                        ? db.findTable(schema, tab.getQualifiedPath())
                        : null;
                if (dbTable != null) {
                    for (Index idx : tab.getIndexes()) {
                        Index dbIdx = findIndex(dbTable, idx);
                        if (dbIdx == null) {
                            if (createIndex(idx, dbTable, tab.getUniques())) {
                                dbTable.importIndex(idx);
                            } else {
                                _log.warn(_loc.get("add-index", idx, tab));
                            }
                        } else if (!idx.equalsIndex(dbIdx)) {
                            _log.warn(_loc.get("bad-index", dbIdx, dbTable));
                        }
                    }
                }
            }
        }

        // Unique Constraints on group of columns
        for (Schema schema : repos.getSchemas()) {
            for (Table tab : schema.getTables()) {
                // create unique constraints only on new tables
                if (!newTables.contains(tab)) {
                    continue;
                }
                Unique[] uniques = tab.getUniques();
                if (uniques == null || uniques.length == 0) {
                    continue;
                }
                Table dbTable = considerDatabaseState
                        ? db.findTable(tab)
                        : null;
                if (dbTable == null) {
                    continue;
                }
                for (Unique unique : uniques) {
                    dbTable.importUnique(unique);
                }
            }
        }

        // foreign keys
        for (Schema schema : repos.getSchemas()) {
            for (Table tab : schema.getTables()) {
                // create foreign keys on new tables even if fks have been turned off
                if (!getForeignKeys() && !newTables.contains(tab)) {
                    continue;
                }

                Table dbTable = considerDatabaseState
                        ? db.findTable(schema, tab.getQualifiedPath())
                        : null;
                if (dbTable != null) {
                    for (ForeignKey fk : tab.getForeignKeys()) {
                        if (!fk.isLogical()) {
                            ForeignKey dbFk = findForeignKey(dbTable, fk);
                            if (dbFk == null) {
                                if (addForeignKey(fk)) {
                                    dbTable.importForeignKey(fk);
                                } else {
                                    _log.warn(_loc.get("add-fk", fk, tab));
                                }
                            } else if (!fk.equalsForeignKey(dbFk)) {
                                _log.warn(_loc.get("bad-fk", dbFk, dbTable));
                            }
                        }
                    }
                }
            }
        }
    }
}
