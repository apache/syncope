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
package org.apache.syncope.core.spring.implementation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.macro.MacroActions;
import org.apache.syncope.core.spring.SpringTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { SpringTestConfiguration.class })
class GroovySandboxTest {

    @TempDir
    private Path tempDir;

    @Test
    void processBuilder() throws Exception {
        final MacroActions actions = actions("processBuilder", "/ProcessBuilderMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class, () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new java.lang.ProcessBuilder java.lang.String[]'"));
    }

    private MacroActions actions(final String key, final String resource) throws Exception {
        final Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn(key);
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(resource))));

        return ImplementationManager.build(impl);
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void bash() throws Exception {
        final MacroActions actions = actions("bash", "/BashMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class, () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new java.io.File java.lang.String'"));
    }

    @Test
    void staticMacroActions() throws Exception {
        final BeanCreationException e =
                assertThrows(
                        BeanCreationException.class,
                        () -> actions("staticMacroActions", "/StaticMacroActions.groovy"));
        final SecurityException sec = (SecurityException) ExceptionUtils.getRootCause(e);
        assertTrue(sec.getMessage().startsWith("Insecure call to 'new java.lang.ProcessBuilder java.util.List'"));
    }

    @Test
    void pathOfFilesReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-read.txt");
        Files.writeString(testFile, "sandbox-read-ok");

        final MacroActions actions = actions("pathOfFilesReadString", "/PathOfFilesReadStringMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Path of java.lang.String java.lang.String[]'"));
    }

    @Test
    void pathOfFilesWriteString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-write.txt");

        final MacroActions actions = actions("pathOfFilesWriteString", "/PathOfFilesWriteStringMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Path of java.lang.String java.lang.String[]'"));
    }

    @Test
    void pathOfUriFilesReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-read-uri.txt");
        Files.writeString(testFile, "sandbox-read-uri-ok");

        final MacroActions actions =
                actions("pathOfUriFilesReadString", "/PathOfUriFilesReadStringMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Path of java.net.URI'"));
    }

    @Test
    void pathsGetFilesReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-paths-get-read.txt");
        Files.writeString(testFile, "sandbox-paths-get-read-ok");

        final MacroActions actions = actions("pathsGetFilesReadString", "/PathsGetFilesReadStringMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Paths get java.lang.String java.lang.String[]'"));
    }

    @Test
    void pathsGetUriFilesReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-paths-get-uri-read.txt");
        Files.writeString(testFile, "sandbox-paths-get-uri-read-ok");

        final MacroActions actions =
                actions("pathsGetUriFilesReadString", "/PathsGetUriFilesReadStringMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Paths get java.net.URI'"));
    }

    @Test
    void fileSystemsReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-filesystems-read.txt");
        Files.writeString(testFile, "sandbox-filesystems-read-ok");

        final MacroActions actions = actions("fileSystemsReadString", "/FileSystemsReadStringMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.FileSystems getDefault'"));
    }

    @Test
    void fileSystemProviderReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-filesystem-provider-read.txt");
        Files.writeString(testFile, "sandbox-filesystem-provider-read-ok");

        final MacroActions actions =
                actions("fileSystemProviderReadString", "/FileSystemProviderReadStringMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.spi.FileSystemProvider installedProviders'"));
    }

    @Test
    void filesCreateTempFile() throws Exception {
        final MacroActions actions = actions("filesCreateTempFile", "/FilesCreateTempFileMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Files createTempFile "
                + "java.lang.String java.lang.String java.nio.file.attribute.FileAttribute[]'"));
    }

    @Test
    void filesCreateTempDirectory() throws Exception {
        final MacroActions actions =
                actions("filesCreateTempDirectory", "/FilesCreateTempDirectoryMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Files createTempDirectory "
                + "java.lang.String java.nio.file.attribute.FileAttribute[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void processBuilderStartPipeline() throws Exception {
        final MacroActions actions =
                actions("processBuilderStartPipeline", "/ProcessBuilderStartPipelineMacroActions.groovy");

        @SuppressWarnings("unchecked")
        final Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of(
                        "builders",
                        List.of(new ProcessBuilder("/bin/sh", "-c", "printf sandbox-start-pipeline-ok")));

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.lang.ProcessBuilder startPipeline java.util.List'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void runtimeExec() throws Exception {
        final MacroActions actions = actions("runtimeExec", "/RuntimeExecMacroActions.groovy");

        @SuppressWarnings("unchecked")
        final Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("runtime", Runtime.getRuntime());

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'method java.lang.Runtime exec java.lang.String[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void beansExpressionRuntimeExec() throws Exception {
        final MacroActions actions =
                actions("beansExpressionRuntimeExec", "/BeansExpressionRuntimeExecMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'new java.beans.Expression java.lang.Object java.lang.String java.lang.Object[]'"));
    }

    @Test
    void beansStatementSystemExit() throws Exception {
        final MacroActions actions =
                actions("beansStatementSystemExit", "/BeansStatementSystemExitMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'new java.beans.Statement java.lang.Object java.lang.String java.lang.Object[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void groovyShellRuntimeExec() throws Exception {
        final MacroActions actions = actions("groovyShellRuntimeExec", "/GroovyShellRuntimeExecMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new groovy.lang.GroovyShell'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void evalRuntimeExec() throws Exception {
        final MacroActions actions = actions("evalRuntimeExec", "/EvalRuntimeExecMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod groovy.util.Eval me java.lang.String'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void methodHandlesRuntimeExec() throws Exception {
        final MacroActions actions =
                actions("methodHandlesRuntimeExec", "/MethodHandlesRuntimeExecMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.lang.invoke.MethodHandles publicLookup'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void runtimeExecOverloads() throws Exception {
        final MacroActions actions = actions("runtimeExecOverloads", "/RuntimeExecOverloadsMacroActions.groovy");

        @SuppressWarnings("unchecked")
        final Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("runtime", Runtime.getRuntime());

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'method java.lang.Runtime exec java.lang.String'"));
    }

    @Test
    void evalOverloads() throws Exception {
        final MacroActions actions = actions("evalOverloads", "/EvalOverloadsMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod groovy.util.Eval me java.lang.String'"));
    }

    @Test
    void groovyShellOverloads() throws Exception {
        final Path script = tempDir.resolve("sandbox-groovy-shell-overload.groovy");
        Files.writeString(script, "return 'shell-evaluate-file|'");
        @SuppressWarnings("unchecked")
        final Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("scriptFile", script.toFile());

        final MacroActions actions = actions("groovyShellOverloads", "/GroovyShellOverloadsMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new groovy.lang.GroovyShell'"));
    }

    @Test
    void groovyClassLoaderOverloads() throws Exception {
        final MacroActions actions =
                actions("groovyClassLoaderOverloads", "/GroovyClassLoaderOverloadsMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new groovy.lang.GroovyClassLoader'"));
    }

    @Test
    void scriptEngineOverloads() throws Exception {
        final MacroActions actions = actions("scriptEngineOverloads", "/ScriptEngineOverloadsMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new javax.script.ScriptEngineManager'"));
    }

    @Test
    void fileSystemsOverloads() throws Exception {
        final Path file = tempDir.resolve("sandbox-filesystems-overloads.txt");
        Files.writeString(file, "sandbox-filesystems-overloads-ok");
        @SuppressWarnings("unchecked")
        final Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("path", file);

        final MacroActions actions = actions("fileSystemsOverloads", "/FileSystemsOverloadsMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.FileSystems getFileSystem java.net.URI'"));
    }

    @Test
    void fileSystemProviderOverloads() throws Exception {
        final Path file = tempDir.resolve("sandbox-filesystem-provider-overloads.txt");
        Files.writeString(file, "sandbox-filesystem-provider-overloads-ok");
        @SuppressWarnings("unchecked")
        final Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of(
                        "path", file,
                        "provider", file.getFileSystem().provider());

        final MacroActions actions =
                actions("fileSystemProviderOverloads", "/FileSystemProviderOverloadsMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'method java.nio.file.spi.FileSystemProvider getFileSystem java.net.URI'"));
    }

    @Test
    void methodHandlesOverloads() throws Exception {
        final MacroActions actions = actions("methodHandlesOverloads", "/MethodHandlesOverloadsMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.lang.invoke.MethodHandles lookup'"));
    }

    @Test
    void beansOverloads() throws Exception {
        final MacroActions actions = actions("beansOverloads", "/BeansOverloadsMacroActions.groovy");

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'new java.beans.Expression java.lang.Object java.lang.String java.lang.Object[]'"));
    }
}
