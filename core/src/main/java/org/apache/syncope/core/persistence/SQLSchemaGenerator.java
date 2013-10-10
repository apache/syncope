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
package org.apache.syncope.core.persistence;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.Entity;
import org.apache.commons.io.FileUtils;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.meta.MappingTool;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.meta.ClassArgParser;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.meta.MetaDataRepository;

public final class SQLSchemaGenerator {

    private static final String OPTION_PROPERTIES_FILE = "propertiesFile";

    private static final String OPTION_CONNECTION_PROPERTIES = "ConnectionProperties";

    private static final String OPTION_CONNECTION_DRIVER_NAME = "ConnectionDriverName";

    private static final String OPTION_SQL_ACTION = "schemaAction";

    private static final String OPTION_SQL_FILE = "sqlFile";

    /**
     * Locates and returns a list of class files found under specified class directory.
     *
     * @param base base class directory
     * @return list of class files
     */
    private static List<File> findEntityClassFiles(final String base) {
        File baseDir = new File(base);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw new IllegalArgumentException(baseDir + " not found or not a directory");
        }

        @SuppressWarnings("unchecked")
        Iterator<File> itor = FileUtils.iterateFiles(baseDir, new String[] {"class"}, true);
        List<File> entityClasses = new ArrayList<File>();
        while (itor.hasNext()) {
            entityClasses.add(itor.next());
        }

        return entityClasses;
    }

    /**
     * @param cls the Class to check
     * @return <code>true</code> if the given Class cls implements the interface {@link PersistenceCapable}
     */
    private static boolean implementsPersistenceCapable(final Class<?> cls) {
        boolean isPersistenceCapable = false;
        Class<?>[] interfaces = cls.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].getName().equals(PersistenceCapable.class.getName())) {
                isPersistenceCapable = true;
                break;
            }
        }

        return isPersistenceCapable;
    }

    /**
     * Filter out all classes which are not PersistenceCapable.
     * This is needed since the MappingTool fails if it gets non-persistent capable classes.
     *
     * @param files List with classPath Files; non persistence classes will be removed
     * @param opts filled configuration Options
     */
    private static void filterPersistenceCapable(final List<File> files, final Options opts) {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        Configurations.populateConfiguration(conf, opts);
        MetaDataRepository repo = conf.newMetaDataRepositoryInstance();
        ClassArgParser cap = repo.getMetaDataFactory().newClassArgParser();

        Iterator<File> fileIt = files.iterator();
        while (fileIt.hasNext()) {
            File classPath = fileIt.next();

            Class[] classes = cap.parseTypes(classPath.getAbsolutePath());
            if (classes == null) {
                System.out.println("Found no classes for " + classPath.getAbsolutePath());
            } else {
                for (int i = 0; i < classes.length; i++) {
                    Class<?> cls = classes[i];

                    if (cls.getAnnotation(Entity.class) == null && !implementsPersistenceCapable(cls)) {
                        fileIt.remove();
                    }
                }
            }
        }
    }

    /**
     * @param files List of files
     * @return the paths of the given files as String[]
     */
    private static String[] getFilePaths(final List<File> files) {
        String[] args = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);

            args[ i] = file.getAbsolutePath();
        }
        return args;
    }

    /**
     * Processes a list of class file resources and perform the proper mapping action.
     */
    private static void mappingTool(final List<File> files,
            final String persistenceXmlFile, final String sqlFile, final String connectionDriverName,
            final String connectionProperties) {

        //extendRealmClasspath();

        Options opts = new Options();
        opts.put(OPTION_PROPERTIES_FILE, persistenceXmlFile);
        opts.put(OPTION_CONNECTION_DRIVER_NAME, connectionDriverName);
        opts.put(OPTION_CONNECTION_PROPERTIES, connectionProperties);
        opts.put(OPTION_SQL_FILE, sqlFile);
        opts.put(OPTION_SQL_ACTION, "build");

        filterPersistenceCapable(files, opts);

        // list of input files
        final String[] args = getFilePaths(files);

        boolean ok = Configurations.runAgainstAllAnchors(opts,
                new Configurations.Runnable() {

            @Override
            public boolean run(final Options opts) throws IOException, SQLException {
                JDBCConfiguration conf = new JDBCConfigurationImpl();
                try {
                    return MappingTool.run(conf, args, opts);
                } finally {
                    conf.close();
                }
            }
        });

        if (!ok) {
            throw new IllegalStateException("The OpenJPA MappingTool detected an error!");
        }

    }

    public static void main(final String[] args) {
        List<File> entities = findEntityClassFiles(System.getProperty("base"));

        mappingTool(entities, System.getProperty("persistenceXmlFile"), System.getProperty("sqlFile"),
                System.getProperty("connectionDriverName"),
                System.getProperty("connectionProperties").replaceAll(";", "="));
    }

    private SQLSchemaGenerator() {
        // Empty constructor for main() class
    }
}
