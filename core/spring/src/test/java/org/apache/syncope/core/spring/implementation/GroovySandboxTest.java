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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.api.macro.Command;
import org.apache.syncope.core.provisioning.api.macro.MacroActions;
import org.apache.syncope.core.spring.SpringTestConfiguration;
import org.codehaus.groovy.runtime.ProcessGroovyMethods;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { SpringTestConfiguration.class })
class GroovySandboxTest {

    @Autowired
    private Whitelist whitelist;

    private static <T> T build(final String key, final String resource) throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn(key);
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(new String(
                Objects.requireNonNull(GroovySandboxTest.class.getResourceAsStream(resource)).readAllBytes(),
                StandardCharsets.UTF_8));

        return ImplementationManager.build(SyncopeConstants.MASTER_DOMAIN, impl);
    }

    private static void assertDenied(final Executable executable) {
        Throwable thrown = assertThrows(Throwable.class, executable);
        Throwable root = ExceptionUtils.getRootCause(thrown);
        assertTrue(root instanceof SecurityException, "Expected a sandbox rejection, got: " + root);
        assertTrue(root.getMessage().startsWith("Insecure call to"), "Unexpected rejection: " + root.getMessage());
    }

    @TempDir
    private Path tempDir;

    @Test
    void processGroovyMethodsAreNotWhitelisted() throws Exception {
        assertFalse(whitelist.permitsStaticMethod(
                ProcessGroovyMethods.class.getMethod("execute", String.class), new Object[] { "id" }));
    }

    @Test
    void threadCreationIsNotWhitelisted() throws Exception {
        assertFalse(whitelist.permitsConstructor(
                Thread.class.getConstructor(Runnable.class), new Object[] { (Runnable) () -> { } }));
    }

    @Test
    void script() {
        String script =
                """
                log.info("Entering {0} Script for {1}", action, objectClass)
                """;
        ScriptExecutor scriptExecutor = ScriptExecutorFactory.newInstance("GROOVY").
                newScriptExecutor(getClass().getClassLoader(), script, true);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("objectClass", ObjectClass.ACCOUNT_NAME);
        arguments.put("action", "SEARCH");
        arguments.put("log", Log.getLog(GroovySandboxTest.class));

        assertDoesNotThrow(() -> scriptExecutor.execute(arguments));
    }

    @Test
    void processBuilder() throws Exception {
        MacroActions actions = build("processBuilder", "/ProcessBuilderMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void bash() throws Exception {
        MacroActions actions = build("bash", "/BashMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @Test
    void staticMacroActions() {
        assertDenied(() -> build("staticMacroActions", "/StaticMacroActions.groovy"));
    }

    @Test
    void pathOfFilesReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-read.txt");
        Files.writeString(testFile, "sandbox-read-ok");

        MacroActions actions = build("pathOfFilesReadString", "/PathOfFilesReadStringMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
    }

    @Test
    void pathOfFilesWriteString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-write.txt");

        MacroActions actions = build("pathOfFilesWriteString", "/PathOfFilesWriteStringMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
    }

    @Test
    void pathOfUriFilesReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-read-uri.txt");
        Files.writeString(testFile, "sandbox-read-uri-ok");

        MacroActions actions = build("pathOfUriFilesReadString", "/PathOfUriFilesReadStringMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
    }

    @Test
    void pathsGetFilesReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-paths-get-read.txt");
        Files.writeString(testFile, "sandbox-paths-get-read-ok");

        MacroActions actions = build("pathsGetFilesReadString", "/PathsGetFilesReadStringMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
    }

    @Test
    void pathsGetUriFilesReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-paths-get-uri-read.txt");
        Files.writeString(testFile, "sandbox-paths-get-uri-read-ok");

        MacroActions actions = build("pathsGetUriFilesReadString", "/PathsGetUriFilesReadStringMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
    }

    @Test
    void fileSystemsReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-filesystems-read.txt");
        Files.writeString(testFile, "sandbox-filesystems-read-ok");

        MacroActions actions = build("fileSystemsReadString", "/FileSystemsReadStringMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
    }

    @Test
    void fileSystemProviderReadString() throws Exception {
        Path testFile = tempDir.resolve("sandbox-filesystem-provider-read.txt");
        Files.writeString(testFile, "sandbox-filesystem-provider-read-ok");

        MacroActions actions =
                build("fileSystemProviderReadString", "/FileSystemProviderReadStringMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
    }

    @Test
    void filesCreateTempFile() throws Exception {
        MacroActions actions = build("filesCreateTempFile", "/FilesCreateTempFileMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @Test
    void filesCreateTempDirectory() throws Exception {
        MacroActions actions = build("filesCreateTempDirectory", "/FilesCreateTempDirectoryMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void processBuilderStartPipeline() throws Exception {
        MacroActions actions = build("processBuilderStartPipeline", "/ProcessBuilderStartPipelineMacroActions.groovy");

        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx = (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of(
                "builders",
                List.of(new ProcessBuilder("/bin/sh", "-c", "printf sandbox-start-pipeline-ok")));

        assertDenied(() -> actions.afterAll(ctx, new StringBuilder()));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void runtimeExec() throws Exception {
        MacroActions actions = build("runtimeExec", "/RuntimeExecMacroActions.groovy");

        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("runtime", Runtime.getRuntime());

        assertDenied(() -> actions.afterAll(ctx, new StringBuilder()));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void beansExpressionRuntimeExec() throws Exception {
        MacroActions actions = build("beansExpressionRuntimeExec", "/BeansExpressionRuntimeExecMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @Test
    void beansStatementSystemExit() throws Exception {
        MacroActions actions = build("beansStatementSystemExit", "/BeansStatementSystemExitMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void groovyShellRuntimeExec() throws Exception {
        MacroActions actions = build("groovyShellRuntimeExec", "/GroovyShellRuntimeExecMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void evalRuntimeExec() throws Exception {
        MacroActions actions = build("evalRuntimeExec", "/EvalRuntimeExecMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void methodHandlesRuntimeExec() throws Exception {
        MacroActions actions = build("methodHandlesRuntimeExec", "/MethodHandlesRuntimeExecMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void runtimeExecOverloads() throws Exception {
        MacroActions actions = build("runtimeExecOverloads", "/RuntimeExecOverloadsMacroActions.groovy");

        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("runtime", Runtime.getRuntime());

        assertDenied(() -> actions.afterAll(ctx, new StringBuilder()));
    }

    @Test
    void evalOverloads() throws Exception {
        MacroActions actions = build("evalOverloads", "/EvalOverloadsMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @Test
    void groovyShellOverloads() throws Exception {
        Path script = tempDir.resolve("sandbox-groovy-shell-overload.groovy");
        Files.writeString(script, "return 'shell-evaluate-file|'");
        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("scriptFile", script.toFile());

        MacroActions actions = build("groovyShellOverloads", "/GroovyShellOverloadsMacroActions.groovy");

        assertDenied(() -> actions.afterAll(ctx, new StringBuilder()));
    }

    @Test
    void groovyClassLoaderOverloads() throws Exception {
        MacroActions actions = build(
                "groovyClassLoaderOverloads", "/GroovyClassLoaderOverloadsMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @Test
    void scriptEngineOverloads() throws Exception {
        MacroActions actions = build("scriptEngineOverloads", "/ScriptEngineOverloadsMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @Test
    void fileSystemsOverloads() throws Exception {
        Path file = tempDir.resolve("sandbox-filesystems-overloads.txt");
        Files.writeString(file, "sandbox-filesystems-overloads-ok");
        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("path", file);

        MacroActions actions = build("fileSystemsOverloads", "/FileSystemsOverloadsMacroActions.groovy");

        assertDenied(() -> actions.afterAll(ctx, new StringBuilder()));
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

        assertDenied(() -> actions.afterAll(ctx, new StringBuilder()));
    }

    @Test
    void methodHandlesOverloads() throws Exception {
        MacroActions actions = build("methodHandlesOverloads", "/MethodHandlesOverloadsMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @Test
    void beansOverloads() throws Exception {
        MacroActions actions = build("beansOverloads", "/BeansOverloadsMacroActions.groovy");

        assertDenied(() -> actions.afterAll(null, new StringBuilder()));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void processGroovyMethods() throws Exception {
        SchedTaskJobDelegate delegate = build(
                "processGroovyMethods", "/ProcessGroovyMethodsSchedTaskJobDelegate.groovy");

        assertDenied(() -> delegate.execute(TaskType.SCHEDULED, null, null));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void hashCodeAfterSandbox() throws Exception {
        Command<CommandArgs> command = build("postSandboxHashCode", "/PostSandboxHashCodeCommand.groovy");

        Command.Result result = command.run(new CommandArgs());

        Object key = result.values().keySet().iterator().next();
        assertDenied(key::hashCode);
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void staticInitializerAfterSandbox() throws Exception {
        Command<CommandArgs> command = build("postSandboxConstructor", "/PostSandboxConstructorCommand.groovy");

        Command.Result result = command.run(new CommandArgs());

        Class<?> type = (Class<?>) result.values().get("staticType");
        assertDenied(() -> Class.forName(type.getName(), true, type.getClassLoader()));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void constructorAfterSandbox() throws Exception {
        Command<CommandArgs> command = build("postSandboxConstructor", "/PostSandboxConstructorCommand.groovy");

        Command.Result result = command.run(new CommandArgs());

        Class<?> type = (Class<?>) result.values().get("type");
        assertDenied(() -> type.getDeclaredConstructor().newInstance());
    }

    @Test
    void closureCall() throws Exception {
        Command<CommandArgs> command = build("closure", "/ClosureCommand.groovy");

        Command.Result result = command.run(new CommandArgs());

        assertEquals("no-arg", result.values().get("noArg"));
        assertEquals("one", result.values().get("oneArg"));
        assertEquals("two-args", result.values().get("twoArgs"));
    }

    @Test
    void externalClosureCallIsNotWhitelisted() throws Exception {
        Whitelist sandboxWhitelist = new SelfAllowingWhitelist(whitelist, new GroovyClassLoader());
        Closure<?> closure = new Closure<>(this) {
        };

        assertFalse(sandboxWhitelist.permitsMethod(
                Closure.class.getMethod("call", Object.class), closure, new Object[] { "test" }));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void closureAfterSandbox() throws Exception {
        Command<CommandArgs> command = build("postSandboxClosure", "/PostSandboxClosureCommand.groovy");

        Command.Result result = command.run(new CommandArgs());

        Closure<?> closure = (Closure<?>) result.values().get("closure");
        assertDenied(closure::call);
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void threadLocalBypass() throws Exception {
        SchedTaskJobDelegate delegate = build("threadLocalBypass", "/ThreadLocalBypassSchedTaskJobDelegate.groovy");

        assertDenied(() -> delegate.execute(TaskType.SCHEDULED, null, null));
    }
}
