/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

public class SchemaExporter {

    private Configuration cfg;

    public SchemaExporter(String[] packageNames)
            throws Exception {

        cfg = new Configuration();
        cfg.setProperty("hibernate.hbm2ddl.auto", "create");

        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (int i = 0; i < packageNames.length; i++) {
            classes.addAll(getClasses(packageNames[i]));
        }

        for (Class<?> clazz : classes) {
            cfg.addAnnotatedClass(clazz);
        }
    }

    /**
     * Method that actually creates the file.  
     * @param dbDialect to use
     */
    private void generate(Dialect dialect) {
        cfg.setProperty("hibernate.dialect", dialect.getDialectClass());

        SchemaExport export = new SchemaExport(cfg);
        export.setFormat(true);
        export.setDelimiter(";");
        export.execute(true, false, false, true);
    }

    /**
     * Utility method used to fetch Class list based on a package name.
     * @param packageName should be the package containing your annotated beans.
     */
    private List<Class<?>> getClasses(final String packageName)
            throws Exception {

        List<Class<?>> classes = new ArrayList<Class<?>>();
        File directory = null;

        try {
            ClassLoader cld = Thread.currentThread().
                    getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException(
                        "Can't get class loader.");
            }
            String path = packageName.replace('.', '/');
            URL resource = cld.getResource(path);
            if (resource == null) {
                throw new ClassNotFoundException("No resource for "
                        + path);
            }
            directory = new File(resource.getFile());
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(packageName + " ("
                    + directory
                    + ") does not appear to be a valid package");
        }


        if (directory.exists()) {
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".class")) {
                    // removes the .class extension
                    classes.add(Class.forName(packageName + '.'
                            + files[i].substring(0, files[i].length()
                            - 6)));
                }
            }
        } else {
            throw new ClassNotFoundException(packageName
                    + " is not a valid package");
        }

        return classes;

    }

    /**
     * Holds the classnames of hibernate dialects for easy reference.
     */
    private static enum Dialect {

        ORACLE("org.hibernate.dialect.Oracle10gDialect"),
        POSTGRESQL("org.hibernate.dialect.PostgreSQLDialect"),
        MYSQL("org.hibernate.dialect.MySQL5InnoDBDialect"),
        H2("org.hibernate.dialect.H2Dialect"),
        HSQL("org.hibernate.dialect.HSQLDialect");

        private String dialectClass;

        private Dialect(String dialectClass) {
            this.dialectClass = dialectClass;
        }

        public String getDialectClass() {
            return dialectClass;
        }
    }

    public static void main(final String[] args)
            throws Exception {

        Dialect dialect = Dialect.HSQL;
        if (args.length == 1) {
            try {
                dialect = Dialect.valueOf(args[0]);
            } catch (IllegalArgumentException e) {
                System.err.println("Dialect not recognized, reverting to "
                        + dialect);
            }
        }

        SchemaExporter gen = new SchemaExporter(new String[]{
                    "org.syncope.core.persistence.beans",
                    "org.syncope.core.persistence.beans.user",
                    "org.syncope.core.persistence.beans.role",
                    "org.syncope.core.persistence.beans.membership"});
        gen.generate(dialect);
    }
}
