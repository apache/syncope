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
package org.syncope.buildtools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;

/**
 * Extracts LDIF files for the schema repository onto a destination directory,
 * from specified JAR.
 */
public class JarSchemaLdifExtractor implements SchemaLdifExtractor {

    private static final String SCHEMA_SUBDIR = "schema";

    private boolean extracted;

    private final File outputDirectory;

    private final File schemaDirectory;

    private final File jarFile;

    /**
     * Creates an extractor which deposits files into the specified output
     * directory.
     *
     * @param outputDirectory the directory where the schema root is extracted
     * @param jarFile the JAR file
     */
    public JarSchemaLdifExtractor(final File outputDirectory,
            final File jarFile)
            throws IOException {

        this.outputDirectory = outputDirectory;
        this.schemaDirectory = new File(outputDirectory, SCHEMA_SUBDIR);
        this.jarFile = jarFile;

        if (!outputDirectory.exists() && !outputDirectory.mkdir()) {
            throw new IOException("Failed to create outputDirectory: "
                    + outputDirectory);
        }

        extracted = !schemaDirectory.exists();
    }

    /**
     * Gets whether or not schema folder has been created or not.
     *
     * @return true if schema folder has already been extracted.
     */
    @Override
    public boolean isExtracted() {
        return extracted;
    }

    /**
     * Extracts the LDIF files from a Jar file.
     *
     * @param overwrite over write extracted structure if true, false otherwise
     * @throws IOException if schema already extracted and on IO errors
     */
    @Override
    public void extractOrCopy(final boolean overwrite)
            throws IOException {

        if (!outputDirectory.exists() && !outputDirectory.mkdir()) {
            throw new IOException("Could not create "
                    + outputDirectory.getAbsolutePath());
        }

        if (!schemaDirectory.exists()) {
            if (!schemaDirectory.mkdir()) {
                throw new IOException("Could not create "
                        + schemaDirectory.getAbsolutePath());
            }
        } else if (!overwrite) {
            throw new IOException(I18n.err(I18n.ERR_08001, schemaDirectory.
                    getAbsolutePath()));
        }

        final Pattern pattern = Pattern.compile(".*schema/ou=schema.*\\.ldif");
        for (String entry : getResources(pattern)) {
            extractFromJar(entry);
        }
    }

    /**
     * Extracts the LDIF files from a Jar file or copies exploded LDIF
     * resources without overwriting the resources if the schema has
     * already been extracted.
     *
     * @throws IOException if schema already extracted and on IO errors
     */
    @Override
    public void extractOrCopy()
            throws IOException {

        extractOrCopy(false);
    }

    private Set<String> getResources(final Pattern pattern)
            throws IOException {

        final Set<String> result = new HashSet<String>();

        final ZipFile zipFile = new ZipFile(jarFile);
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (pattern.matcher(entry.getName()).matches()) {
                result.add(entry.getName());
            }
        }
        zipFile.close();

        return result;
    }

    /**
     * Extracts the LDIF schema resource from a Jar.
     *
     * @param resource the LDIF schema resource
     * @throws IOException if there are IO errors
     */
    private void extractFromJar(final String resource)
            throws IOException {

        final InputStream in =
                DefaultSchemaLdifExtractor.getUniqueResourceAsStream(
                resource, "LDIF file in schema repository");
        try {
            final File destination = new File(outputDirectory, resource);

            /*
             * Do not overwrite an LDIF file if it has already been extracted.
             */
            if (destination.exists()) {
                return;
            }

            if (!destination.getParentFile().exists() && !destination.
                    getParentFile().mkdirs()) {

                throw new IOException("Could not create "
                        + destination.getParentFile().getAbsolutePath());
            }

            final FileOutputStream out = new FileOutputStream(destination);
            final byte[] buf = new byte[512];
            try {
                while (in.available() > 0) {
                    final int readCount = in.read(buf);
                    out.write(buf, 0, readCount);
                }
                out.flush();
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
