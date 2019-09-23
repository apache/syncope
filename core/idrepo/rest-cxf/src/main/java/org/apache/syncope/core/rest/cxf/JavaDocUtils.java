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
package org.apache.syncope.core.rest.cxf;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

public final class JavaDocUtils {

    public static URL[] getJavaDocURLs() {
        URL[] result = null;

        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        if (classLoader instanceof URLClassLoader) {
            List<URL> javaDocURLs = new ArrayList<>();
            for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                String filename = StringUtils.substringAfterLast(url.toExternalForm(), "/");
                if (filename.startsWith("syncope-") && filename.endsWith("-javadoc.jar")) {
                    javaDocURLs.add(url);
                }
            }
            if (!javaDocURLs.isEmpty()) {
                result = javaDocURLs.toArray(new URL[javaDocURLs.size()]);
            }
        }

        return result;
    }

    public static String[] getJavaDocPaths(final Environment env) {
        String[] result = null;

        if (env.containsProperty("javadocPaths")) {
            result = Objects.requireNonNull(env.getProperty("javadocPaths")).split(",");
        }

        return result;
    }

    private JavaDocUtils() {
        // private constructor for static utility class
    }
}
