<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">
  
  <xsl:template match="/dataset">
    <dataset>
      <xsl:apply-templates/>
      
      <ACT_RU_EXECUTION ID_="4" REV_="2" PROC_INST_ID_="4" PROC_DEF_ID_="userWorkflow:1:4" ACT_ID_="active" IS_ACTIVE_="1" IS_CONCURRENT_="0" IS_SCOPE_="1" IS_EVENT_SCOPE_="0" SUSPENSION_STATE_="1"/>
      <ACT_RU_TASK ID_="5" REV_="2" EXECUTION_ID_="4" PROC_INST_ID_="4" PROC_DEF_ID_="userWorkflow:1:4" NAME_="Active" TASK_DEF_KEY_="active" PRIORITY_="50" CREATE_TIME_="2013-02-25T17:19:03+0100"/>

      <ACT_RU_EXECUTION ID_="6" REV_="2" PROC_INST_ID_="6" PROC_DEF_ID_="userWorkflow:1:4" ACT_ID_="active" IS_ACTIVE_="1" IS_CONCURRENT_="0" IS_SCOPE_="1" IS_EVENT_SCOPE_="0" SUSPENSION_STATE_="1"/>
      <ACT_RU_TASK ID_="7" REV_="2" EXECUTION_ID_="6" PROC_INST_ID_="6" PROC_DEF_ID_="userWorkflow:1:4" NAME_="Active" TASK_DEF_KEY_="active" PRIORITY_="50" CREATE_TIME_="2013-02-25T17:19:03+0100"/>

      <ACT_RU_EXECUTION ID_="8" REV_="2" PROC_INST_ID_="8" PROC_DEF_ID_="userWorkflow:1:4" ACT_ID_="active" IS_ACTIVE_="1" IS_CONCURRENT_="0" IS_SCOPE_="1" IS_EVENT_SCOPE_="0" SUSPENSION_STATE_="1"/>
      <ACT_RU_TASK ID_="9" REV_="2" EXECUTION_ID_="8" PROC_INST_ID_="8" PROC_DEF_ID_="userWorkflow:1:4" NAME_="Active" TASK_DEF_KEY_="active" PRIORITY_="50" CREATE_TIME_="2013-02-25T17:19:03+0100"/>

      <ACT_RU_EXECUTION ID_="10" REV_="2" PROC_INST_ID_="10" PROC_DEF_ID_="userWorkflow:1:4" ACT_ID_="active" IS_ACTIVE_="1" IS_CONCURRENT_="0" IS_SCOPE_="1" IS_EVENT_SCOPE_="0" SUSPENSION_STATE_="1"/>
      <ACT_RU_TASK ID_="11" REV_="2" EXECUTION_ID_="10" PROC_INST_ID_="10" PROC_DEF_ID_="userWorkflow:1:4" NAME_="Active" TASK_DEF_KEY_="active" PRIORITY_="50" CREATE_TIME_="2013-02-25T17:19:03+0100"/>

      <ACT_RU_EXECUTION ID_="12" REV_="2" PROC_INST_ID_="12" PROC_DEF_ID_="userWorkflow:1:4" ACT_ID_="active" IS_ACTIVE_="1" IS_CONCURRENT_="0" IS_SCOPE_="1" IS_EVENT_SCOPE_="0" SUSPENSION_STATE_="1"/>
      <ACT_RU_TASK ID_="13" REV_="2" EXECUTION_ID_="12" PROC_INST_ID_="12" PROC_DEF_ID_="userWorkflow:1:4" NAME_="Active" TASK_DEF_KEY_="active" PRIORITY_="50" CREATE_TIME_="2013-02-25T17:19:03+0100"/>
    </dataset>
  </xsl:template>
  
  <xsl:template match="node()|@*|comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
