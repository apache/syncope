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
import groovy.transform.BaseScript
import org.apache.syncope.core.provisioning.api.macro.MacroActions
import java.io.Serializable

abstract class GroovyShellOverloadsMacroActions extends Script implements MacroActions {}
@BaseScript GroovyShellOverloadsMacroActions _

@Override
StringBuilder afterAll(Map<String, Serializable> ctx, StringBuilder output) {
  def shell = new groovy.lang.GroovyShell()

  output.append(shell.evaluate("'shell-evaluate-string|'"))
  output.append(shell.evaluate("'shell-evaluate-string-name|'", 'TmpEvaluate.groovy'))
  output.append(shell.evaluate("'shell-evaluate-string-name-codebase|'", 'TmpEvaluate.groovy', '/tmp'))
  output.append(shell.evaluate(new java.io.StringReader("'shell-evaluate-reader|'")))
  output.append(shell.evaluate(new java.io.StringReader("'shell-evaluate-reader-name|'"), 'TmpEvaluateReader.groovy'))
  output.append(shell.evaluate(ctx.scriptFile as java.io.File))
  output.append(shell.evaluate((ctx.scriptFile as java.io.File).toURI()))
  output.append(shell.parse("return 'shell-parse-string|'").run())
  output.append(shell.parse(new java.io.StringReader("return 'shell-parse-reader|'")).run())
  output.append(shell.run("return 'shell-run-string-list|'", 'TmpRun.groovy', []))
  output.append(shell.run(new java.io.StringReader("return 'shell-run-reader-list|'"), 'TmpRunReader.groovy', []))

  return output
}
