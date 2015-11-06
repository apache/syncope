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
package org.apache.syncope.client.cli.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import org.apache.syncope.client.cli.commands.install.InstallConfigFileTemplate;

public final class FileSystemUtils {

    private static final ResourceBundle CONF = ResourceBundle.getBundle("configuration");

    public static void createNewDirectory(final String directoryToCreate) {
        final File directory = new File(directoryToCreate);
        directory.mkdirs();
    }

    public static void createFileWith(final String filePath, final String content)
            throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter writer = new PrintWriter(filePath, "UTF-8")) {
            writer.println(content);
        }
    }

    public static boolean canWrite(final String path) {
        final File installationDirectory = new File(path);
        return installationDirectory.canWrite();
    }

    public static boolean exists(final String path) {
        final File installationDirectory = new File(path);
        return installationDirectory.exists();
    }

    public static void createScriptFile() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        final File file = new File(InstallConfigFileTemplate.scriptFilePath());
        file.setExecutable(true);
        file.setReadable(true);
        file.setWritable(true);
        file.createNewFile();
        final FileWriter fw = new FileWriter(file.getAbsoluteFile());
        final BufferedWriter bw = new BufferedWriter(fw);
        if (isWindows()) {
            bw.write(CONF.getString("script.file.windows"));
        } else {
            bw.write(CONF.getString("script.file.linux"));
            final Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(Paths.get(file.getAbsolutePath()), perms);
        }
        bw.close();
    }

    public static boolean isWindows() {
        return (System.getProperty("os.name").toLowerCase().contains("win"));
    }

    private FileSystemUtils() {

    }
}
