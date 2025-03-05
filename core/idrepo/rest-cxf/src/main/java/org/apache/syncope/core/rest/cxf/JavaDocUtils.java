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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

public final class JavaDocUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JavaDocUtils.class);

    private static URL toURL(final String classPathEntry) {
        try {
            return Path.of(classPathEntry).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("URL could not be created from '" + classPathEntry + "'", e);
        }
    }

    /**
     * Inspired by org.springframework.boot.devtools.restart.ChangeableUrls#urlsFromClassLoader
     *
     * @param classLoader class loader
     * @return URLs from underlying class loader
     */
    private static URL[] urlsFromClassLoader(final ClassLoader classLoader) {
        if (classLoader instanceof final URLClassLoader urlClassLoader) {
            return urlClassLoader.getURLs();
        }

        return Stream.of(ManagementFactory.getRuntimeMXBean().getClassPath().
                split(File.pathSeparator)).
                map(JavaDocUtils::toURL).toArray(URL[]::new);
    }

    public static Optional<URL[]> getJavaDocURLs() {
        URL[] urls = urlsFromClassLoader(ClassUtils.getDefaultClassLoader());
        if (urls == null) {
            LOG.debug("No classpath URLs found");
            return Optional.empty();
        }

        List<URL> javaDocURLs = new ArrayList<>();
        for (URL url : urls) {
            LOG.debug("Processing {}", url.toExternalForm());

            String filename = StringUtils.substringAfterLast(url.toExternalForm(), "/");
            if (filename.startsWith("syncope-") && filename.endsWith("-javadoc.jar")) {
                javaDocURLs.add(url);
            }
        }

        LOG.debug("JavaDoc Urls found: {}", javaDocURLs);
        return Optional.of(javaDocURLs.toArray(URL[]::new));
    }

    public static Optional<String[]> getJavaDocPaths(final Environment env) {
        String[] result = null;

        if (env.containsProperty("javadocPaths")) {
            result = Objects.requireNonNull(env.getProperty("javadocPaths")).split(",");
        }

        LOG.debug("JavaDoc paths found: {}",
                result == null ? List.of() : Arrays.stream(result).toList());
        return Optional.ofNullable(result);
    }

    private JavaDocUtils() {
        // private constructor for static utility class
    }
}
