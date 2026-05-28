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

abstract class FileSystemsOverloadsMacroActions extends Script implements MacroActions {}
@BaseScript FileSystemsOverloadsMacroActions _

@Override
StringBuilder afterAll(Map<String, Serializable> ctx, StringBuilder output) {
  def fileUri = java.net.URI.create('file:///')
  def unsupportedUri = java.net.URI.create('tmpfs://sandbox')
  def path = ctx.path as java.nio.file.Path

  java.nio.file.FileSystems.getFileSystem(fileUri)
  output.append('fs-get-filesystem|')

  try {
    java.nio.file.FileSystems.newFileSystem(unsupportedUri, [:])
  } catch (java.nio.file.ProviderNotFoundException e) {
    output.append('fs-new-uri-map|')
  }

  try {
    java.nio.file.FileSystems.newFileSystem(unsupportedUri, [:], getClass().classLoader)
  } catch (java.nio.file.ProviderNotFoundException e) {
    output.append('fs-new-uri-map-loader|')
  }

  try {
    java.nio.file.FileSystems.newFileSystem(path)
  } catch (java.io.IOException | java.nio.file.ProviderNotFoundException e) {
    output.append('fs-new-path|')
  }

  try {
    java.nio.file.FileSystems.newFileSystem(path, getClass().classLoader)
  } catch (java.io.IOException | java.nio.file.ProviderNotFoundException e) {
    output.append('fs-new-path-loader|')
  }

  try {
    java.nio.file.FileSystems.newFileSystem(path, [:])
  } catch (java.io.IOException | java.nio.file.ProviderNotFoundException e) {
    output.append('fs-new-path-map|')
  }

  try {
    java.nio.file.FileSystems.newFileSystem(path, [:], getClass().classLoader)
  } catch (java.io.IOException | java.nio.file.ProviderNotFoundException e) {
    output.append('fs-new-path-map-loader|')
  }

  return output
}
