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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
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

    private static <T> T build(final String key, final String resource) throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn(key);
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(new String(
                Objects.requireNonNull(GroovySandboxTest.class.getResourceAsStream(resource)).readAllBytes(),
                StandardCharsets.UTF_8));

        return ImplementationManager.build(SyncopeConstants.MASTER_DOMAIN, impl);
    }

    @TempDir
    private Path tempDir;

    @Test
    void processBuilder() throws Exception {
        MacroActions actions = build("processBuilder", "/ProcessBuilderMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class, () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new java.lang.ProcessBuilder java.lang.String[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void bash() throws Exception {
        MacroActions actions = build("bash", "/BashMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class, () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new java.io.File java.lang.String'"));
    }

    @Test
    void staticMacroActions() throws Exception {
        BeanCreationException e = assertThrows(
                BeanCreationException.class,
                () -> build("staticMacroActions", "/StaticMacroActions.groovy"));
        SecurityException sec = (SecurityException) ExceptionUtils.getRootCause(e);
        assertTrue(sec.getMessage().startsWith("Insecure call to 'new java.lang.ProcessBuilder java.util.List'"));
    }

    @Test
    void pathOfFilesReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-read.txt");
        Files.writeString(testFile, "sandbox-read-ok");

        MacroActions actions = build("pathOfFilesReadString", "/PathOfFilesReadStringMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Path of java.lang.String java.lang.String[]'"));
    }

    @Test
    void pathOfFilesWriteString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-write.txt");

        MacroActions actions = build("pathOfFilesWriteString", "/PathOfFilesWriteStringMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Path of java.lang.String java.lang.String[]'"));
    }

    @Test
    void pathOfUriFilesReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-read-uri.txt");
        Files.writeString(testFile, "sandbox-read-uri-ok");

        MacroActions actions = build("pathOfUriFilesReadString", "/PathOfUriFilesReadStringMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Path of java.net.URI'"));
    }

    @Test
    void pathsGetFilesReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-paths-get-read.txt");
        Files.writeString(testFile, "sandbox-paths-get-read-ok");

        MacroActions actions = build("pathsGetFilesReadString", "/PathsGetFilesReadStringMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Paths get java.lang.String java.lang.String[]'"));
    }

    @Test
    void pathsGetUriFilesReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-paths-get-uri-read.txt");
        Files.writeString(testFile, "sandbox-paths-get-uri-read-ok");

        MacroActions actions = build("pathsGetUriFilesReadString", "/PathsGetUriFilesReadStringMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Paths get java.net.URI'"));
    }

    @Test
    void fileSystemsReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-filesystems-read.txt");
        Files.writeString(testFile, "sandbox-filesystems-read-ok");

        MacroActions actions = build("fileSystemsReadString", "/FileSystemsReadStringMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.FileSystems getDefault'"));
    }

    @Test
    void fileSystemProviderReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-filesystem-provider-read.txt");
        Files.writeString(testFile, "sandbox-filesystem-provider-read-ok");

        MacroActions actions =
                build("fileSystemProviderReadString", "/FileSystemProviderReadStringMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.spi.FileSystemProvider installedProviders'"));
    }

    @Test
    void filesCreateTempFile() throws Exception {
        MacroActions actions = build("filesCreateTempFile", "/FilesCreateTempFileMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Files createTempFile "
                + "java.lang.String java.lang.String java.nio.file.attribute.FileAttribute[]'"));
    }

    @Test
    void filesCreateTempDirectory() throws Exception {
        MacroActions actions = build("filesCreateTempDirectory", "/FilesCreateTempDirectoryMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Files createTempDirectory "
                + "java.lang.String java.nio.file.attribute.FileAttribute[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void processBuilderStartPipeline() throws Exception {
        MacroActions actions = build("processBuilderStartPipeline", "/ProcessBuilderStartPipelineMacroActions.groovy");

        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx = (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of(
                "builders",
                List.of(new ProcessBuilder("/bin/sh", "-c", "printf sandbox-start-pipeline-ok")));

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.lang.ProcessBuilder startPipeline java.util.List'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void runtimeExec() throws Exception {
        MacroActions actions = build("runtimeExec", "/RuntimeExecMacroActions.groovy");

        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("runtime", Runtime.getRuntime());

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'method java.lang.Runtime exec java.lang.String[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void beansExpressionRuntimeExec() throws Exception {
        MacroActions actions = build("beansExpressionRuntimeExec", "/BeansExpressionRuntimeExecMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'new java.beans.Expression java.lang.Object java.lang.String java.lang.Object[]'"));
    }

    @Test
    void beansStatementSystemExit() throws Exception {
        MacroActions actions = build("beansStatementSystemExit", "/BeansStatementSystemExitMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'new java.beans.Statement java.lang.Object java.lang.String java.lang.Object[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void groovyShellRuntimeExec() throws Exception {
        MacroActions actions = build("groovyShellRuntimeExec", "/GroovyShellRuntimeExecMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new groovy.lang.GroovyShell'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void evalRuntimeExec() throws Exception {
        MacroActions actions = build("evalRuntimeExec", "/EvalRuntimeExecMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod groovy.util.Eval me java.lang.String'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void methodHandlesRuntimeExec() throws Exception {
        MacroActions actions = build("methodHandlesRuntimeExec", "/MethodHandlesRuntimeExecMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.lang.invoke.MethodHandles publicLookup'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void runtimeExecOverloads() throws Exception {
        MacroActions actions = build("runtimeExecOverloads", "/RuntimeExecOverloadsMacroActions.groovy");

        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("runtime", Runtime.getRuntime());

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'method java.lang.Runtime exec java.lang.String'"));
    }

    @Test
    void evalOverloads() throws Exception {
        MacroActions actions = build("evalOverloads", "/EvalOverloadsMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod groovy.util.Eval me java.lang.String'"));
    }

    @Test
    void groovyShellOverloads() throws Exception {
        Path script = tempDir.resolve("sandbox-groovy-shell-overload.groovy");
        Files.writeString(script, "return 'shell-evaluate-file|'");
        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("scriptFile", script.toFile());

        MacroActions actions = build("groovyShellOverloads", "/GroovyShellOverloadsMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new groovy.lang.GroovyShell'"));
    }

    @Test
    void groovyClassLoaderOverloads() throws Exception {
        MacroActions actions = build(
                "groovyClassLoaderOverloads", "/GroovyClassLoaderOverloadsMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new groovy.lang.GroovyClassLoader'"));
    }

    @Test
    void scriptEngineOverloads() throws Exception {
        MacroActions actions = build("scriptEngineOverloads", "/ScriptEngineOverloadsMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new javax.script.ScriptEngineManager'"));
    }

    @Test
    void fileSystemsOverloads() throws Exception {
        Path file = tempDir.resolve("sandbox-filesystems-overloads.txt");
        Files.writeString(file, "sandbox-filesystems-overloads-ok");
        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("path", file);

        MacroActions actions = build("fileSystemsOverloads", "/FileSystemsOverloadsMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.FileSystems getFileSystem java.net.URI'"));
    }

    @Test
    void fileSystemProviderOverloads() throws Exception {
        Path file = tempDir.resolve("sandbox-filesystem-provider-overloads.txt");
        Files.writeString(file, "sandbox-filesystem-provider-overloads-ok");
        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of(
                        "path", file,
                        "provider", file.getFileSystem().provider());

        MacroActions actions = build("fileSystemProviderOverloads", "/FileSystemProviderOverloadsMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'method java.nio.file.spi.FileSystemProvider getFileSystem java.net.URI'"));
    }

    @Test
    void methodHandlesOverloads() throws Exception {
        MacroActions actions = build("methodHandlesOverloads", "/MethodHandlesOverloadsMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.lang.invoke.MethodHandles lookup'"));
    }

    @Test
    void beansOverloads() throws Exception {
        MacroActions actions = build("beansOverloads", "/BeansOverloadsMacroActions.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'new java.beans.Expression java.lang.Object java.lang.String java.lang.Object[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void processGroovyMethodsSchedTaskJobDelegate() throws Exception {
        SchedTaskJobDelegate delegate = build(
                "processGroovyMethodsSchedTaskJobDelegate", "/ProcessGroovyMethodsSchedTaskJobDelegate.groovy");

        SecurityException e = assertThrows(
                SecurityException.class,
                () -> delegate.execute(TaskType.SCHEDULED, null, null));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod org.codehaus.groovy.runtime.ProcessGroovyMethods "
                + "execute java.lang.String'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void threadLocalBypassSchedTaskJobDelegate() throws Exception {
        SchedTaskJobDelegate delegate = build("threadLocalBypass", "/ThreadLocalBypassSchedTaskJobDelegate.groovy");

        JobExecutionException e = assertThrows(
                JobExecutionException.class,
                () -> delegate.execute(TaskType.SCHEDULED, null, null));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod org.codehaus.groovy.runtime.ProcessGroovyMethods "
                + "execute java.lang.String'"));
    }
}
