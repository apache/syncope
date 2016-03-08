<%--
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
--%>
<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@page import="org.springframework.beans.factory.support.DefaultListableBeanFactory"%>
<%@page import="org.apache.syncope.common.lib.SyncopeConstants"%>
<%@page import="org.apache.syncope.core.spring.ApplicationContextProvider"%>
<%@page import="org.apache.commons.lang3.time.FastDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="org.apache.openjpa.datacache.CacheStatisticsImpl"%>
<%@page import="javax.persistence.EntityManagerFactory"%>
<%@page import="org.apache.openjpa.persistence.OpenJPAPersistence"%>
<%@page import="org.apache.openjpa.persistence.OpenJPAEntityManagerFactory"%>
<%@page import="org.springframework.context.ConfigurableApplicationContext"%>
<%@page import="org.apache.openjpa.datacache.QueryKey"%>
<%@page import="org.apache.openjpa.kernel.QueryStatistics"%>
<%@page import="org.apache.openjpa.persistence.QueryResultCacheImpl"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Cache Statistics</title>
    <style type="text/css">
      .c{
        text-align: center;
      }
      .r{
        text-align: right;
      }
      .l{
        text-align: left;
      }
      .t{
        vertical-align: top;
      }
      .b{
        vertical-align: bottom;
      }
      .odd{
        background-color: #D4D4D4;
      }
      .even{
        background-color: #EEEEEE;
      }
      .bd1{
        border: solid #888888 1px;
      }
      .bg1{
        background-color: #CCCCCC;
      }
      .bg2{
        background-color: #DDDDDD;
      }
    </style>
  </head>
  <body>
    <p/>
    <%
        DefaultListableBeanFactory beanFactory = ApplicationContextProvider.getBeanFactory();

        EntityManagerFactory emf = beanFactory.getBean("MasterEntityManagerFactory", EntityManagerFactory.class);
        OpenJPAEntityManagerFactory oemf = OpenJPAPersistence.cast(emf);

        QueryStatistics<QueryKey> queryStatistics =
                ((QueryResultCacheImpl) oemf.getQueryResultCache()).getDelegate().getStatistics();

        CacheStatisticsImpl statistics = (CacheStatisticsImpl) oemf.getStoreCache().getStatistics();

        String action = request.getParameter("do");
        StringBuilder info = new StringBuilder(512);

        if ("activate".equals(action) && !statistics.isEnabled()) {
            statistics.enable();
            info.append("Statistics enabled\n");
        } else if ("deactivate".equals(action) && !statistics.isEnabled()) {
            statistics.disable();
            info.append("Statistics disabled\n");
        } else if ("clear".equals(action)) {
            queryStatistics.reset();
            statistics.reset();
            info.append("Statistics cleared\n");
        }

        FastDateFormat sdf = FastDateFormat.getInstance(SyncopeConstants.DEFAULT_DATE_PATTERN);
        if (info.length() > 0) {
    %>
    <p/><div class="success">
      <c:out value="${fn:escapeXml(info)}"/>
    </div>
    <%                    }%>
    <p/>
    <a href="?">Reload</a>
    <p/>
    <a href="?do=<%=(statistics.isEnabled() ? "deactivate" : "activate")%>">
      <%=(statistics.isEnabled() ? "DEACTIVATE" : "ACTIVATE")%></a>
    <a href="?do=clear">CLEAR</a>
    <p/>
    Last update: <%=sdf.format(statistics.since())%><br/>
    Activation: <%=sdf.format(statistics.start())%><br/>
    <p/>
    <table>
      <tr>
        <th class="c bd1 bg1">Hits</th>
        <td><%=statistics.getHitCount()%></td>
      </tr>
      <tr>
        <th class="c bd1 bg1">Reads</th>
        <td><%=statistics.getReadCount()%></td>
      </tr>
      <tr>
        <th class="c bd1 bg1">Writes</th>
        <td><%=statistics.getWriteCount()%></td>
      </tr>
      <tr>
        <th class="c bd1 bg1">Query Hits</th>
        <td><%=queryStatistics.getHitCount()%></td>
      </tr>
      <tr>
        <th class="c bd1 bg1">Query Executions</th>
        <td><%=queryStatistics.getExecutionCount()%></td>
      </tr>
      <tr>
        <th class="c bd1 bg1">Query Evictions</th>
        <td><%=queryStatistics.getEvictionCount()%></td>
      </tr>
    </table>
    <p/>
    <table width="100%">
      <tr><th colspan="3" class="c bd1 bg2">Query statistics</th></tr>
      <tr>
        <th class="c bd1 bg1">Query</th>
        <th class="c bd1 bg1">Hits</th>
        <th class="c bd1 bg1">Executions</th>
      </tr>
      <%
          boolean odd = true;
          for (QueryKey key : queryStatistics.keys()) {
      %>
      <tr class="<%=(odd ? "odd" : "even")%>">
        <td><%=key%></td>
        <td><%=queryStatistics.getHitCount(key)%></td>
        <td><%=queryStatistics.getExecutionCount(key)%></td>
      </tr>
      <%
              odd = !odd;
          }
      %>
    </table>
    <p/>
    <table width="100%">
      <tr><th colspan="4" class="c bd1 bg2">2nd level cache statistics</th></tr>
      <tr>
        <th class="c bd1 bg1">Region</th>
        <th class="c bd1 bg1">Hits</th>
        <th class="c bd1 bg1">Reads</th>
        <th class="c bd1 bg1">Writes</th>
      </tr>
      <%
          odd = true;
          for (String className : statistics.classNames()) {
      %>
      <tr class="<%=(odd ? "odd" : "even")%>">
        <td><%=className%></td>
        <td><%=statistics.getHitCount(className)%></td>
        <td><%=statistics.getReadCount(className)%></td>
        <td><%=statistics.getWriteCount(className)%></td>
      </tr>
      <%
              odd = !odd;
          }
      %>
    </table>
  </body>
</html>
