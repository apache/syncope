<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<jsp:useBean id="sessionFactory"
             scope="request"
             type="org.hibernate.SessionFactory"/>
<%@page import="org.springframework.web.context.support.ContextExposingHttpServletRequest"%>
<%@page import="javax.persistence.EntityManagerFactory"%>
<%@page import="org.hibernate.Session"%>
<%@page import="javax.persistence.EntityManager"%>
<%@page import="org.hibernate.stat.Statistics"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%@page import="org.springframework.context.ApplicationContext"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.hql.QueryTranslator"%>
<%@page import="org.hibernate.hql.ast.ASTQueryTranslatorFactory"%>
<%@page import="org.hibernate.engine.SessionFactoryImplementor"%>
<%@page import="org.hibernate.hql.QueryTranslatorFactory"%>
<%@page import="org.hibernate.SessionFactory"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.hibernate.stat.SecondLevelCacheStatistics"%>
<%@page import="org.hibernate.stat.CollectionStatistics"%>
<%@page import="org.hibernate.stat.EntityStatistics"%>
<%@page import="java.text.Collator"%>
<%@page import="java.util.TreeMap"%>
<%@page import="java.util.Collections"%>
<%@page import="org.hibernate.stat.QueryStatistics"%>
<%@page import="java.util.Map"%>
<%!
    static Map<String, QueryStatistics> queryStatistics =
            Collections.synchronizedMap(
            new TreeMap<String, QueryStatistics>(Collator.getInstance()));

    static Map<String, EntityStatistics> entityStatistics =
            Collections.synchronizedMap(
            new TreeMap<String, EntityStatistics>(Collator.getInstance()));

    static Map<String, CollectionStatistics> collectionStatistics =
            Collections.synchronizedMap(
            new TreeMap<String, CollectionStatistics>(Collator.getInstance()));

    static Map<String, SecondLevelCacheStatistics> secondLevelCacheStatistics =
            Collections.synchronizedMap(
            new TreeMap<String, SecondLevelCacheStatistics>(
            Collator.getInstance()));

    static List<Long> generalStatistics = Collections.synchronizedList(
            new ArrayList<Long>(18));

    static {
        for (int i = 0; i < 9; i++) {
            generalStatistics.add(new Long(-1));
        }
    }
    static Date lastUpdate;

    static Date activation;

    static Date deactivation;

    public static class HqlToSqlTranslator {

        public String toSql(SessionFactory sessionFactory,
                String hqlQueryText) {

            if (hqlQueryText != null) {
                final QueryTranslatorFactory ast =
                        new ASTQueryTranslatorFactory();
                final SessionFactoryImplementor factory =
                        (SessionFactoryImplementor) sessionFactory;
                final QueryTranslator newQueryTranslator =
                        ast.createQueryTranslator(
                        hqlQueryText,
                        hqlQueryText,
                        Collections.EMPTY_MAP, factory);
                try {
                    newQueryTranslator.compile(Collections.EMPTY_MAP, false);
                    return newQueryTranslator.getSQLString();
                } catch (Throwable t) {
                    return hqlQueryText;
                }
            }

            return null;
        }
    }
    static HqlToSqlTranslator translator = new HqlToSqlTranslator();

    public static class StringUtils {

        public static final String format(final Date date) {
            if (date == null) {
                return null;
            }
            final SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd.MM.yy HH:mm:ss");
            return sdf.format(date);
        }

        public static final String formatTime(final long millis) {
            Date date = new Date(millis);

            return format(date);
        }
    }
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Hibernate statistics</title>
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
                    final Statistics statistics =
                            sessionFactory.getStatistics();

                    final String action = request.getParameter("do");
                    final StringBuilder info = new StringBuilder(512);

                    if ("activate".equals(action)
                            && !statistics.isStatisticsEnabled()) {

                        statistics.setStatisticsEnabled(true);
                        activation = new Date();
                        info.append("Statistics enabled\n");
                    } else if ("deactivate".equals(action)
                            && statistics.isStatisticsEnabled()) {

                        statistics.setStatisticsEnabled(false);
                        deactivation = new Date();
                        info.append("Statistics disabled\n");
                    } else if ("clear".equals(action)) {
                        activation = null;
                        deactivation = null;
                        statistics.clear();
                        generalStatistics.set(0, new Long(0));
                        generalStatistics.set(1, new Long(0));

                        generalStatistics.set(2, new Long(0));
                        generalStatistics.set(3, new Long(0));

                        generalStatistics.set(4, new Long(0));
                        generalStatistics.set(5, new Long(0));

                        generalStatistics.set(6, new Long(0));
                        generalStatistics.set(7, new Long(0));
                        generalStatistics.set(8, new Long(0));
                        queryStatistics.clear();
                        entityStatistics.clear();
                        collectionStatistics.clear();
                        secondLevelCacheStatistics.clear();
                        info.append("Statistics cleared\n");
                    }

                    boolean active = statistics.isStatisticsEnabled();
                    if (info.length() > 0) {
        %>
        <p/><div class="success">
            <c:out value="${fn:escapeXml(info)}"/>
        </div>
        <%                    }%>
        <p/>
        <a href="?">Reload</a>
        <p/>
        <a href="?do=<%=(active ? "deactivate" : "activate")%>">
            <%=(active ? "DEACTIVATE" : "ACTIVATE")%></a> |
        <a href="?do=clear">CLEAR</a>
        <%
                    if (active) {
                        lastUpdate = new Date();
                        String[] names;

                        generalStatistics.set(0, statistics.getConnectCount());
                        generalStatistics.set(1, statistics.getFlushCount());

                        generalStatistics.set(2, statistics.
                                getPrepareStatementCount());
                        generalStatistics.set(3, statistics.
                                getCloseStatementCount());

                        generalStatistics.set(4,
                                statistics.getSessionCloseCount());
                        generalStatistics.set(5,
                                statistics.getSessionOpenCount());

                        generalStatistics.set(6,
                                statistics.getTransactionCount());
                        generalStatistics.set(7, statistics.
                                getSuccessfulTransactionCount());
                        generalStatistics.set(8, statistics.
                                getOptimisticFailureCount());

                        queryStatistics.clear();
                        names = statistics.getQueries();
                        if (names != null && names.length > 0) {
                            for (int i = 0;
                                    i < names.length; i++) {

                                queryStatistics.put(names[i],
                                        statistics.getQueryStatistics(
                                        names[i]));
                            }
                        }

                        entityStatistics.clear();
                        names = statistics.getEntityNames();
                        if (names != null && names.length > 0) {
                            for (int i = 0; i
                                    < names.length; i++) {
                                entityStatistics.put(names[i],
                                        statistics.getEntityStatistics(
                                        names[i]));
                            }
                        }

                        collectionStatistics.clear();
                        names = statistics.getCollectionRoleNames();
                        if (names != null && names.length > 0) {
                            for (int i = 0; i
                                    < names.length; i++) {
                                collectionStatistics.put(names[i],
                                        statistics.getCollectionStatistics(
                                        names[i]));
                            }
                        }

                        secondLevelCacheStatistics.clear();
                        names = statistics.getSecondLevelCacheRegionNames();
                        if (names != null && names.length > 0) {
                            for (int i = 0; i
                                    < names.length; i++) {
                                secondLevelCacheStatistics.put(names[i],
                                        statistics.getSecondLevelCacheStatistics(
                                        names[i]));
                            }
                        }
                    }

        %>
        <p/>
	Last update: <%=(lastUpdate != null
                                ? StringUtils.format(lastUpdate) : "none")%><br/>
	Activation: <%=(activation != null
                                ? StringUtils.format(activation) : "none")%><br/>
	Deactivation: <%=(deactivation != null
                                ? StringUtils.format(deactivation) : "none")%><br/>
	Active duration: <%=(activation != null
                                ? StringUtils.formatTime((deactivation != null
                                ? deactivation.getTime()
                                : new Date().getTime()) - activation.getTime())
                                : "none")%>
        <p/>
        <%
                    boolean hasGeneral = false;
                    for (int i = 0; i < 9; i++) {
                        if (generalStatistics.get(i).longValue() > -1) {
                            hasGeneral = true;
                            break;
                        }
                    }
                    if (hasGeneral) {
        %>
        <table>
            <tr>
                <th class="c bd1 bg1">Connects</th>
                <td><%=generalStatistics.get(0)%></td>
            </tr>
            <tr>
                <th class="c bd1 bg1">Flushes</th>
                <td><%=generalStatistics.get(1)%></td>
            </tr>
            <tr>
                <th class="c bd1 bg1">Prepare statements</th>
                <td><%=generalStatistics.get(2)%></td>
            </tr>
            <tr>
                <th class="c bd1 bg1">Close statements</th>
                <td><%=generalStatistics.get(3)%></td>
            </tr>
            <tr>
                <th class="c bd1 bg1">Session opens</th>
                <td><%=generalStatistics.get(5)%></td>
            </tr>
            <tr>
                <th class="c bd1 bg1">Session closes</th>
                <td><%=generalStatistics.get(4)%></td>
            </tr>
            <tr>
                <th class="c bd1 bg1">Total Transactions</th>
                <td><%=generalStatistics.get(6)%></td>
            </tr>
            <tr>
                <th class="c bd1 bg1">Successfull Transactions</th>
                <td><%=generalStatistics.get(7)%></td>
            </tr>
            <tr>
                <th class="c bd1 bg1">Optimistic failures</th>
                <td><%=generalStatistics.get(8)%></td>
            </tr>
        </table>
        <p/>
        <%                    }%>

        <%
                    if (queryStatistics.size() > 0) {
        %>
        <table width="100%">
            <tr><th colspan="11" class="c bd1 bg2">Query statistics</th></tr>
            <tr>
                <th class="c bd1 bg1">HQL Query</th>
                <th class="c bd1 bg1">SQL Query</th>
                <th class="c bd1 bg1">Calls</th>
                <th class="c bd1 bg1">Total rowcount</th>
                <th class="c bd1 bg1">Max dur.</th>
                <th class="c bd1 bg1">Min dur.</th>
                <th class="c bd1 bg1">Avg dur.</th>
                <th class="c bd1 bg1">Total dur.</th>
                <th class="c bd1 bg1">Cache hits</th>
                <th class="c bd1 bg1">Cache miss</th>
                <th class="c bd1 bg1">Cache put</th>
            </tr>
            <%
                                    QueryStatistics queryStats;
                                    boolean odd = true;
                                    for (String query :
                                            queryStatistics.keySet()) {

                                        queryStats = queryStatistics.get(query);
            %>
            <tr class="<%=(odd ? "odd" : "even")%>">
                <td class="t"><%=query%></td>
                <td><small><%=translator.toSql(sessionFactory, query)%></small></td>
                <td class="t"><%=queryStats.getExecutionCount()%></td>
                <td class="t"><%=queryStats.getExecutionRowCount()%></td>
                <td class="t"><%=queryStats.getExecutionMaxTime()%></td>
                <td class="t"><%=queryStats.getExecutionMinTime()%></td>
                <td class="t"><%=queryStats.getExecutionAvgTime()%></td>
                <td class="t">
                    <%=queryStats.getExecutionAvgTime() * queryStats.
                                            getExecutionCount()%>
                </td>
                <td class="t"><%=queryStats.getCacheHitCount()%></td>
                <td class="t"><%=queryStats.getCacheMissCount()%></td>
                <td class="t"><%=queryStats.getCachePutCount()%></td>
            </tr>
            <%
                                        odd = !odd;
                                    }
            %>
        </table>
        <p/>
        <%
                    }

                    if (entityStatistics.size() > 0) {
        %>
        <table width="100%">
            <tr><th colspan="7" class="c bd1 bg2">Entity statistics</th></tr>
            <tr>
                <th class="c bd1 bg1">Entity</th>
                <th class="c bd1 bg1">Loads</th>
                <th class="c bd1 bg1">Fetches</th>
                <th class="c bd1 bg1">Inserts</th>
                <th class="c bd1 bg1">Updates</th>
                <th class="c bd1 bg1">Deletes</th>
                <th class="c bd1 bg1">Optimistic failures</th>
            </tr>
            <%
                                    EntityStatistics entityStats;
                                    boolean odd = true;
                                    for (String entity :
                                            entityStatistics.keySet()) {

                                        entityStats = entityStatistics.get(
                                                entity);
            %>
            <tr class="<%=(odd ? "odd" : "even")%>">
                <td><%=entity%></td>
                <td><%=entityStats.getLoadCount()%></td>
                <td><%=entityStats.getFetchCount()%></td>
                <td><%=entityStats.getInsertCount()%></td>
                <td><%=entityStats.getUpdateCount()%></td>
                <td><%=entityStats.getDeleteCount()%></td>
                <td><%=entityStats.getOptimisticFailureCount()%></td>
            </tr>
            <%
                                        odd = !odd;
                                    }
            %>
        </table>
        <p/>
        <%
                    }

                    if (collectionStatistics.size() > 0) {
        %>
        <table width="100%">
            <tr><th colspan="6" class="c bd1 bg2">Collection statistics</th></tr>
            <tr>
                <th class="c bd1 bg1">Role</th>
                <th class="c bd1 bg1">Loads</th>
                <th class="c bd1 bg1">Fetches</th>
                <th class="c bd1 bg1">Updates</th>
                <th class="c bd1 bg1">Recreate</th>
                <th class="c bd1 bg1">Remove</th>
            </tr>
            <%
                                    CollectionStatistics collectionStats;
                                    boolean odd = true;

                                    for (String collection :
                                            collectionStatistics.keySet()) {

                                        collectionStats = collectionStatistics.
                                                get(collection);
            %>
            <tr class="<%=(odd ? "odd" : "even")%>">
                <td><%=collection%></td>
                <td><%=collectionStats.getLoadCount()%></td>
                <td><%=collectionStats.getFetchCount()%></td>
                <td><%=collectionStats.getUpdateCount()%></td>
                <td><%=collectionStats.getRecreateCount()%></td>
                <td><%=collectionStats.getRemoveCount()%></td>
            </tr>
            <%
                                        odd = !odd;
                                    }
            %>
        </table>
        <p/>
        <%
                    }

                    if (secondLevelCacheStatistics.size() > 0) {
                        long totalSizeInMemory = 0;
        %>
        <table width="100%">
            <tr><th colspan="7" class="c bd1 bg2">2nd level cache statistics</th></tr>
            <tr>
                <th class="c bd1 bg1">Regionname</th>
                <th class="c bd1 bg1">Puts</th>
                <th class="c bd1 bg1">Hits</th>
                <th class="c bd1 bg1">Misses</th>
                <th class="c bd1 bg1">Elements in memory</th>
                <th class="c bd1 bg1">Size in memory</th>
                <th class="c bd1 bg1">Elements on disk</th>
            </tr>
            <%
                                    SecondLevelCacheStatistics cacheStats;
                                    boolean odd = true;
                                    for (String cache :
                                            secondLevelCacheStatistics.keySet()) {
                                        cacheStats = secondLevelCacheStatistics.
                                                get(cache);
                                        totalSizeInMemory += cacheStats.
                                                getSizeInMemory();
            %>
            <tr class="<%=(odd ? "odd" : "even")%>">
                <td><%=cache%></td>
                <td><%=cacheStats.getPutCount()%></td>
                <td><%=cacheStats.getHitCount()%></td>
                <td><%=cacheStats.getMissCount()%></td>
                <td><%=cacheStats.getElementCountInMemory()%></td>
                <td><%=cacheStats.getSizeInMemory()%></td>
                <td><%=cacheStats.getElementCountOnDisk()%></td>
            </tr>
            <%
                                        odd = !odd;
                                    }
            %>
            <tr>
                <td colspan="5">&nbsp;</td>
                <td><%=totalSizeInMemory%></td>
                <td>&nbsp;</td>
            </tr>
        </table>
        <p/>
        <%          }%>
    </body>
</html>