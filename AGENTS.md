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
# Apache Syncope - AGENTS.md

This document provides context and guidelines for AI agents interacting with the Apache Syncope codebase.

## Project Overview

Apache Syncope is a unified identity management platform that enables consistent management of identities, attributes, group membership, relationships, and authorization across multiple applications and directories (LDAP, Active Directory, databases, SCIM, cloud IdPs).

**Version:** 5.0.0-SNAPSHOT
**License:** Apache-2.0
**Inception Year:** 2010
**Website:** https://syncope.apache.org/
**Build JDK:** OpenJDK 25 (CI also runs on 28-ea)
**Maven:** 3.9.x (CI uses 3.9.6)

## Architecture: Three Domains + Keymaster

Since 4.x the codebase is organized around three parallel domains sharing the same layering:

- **idrepo** - Users, Groups, AnyObjects, Realms, types, schemas, policies
- **idm** - Identity management extensions (roles, delegations, etc.)
- **am** - Access management (auth modules, OIDC clients, SAML SP, MFA)

Each domain is split into `common/<domain>` (API contracts), `core/<domain>` (server logic + REST), `client/<domain>` (Wicket UIs).
**Keymaster** provides key management / encryption and lives in `common/keymaster` + `core/self-keymaster-starter`.

Typical request flow: `client` → JAX-RS interface (`common/*/rest-api`) → implementation (`core/*/rest-cxf`) → logic (`core/*/logic`) → DAO (`core/persistence-api` → `core/persistence-jpa`) → database.

## Multi-Module Maven Structure

```
root (parent POM) [5.0.0-SNAPSHOT]
├── common/
│   ├── keymaster/            - Key management
│   │   ├── client-api/
│   │   ├── client-zookeeper/
│   │   └── self/
│   ├── idrepo/
│   │   ├── lib/              - Shared beans, TOs, utilities
│   │   └── rest-api/         - JAX-RS annotated service interfaces
│   ├── idm/
│   │   ├── lib/
│   │   └── rest-api/
│   └── am/
│       ├── lib/
│       └── rest-api/
├── core/
│   ├── idrepo/               - logic/ + rest-cxf/
│   ├── idm/                  - logic/ + rest-cxf/
│   ├── am/                   - logic/ + rest-cxf/
│   ├── persistence-api/      - Entity + DAO interfaces
│   ├── persistence-common/   - Shared persistence code
│   ├── persistence-jpa/      - Hibernate ORM entities and DAOs
│   ├── persistence-neo4j/    - Neo4j persistence
│   ├── provisioning-api/     - Provisioning interfaces
│   ├── provisioning-java/    - Provisioning managers, push/pull
│   ├── workflow-api/         - Workflow interfaces
│   ├── workflow-java/        - Workflow logic
│   ├── spring/               - Spring Boot auto-configuration, security
│   ├── starter/              - Spring Boot starter
│   ├── self-keymaster-starter/
│   └── metrics-starter/      - Micrometer / metrics
├── client/
│   ├── idrepo/               - common-ui/, console/, enduser/, lib/
│   ├── idm/                  - console/, lib/
│   └── am/                   - console/, enduser/, lib/
├── ext/
│   ├── flowable/             - Workflow engine integration (Flowable)
│   ├── saml2sp4ui/           - SAML 2.0 Service Provider for UIs
│   ├── oidcc4ui/             - OpenID Connect for UIs
│   ├── elasticsearch/        - Elasticsearch persistence
│   ├── opensearch/           - OpenSearch persistence
│   ├── scimv2/               - SCIM v2 API
│   └── openfga/              - OpenFGA authorization
├── wa/                       - Web Access (legacy): bootstrap/, starter/ with CAS
├── fit/                      - Functional integration tests
│   ├── build-tools/
│   ├── persistence-embedded/
│   ├── core-reference/
│   ├── console-reference/
│   ├── enduser-reference/
│   └── wa-reference/
├── standalone/               - Standalone distribution
├── docker/                   - Docker images (console/, core/, enduser/, wa/)
├── archetype/                - Maven archetype
└── src/main/asciidoc/        - Reference guide / getting started sources
```

Note: Kubernetes Helm charts are **not** a top-level module; they live under `docker/src/main/resources/kubernetes/`.

## Key Technologies and Dependencies

### Core stack (versions from root `pom.xml`)

| Component | Version | Notes |
|-----------|---------|-------|
| Java / `targetJdk` | 25 | `maven-compiler-plugin` `<release>25</release>` |
| Spring Boot | 4.2.0-RC2 | Provides BOM, auto-configuration, embedded Tomcat |
| Apache CXF | 4.2.4 | JAX-RS (REST) endpoints |
| Hibernate ORM | 7.4.15.Final | JPA provider (Jakarta Persistence), build-time enhancement |
| Spring Security | 7.x | Via `spring-boot-starter-security` |
| Apache Wicket | 11.0.0 | Console / Enduser UIs (not React/Vue) |
| Swagger / OpenAPI | 3.0.0 | + `swagger-ui` 5.34.1 |
| Tomcat | 11.0.27 (from Spring Boot BOM line) | Embedded container |

### Security & identity protocols

- **Spring Security 7** - authentication/authorization in core and clients (no Apache Shiro anymore)
- **Pac4j 6.6.1** - SAML 2.0 (`ext/saml2sp4ui`) and OpenID Connect (`ext/oidcc4ui`)
- **CAS 8.1.0-RC3** / CAS client 4.1.1 - used by the legacy `wa` module
- **OpenFGA** (`openfga.version = v1`) - relationship-based authorization, `ext/openfga`
- **SCIM v2** - `ext/scimv2`
- **BouncyCastle 1.86**, **Nimbus JOSE+JWT 10.10** - crypto/JWT support

### Persistence, search, workflow

- **Hibernate ORM 7.4.15.Final** with `jakarta.persistence` annotations, `hibernate-jcache` for
  second-level caching and `hibernate-maven-plugin` for build-time bytecode enhancement
- **Spring Data JPA** - `SyncopeJPARepository` extends `SimpleJpaRepository`
- **Neo4j** - `core/persistence-neo4j`
- **Elasticsearch** 9.5.6 / **OpenSearch** 3.9.0 (Java client 3.11.0) - optional persistence
- **Flowable** 8.0.0 - workflow engine (`ext/flowable`); legacy 6.8.1 also referenced
- **Debezium** 3.4 - CDC integration

### Connectors (ConnId bundles, configured via `connid.*.version`)

LDAP 1.5.11, Active Directory 1.4.0, Azure 3.0.0, SCIM 1.0.5, Okta 3.0.8,
ServiceNow 1.0.4, Database 2.5.0, CSV directory 0.8.10, Kafka 1.0.0, REST 1.2.0, Google Apps 1.4.6.

### Apache Commons (versions in parent POM)

`commons-lang3` 3.22.0, `commons-text` 1.15.0, `commons-codec` 1.22.1, `commons-jexl` 3.7.1;
others (`commons-io`, `commons-collections4`, ...) managed transitively by the Spring Boot BOM.

### Testing stack

| Framework | Version | Purpose |
|-----------|---------|---------|
| JUnit Jupiter | 6.x (Spring Boot BOM) | Unit/integration tests |
| Mockito | 5.x | Mocking |
| AssertJ | 3.x | Fluent assertions |
| Spring Boot Test | 4.2.0-RC2 | Context/integration tests |
| Testcontainers | 2.0.5 | `core/persistence-jpa` and `ext/openfga` tests (FIT uses embedded PostgreSQL + Cargo instead) |
| JaCoCo | | Coverage |

## Agent Guidelines

### Code Quality Standards

1. **Module boundaries**
   - `common/<domain>/rest-api`: JAX-RS **interfaces** only (annotations live here)
   - `core/<domain>/rest-cxf`: implementations, extend `Abstract*Service`
   - `core/<domain>/logic`: business logic, classes usually named `*Logic` / `*LogicOp`
   - `core/persistence-api`: entity and DAO **interfaces**
   - `core/persistence-jpa`: JPA entities (`JPA*`) and DAO implementations (`JPA*DAO`)
   - `ext/*`: optional integrations; keep core free of optional dependencies

2. **Persistence**
   - JPA provider is **Hibernate ORM 7** (Jakarta Persistence); annotations are `jakarta.persistence.*`
   - Entity interfaces live in `core/persistence-api/entity`, implementations in `core/persistence-jpa/entity`
   - DAO interfaces in `core/persistence-api/dao`, implementations in `core/persistence-jpa/dao`
   - Hibernate-specific pieces stay in `core/persistence-jpa`: `HibernateJpaVendorAdapter`, the custom
     `SyncopePostgreSQLDialect`, `DomainJCacheRegionFactory`, `hibernate.default_schema` / cache
     region-prefix configuration and `hibernate-maven-plugin` enhancement
   - The package `org.apache.syncope.core.persistence.jpa.openjpa` is a legacy name: it now hosts a
     Hibernate/JCache cache-entry listener, not OpenJPA code
   - Never mix raw SQL in business logic; use DAOs / JPQL / Criteria

3. **Security-first**
   - Use the existing Spring Security configuration (`core/spring`) and the domain auth modules
   - Validate inputs via Bean Validation (`jakarta.validation`) and existing validators
   - Use parameterized JPA/JPQL queries only; never concatenate user input
   - Never log secrets, passwords, tokens, or key material

4. **Testing**
   - Unit tests: JUnit Jupiter + Mockito in the same module under `src/test/java`
   - Integration/FIT: `fit/core-reference`, `fit/console-reference`, `fit/enduser-reference`, `fit/wa-reference`
     (see [Integration Tests (FIT)](#integration-tests-fit) for commands)
   - Embedded DB tests: `fit/persistence-embedded`
   - Prefer meaningful coverage of logic-bearing classes; exceptions, annotations, enums and
     Spring `@Configuration` classes are lower priority
   - Test-only changes: run the affected module's tests and Checkstyle
   - Production behavior changes: also run `fit/core-reference` when feasible
   - Tests must be independent: never rely on class execution order
   - **Neo4j persistence tests** (`core/persistence-neo4j`) require the `-Pneo4j` profile and a
     running Docker daemon: they start Neo4j containers via the `docker-maven-plugin` and run
     through `maven-failsafe-plugin` (integration tests), e.g.
     `mvn -pl core/persistence-neo4j -Pneo4j verify`. Use `-Dit.test=...` to select individual
     tests (surefire's `-Dtest=...` does not apply).

5. **No compiler warnings**
   - Produce clean compilation output: zero compiler warnings (unchecked, deprecation, raw types, etc.).
   - Fix root causes rather than suppressing warnings with `@SuppressWarnings`.
   - Verify by running `mvn -pl <module> test-compile` and checking for warnings.

6. **Modernizer rules**
   - Code must pass the `modernizer:modernizer` check (e.g. prefer `java.util.Map.of()` over traditional map construction).
   - Verify by running `mvn -pl <module> modernizer:modernizer` and fixing any reported violations.

### Common Code Patterns

#### JAX-RS service interface (`common/<domain>/rest-api`)

```java
package org.apache.syncope.common.rest.api.service;

@Path("groups")
@Produces({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML })
@Consumes({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML })
public interface GroupService extends AnyService<GroupTO> {
    @GET
    @Path("{key}")
    GroupTO read(@PathParam("key") String key);
}
```

#### REST implementation (`core/<domain>/rest-cxf/.../service`)

```java
public class GroupServiceImpl extends AbstractAnyService<GroupTO, GroupCR, GroupUR>
        implements GroupService {

    protected final GroupDAO groupDAO;
    protected final GroupLogicOp logic;

    public GroupServiceImpl(AnySearchCondVisitor v, GroupDAO groupDAO, GroupLogicOp logic) {
        super(v);
        this.groupDAO = groupDAO;
        this.logic = logic;
    }
}
```

There are **no** `@RestResource` / `@RestMethod` annotations in 5.0; use standard JAX-RS plus the `JAXRSService` interfaces.

#### JPA entity (`core/persistence-jpa/.../entity`)

```java
package org.apache.syncope.core.persistence.jpa.entity;

@Entity
@Table(name = JPADelegation.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "delegating_id", "delegated_id" }))
@Cacheable
public class JPADelegation extends AbstractGeneratedKeyEntity implements Delegation {
    public static final String TABLE = "Delegation";

    @ManyToOne(optional = false)
    private JPAUser delegating;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime startDate;
}
```

#### DAO (`core/persistence-api/dao` + `core/persistence-jpa/dao`)

```java
public interface GroupDAO extends AnyDAO<Group> { ... }

// implementation in core/persistence-jpa/dao, e.g. JPAGroupDAO
```

#### Provisioning (`core/provisioning-java`)

```java
public class DefaultGroupProvisioningManager implements GroupProvisioningManager {
    // push / pull / deprovision logic
}
```

#### Client UI (`client/<domain>`)

The Console and Enduser are **Apache Wicket 11** applications on Spring Boot
(`wicket-spring-boot-starter`), not React/Vue/Angular.

#### Unit tests in `core/spring`

`core/spring` hosts most of the pure unit tests for security, policies, JWS and task
execution. Follow these patterns:

- Use JUnit Jupiter + Mockito; plain `Mockito.mock(...)` is fine, `MockitoExtension` is optional.
- Put the test in the same package as the class under test when protected members need to be
  exercised (for example filters' `doFilterInternal` or rules' `enforce`).
- Use `mockStatic(...)` and `mockConstruction(...)` for static factories and collaborators
  created with `new` (for example `ImplementationManager` and `RestTemplate`).
- Save and restore global static state in `@BeforeEach` / `@AfterEach`:
  - `ApplicationContextProvider` (application context **and** bean factory)
  - `SecurityContextHolder` (call `clearContext()`)
  - JCache caches used by throttlers
- When a test needs `ApplicationContextProvider`, build a `StaticApplicationContext`, register
  the required beans (for example `SecurityProperties`, `UserDAO`), and restore the previous
  state afterwards.
- Do **not** leave `ApplicationContextProvider` set to `null`: Spring Test caches application
  contexts and will not re-invoke `ApplicationContextAware` on a cached context, which can break
  unrelated tests in the same module run.
- For time-dependent throttlers, override `now()` or inject a clock supplier instead of sleeping.
- Respect Checkstyle `ImportOrder`: static imports first (alphabetical, separate group), then
  non-static imports alphabetically.

## Build Commands

### Maven

```bash
# Fast local build (skip tests)
mvn clean install -DskipTests

# Full build including unit + integration (FIT) tests - slow
mvn clean install

# Everything except the FIT integration tests
mvn clean install -DskipITs

# Single module + required upstream modules
mvn clean install -pl core/idrepo -am

# Tests only (ignore failures to inspect all)
mvn test -Dmaven.test.failure.ignore=true

# Unit tests for one module plus Checkstyle
mvn -pl core/spring test checkstyle:check

# Single test class / selected test classes
mvn -pl core/spring -Dtest=MyTest test
mvn -pl core/spring -Dtest='MyTest,OtherTest' -DfailIfNoTests=false test

# Disable Maven build cache (avoids stale results when verifying fixes)
mvn -pl <module> ... -Dmaven.build.cache.enabled=false

# JPA persistence tests against a real DBMS via Docker (default: embedded PostgreSQL)
mvn -pl core/persistence-jpa -Pmysql test
mvn -pl core/persistence-jpa -Pmariadb test
mvn -pl core/persistence-jpa -Poracle test

# Static checks used by CI
mvn checkstyle:check tools:verify-legal-files modernizer:modernizer apache-rat:check

# Site / docs (AsciiDoc reference guide)
mvn clean generate-resources site-deploy -Psite

# Docker profile (adds docker/ module)
mvn clean install -Pdocker
```

### Integration Tests (FIT)

The `fit/` modules are part of the default reactor and are **not** unit tests: they start an
embedded PostgreSQL, deploy WARs into an embedded Tomcat 11 (Cargo) and exercise the REST API,
the Console (WicketTester, in-process) and the Enduser. A plain `mvn clean install` therefore
runs them too - use `-DskipITs` (or `-DskipTests`) to skip them.

| Module | Content |
|--------|---------|
| `fit/build-tools` | Shared WAR deployed by all FIT setups (ConnId bundles, test resources) |
| `fit/persistence-embedded` | Embedded PostgreSQL (`io.zonky.test`) behind the `embedded` Spring profile |
| `fit/core-reference` | **All IT sources**: Core REST (`org.apache.syncope.fit.core`), Console (`...fit.console`), Enduser (`...fit.enduser`) |
| `fit/console-reference` | Console WAR (deployed by `fit/wa-reference`) |
| `fit/enduser-reference` | Enduser WAR (deployed by `fit/wa-reference`) |
| `fit/wa-reference` | WA WAR plus `OIDCC4UIITCase`, `OpenFGAUIITCase`, `SAML2SP4UIITCase` (`org.apache.syncope.fit.ui`) |

FIT modules resolve project artifacts from the local repository (not from a reactor), so
install everything first - this is exactly what CI does before running any FIT job:

```bash
mvn -U -T 1C -P 'skipTests,all'
```

Core FIT runs `maven-failsafe-plugin` (`*ITCase`) during `verify`, deploying the WAR to
`http://localhost:9080/syncope` (property `cargo.servlet.port`):

```bash
# Default: embedded PostgreSQL + all extensions (Spring profile "embedded,all")
mvn -f fit/core-reference/pom.xml verify

# Single IT / several ITs - -Dit.test is failsafe; -Dtest only reaches surefire
mvn -f fit/core-reference/pom.xml verify -Dit.test=RESTITCase
mvn -f fit/core-reference/pom.xml verify -Dit.test='RESTITCase,UserITCase'

# Same flags as CI, to silence unrelated plugins
mvn -f fit/core-reference/pom.xml verify -Dit.test=RESTITCase \
  -Dinvoker.streamLogs=true -Dmodernizer.skip=true -Drat.skip=true \
  -Dcheckstyle.skip=true -Djacoco.skip=true
```

Backend-specific runs use the `*-it` profiles below. They start containers with
`docker-maven-plugin` (Docker daemon required). `elasticsearch-it`, `opensearch-it`,
`mysql-it`, `mariadb-it`, `oracle-it`, `debezium-it` and `syncrepl-it` restrict failsafe to
`org/apache/syncope/fit/core/*ITCase` (Console/Enduser ITs are not run); `zookeeper-it` and
`neo4j-it` run the full suite:

| Profile | Spring profile / container |
|---------|----------------------------|
| `zookeeper-it` | `embedded,zookeeper` - ZooKeeper (keymaster) |
| `elasticsearch-it` | `embedded,elasticsearch` - Elasticsearch + Kibana |
| `opensearch-it` | `embedded,opensearch` - OpenSearch |
| `mysql-it` | `embedded,mysql` - MySQL |
| `mariadb-it` | `embedded,mariadb` - MariaDB |
| `oracle-it` | `embedded,oracle` - Oracle |
| `neo4j-it` | `embedded,neo4j` - Neo4j |
| `debezium-it` | MySQL + Kafka Connect, only `DebeziumITCase` |
| `syncrepl-it` | OpenLDAP, only `SyncReplITCase` |

```bash
mvn -f fit/core-reference/pom.xml -P mysql-it
mvn -f fit/core-reference/pom.xml -P elasticsearch-it
```

#### External search backends: index refresh and IT timing

When the `elasticsearch-it` / `opensearch-it` profile is active, `AnySearchDAO` is backed by the
external index. User / Group / AnyObject documents are written by
`ElasticsearchIndexManager#entity` / `OpenSearchIndexManager#entity`, which are
`@TransactionalEventListener`s invoked after commit; unlike the Realm branch, they do **not**
pass `refresh(Refresh.True)`. Changes therefore become searchable only after the index refresh
interval (default 1s), which can be longer on loaded CI runners.

This makes ITs that assert exact counts immediately after create / update / delete racy; the
`awaitIfExtSearchEnabled()` helper in `AbstractITCase` is only a fixed ~2s sleep, not a real
readiness check. Prefer Awaitility polling until the expected state is observed, re-reading all
involved values inside the retry loop. Also note that with external search `orderBy` does not
participate in `AnySearchDAO.count(...)`, so comparing two counts only checks whether the index
changed between the two requests. These failures typically reproduce only in CI (containerised
ES/OS with limited heap), not locally.

WA FIT deploys Core, Console, Enduser and WA WARs over HTTPS (9443) and starts `httpbin`
and `openfga` containers:

```bash
mvn -f fit/wa-reference/pom.xml verify -Dit.test=OIDCC4UIITCase
mvn -f fit/wa-reference/pom.xml verify -Dit.test=OpenFGAUIITCase

# SAML2SP4UI additionally needs the test keystore on its test classpath
mkdir -p fit/wa-reference/target/test-classes
cp fit/core-reference/src/test/resources/saml.keystore.jks fit/wa-reference/target/test-classes/
mvn -f fit/wa-reference/pom.xml verify -Dit.test=SAML2SP4UIITCase
```

`-P debug` and `-P hotswap` (all `fit/*-reference` modules) change the default goal to
`clean verify cargo:run`: the ITs are skipped and Tomcat stays in the foreground with JDWP on
port 8000 for interactive debugging.

Reports: `target/failsafe-reports/`; Cargo/Tomcat logs under `target/cargo/logs/`.

### Profiles

| Profile | Purpose |
|---------|---------|
| `skipTests` | Skip tests via profile (used by CI build step) |
| `docker` | Adds the `docker` module / image build |
| `site` | AsciiDoc site generation and deployment |
| `mysql`, `mariadb`, `oracle` | `core/persistence-jpa` only: run its tests against the matching DBMS via Docker/Testcontainers instead of embedded PostgreSQL |
| `zookeeper-it`, `elasticsearch-it`, `opensearch-it`, `mysql-it`, `mariadb-it`, `oracle-it`, `neo4j-it`, `debezium-it`, `syncrepl-it` | `fit/core-reference` only: run FIT against the matching backend/container - see [Integration Tests (FIT)](#integration-tests-fit) |
| `debug`, `hotswap` | `fit/*-reference` only: start the container for interactive debugging instead of running ITs |

Hibernate ORM is a regular release dependency: no Apache snapshot repository is required for the
JPA provider (CI builds use `-P 'skipTests,all'`).

### Docker Compose

Compose files live in `docker/src/main/resources/docker-compose/`:

```bash
cd docker/src/main/resources/docker-compose

docker compose -f docker-compose-all.yml up -d --build        # full stack, PostgreSQL
docker compose -f docker-compose-postgresql.yml up -d
docker compose -f docker-compose-mysql.yml up -d
docker compose -f docker-compose-mariadb.yml up -d
docker compose -f docker-compose-oracle.yml up -d
docker compose -f docker-compose-ha.yml up -d                 # HA setup
```

`docker-compose-all.yml` is the full deployment (Core, Console, Enduser, WA, Keymaster/Zookeeper)
on **PostgreSQL**, not MySQL.

### Kubernetes (Helm)

```bash
kubectl apply -f docker/src/main/resources/kubernetes/syncope/
# or Helm
helm install syncope ./docker/src/main/resources/kubernetes/syncope
helm install postgres ./docker/src/main/resources/kubernetes/postgres
```

Charts: `docker/src/main/resources/kubernetes/syncope/` (Core, Console, Enduser)
and `docker/src/main/resources/kubernetes/postgres/`.

## Database Support

- **PostgreSQL** - default for Docker/K8s
- **MySQL**, **MariaDB**, **Oracle**
- **Neo4j** - via `core/persistence-neo4j` (GitHub Actions workflow `neo4j.yml`)
- **Elasticsearch / OpenSearch** - via `ext/elasticsearch` / `ext/opensearch`
- Embedded DB for tests - `fit/persistence-embedded`

Spring profiles select the database in Docker (`docker,postgresql,saml2`, `docker,mysql`, ...).

## CI / Quality Gates

CI is on **GitHub Actions** (`.github/workflows/`), not Jenkins:

- `crosschecks.yml` - build + validate + test matrix on JDK 25 and 28-ea, Linux/Windows/macOS
- `codeql-analysis.yml` - CodeQL security analysis
- `dockerhub.yml` - Docker image publication
- `fit_*.yml` - FIT suites: `fit_Tomcat_PostgreSQL.yml` (default `fit/core-reference`), `fit_Elasticsearch.yml`, `fit_OpenSearch.yml`, `fit_Zookeeper.yml`, `fit_Tomcat_LiveSync.yml` (`debezium-it` + `syncrepl-it`), `fit_WA_*.yml`
- `mysql.yml`, `mariadb.yml`, `oracle.yml`, `neo4j.yml` - `core/persistence-jpa` (or `-neo4j`) unit tests + the matching `fit/core-reference` `*-it` profile

Every FIT job starts with `mvn -U -T 1C -P 'skipTests,all'` and then runs
the corresponding `mvn -f fit/...` command (see [Integration Tests (FIT)](#integration-tests-fit)).

Quality tooling: Checkstyle, Modernizer, Apache RAT, JaCoCo, Maven Enforcer.

## Troubleshooting

1. **JDK errors** - build requires JDK 25 (`targetJdk=25`); older JDKs fail at compile.
2. **Hibernate ORM, not OpenJPA** - the JPA provider is Hibernate; no OpenJPA snapshot or
   `use-apache-snapshots` profile is involved. Keep Hibernate-specific code inside
   `core/persistence-jpa` and prefer portable Jakarta Persistence APIs elsewhere.
3. **Checkstyle/Modernizer/RAT failures** - run the same goals as CI (see Build Commands) before submitting.
4. **FIT failures** - default FIT needs no Docker (embedded PostgreSQL via `fit/persistence-embedded`);
   the `*-it` profiles and `fit/wa-reference` start containers with `docker-maven-plugin`, so Docker
   must be running and its images reachable. Install the project first (`-P 'skipTests,...'`),
   otherwise FIT resolves stale artifacts from the local repository.
5. **Docker stack issues** - use the compose files under `docker/src/main/resources/docker-compose/`
   and check `SPRING_PROFILES_ACTIVE` matches the chosen database.
6. **UI changes** - Console/Enduser are Wicket apps; rebuild `client/<domain>` and use the
   `*-reference` FIT modules for regression checks.
7. **Test order interference** - if a test fails only in the full module run, look for global
   static state (`ApplicationContextProvider`, `SecurityContextHolder`, JCache) that was not
   saved and restored around the test.
8. **External-search FIT failures (counts / visibility)** - `elasticsearch-it` and
   `opensearch-it` index writes are near-real-time (no forced refresh for Users / Groups /
   AnyObjects), so exact-count assertions right after mutations can fail only in CI; see
   [External search backends](#external-search-backends-index-refresh-and-it-timing).

## Repository Information

- **Main Repository:** https://github.com/apache/syncope/
- **CI:** GitHub Actions - https://github.com/apache/syncope/actions
- **Issue Tracker:** https://issues.apache.org/jira/browse/SYNCOPE
- **Mailing Lists:** `user@syncope.apache.org`, `dev@syncope.apache.org`, `commits@syncope.apache.org`
  - Archives: https://lists.apache.org/list.html?dev@syncope.apache.org
- **Website & Docs:** https://syncope.apache.org/

## For AI Agents Working with Syncope

1. **Respect the domain layering**: `common/<domain>/rest-api` → `core/<domain>/rest-cxf` → `core/<domain>/logic` → DAO.
2. **JPA is Hibernate ORM + Jakarta Persistence**: keep Hibernate-specific code (dialects, cache,
   vendor adapter, enhancement) inside `core/persistence-jpa`.
3. **No Shiro**: authentication/authorization is Spring Security (core/clients) plus Pac4j and OpenFGA.
4. **UIs are Apache Wicket**, not SPA frameworks.
5. **Optional features belong in `ext/`**; core must not depend on them.
6. **Keep JDK 25 compatibility** and run Checkstyle/Modernizer/RAT-compatible code.
7. **Keep unit tests order-independent** and restore global static state
   (`ApplicationContextProvider`, `SecurityContextHolder`, JCache) after each test.

---

*This AGENTS.md was generated for Apache Syncope 5.0.0-SNAPSHOT to assist AI agents and developers working with the codebase.*
