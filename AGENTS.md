# Apache Syncope - AGENTS.md

This document provides context and guidelines for AI agents interacting with the Apache Syncope codebase.

## Project Overview

Apache Syncope is a unified identity management platform that enables consistent management of identities, attributes, group membership, relationships, and authorization across multiple applications and directories (LDAP, Active Directory, databases, SCIM, cloud IdPs).

**Version:** 4.2.0-SNAPSHOT
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
root (parent POM) [4.2.0-SNAPSHOT]
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
│   ├── persistence-jpa/      - OpenJPA entities and DAOs
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
| Spring Boot | 4.1.2 | Provides BOM, auto-configuration, embedded Tomcat |
| Apache CXF | 4.2.4 | JAX-RS (REST) endpoints |
| OpenJPA | 4.2.0-SNAPSHOT | JPA provider (not Hibernate) |
| Spring Security | 7.x | Via `spring-boot-starter-security` |
| Apache Wicket | 10.12.0 | Console / Enduser UIs (not React/Vue) |
| Swagger / OpenAPI | 3.0.0 | + `swagger-ui` 5.34.1 |
| Tomcat | 11.0.27 (from Spring Boot BOM line) | Embedded container |

### Security & identity protocols

- **Spring Security 7** - authentication/authorization in core and clients (no Apache Shiro anymore)
- **Pac4j 6.5.9** - SAML 2.0 (`ext/saml2sp4ui`) and OpenID Connect (`ext/oidcc4ui`)
- **CAS 8.0.3** / CAS client 4.1.1 - used by the legacy `wa` module
- **OpenFGA** (`openfga.version = v1`) - relationship-based authorization, `ext/openfga`
- **SCIM v2** - `ext/scimv2`
- **BouncyCastle 1.86**, **Nimbus JOSE+JWT 10.10** - crypto/JWT support

### Persistence, search, workflow

- **OpenJPA 4.2.0-SNAPSHOT** with `jakarta.persistence` annotations
- **Spring Data JPA** - `SyncopeJPARepository` extends `SimpleJpaRepository`
- **Neo4j** - `core/persistence-neo4j`
- **Elasticsearch** 9.4.9 / **OpenSearch** 3.9.0 (Java client 3.10.0) - optional persistence
- **Flowable** 8.0.0 - workflow engine (`ext/flowable`); legacy 6.8.1 also referenced
- **Debezium** 3.4 - CDC integration

### Connectors (ConnId bundles, configured via `connid.*.version`)

LDAP 1.5.11, Active Directory 1.4.0, Azure 3.0.0, SCIM 1.0.5, Okta 3.0.8,
ServiceNow 1.0.4, Database 2.5.0, CSV directory 0.8.10, Kafka 1.0.0, REST 1.2.0, Google Apps 1.4.6.

### Apache Commons (versions in parent POM)

`commons-lang3` 3.21.0, `commons-text` 1.15.0, `commons-codec` 1.22.1, `commons-jexl` 3.7.1;
others (`commons-io`, `commons-collections4`, ...) managed transitively by the Spring Boot BOM.

### Testing stack

| Framework | Version | Purpose |
|-----------|---------|---------|
| JUnit Jupiter | 6.x (Spring Boot BOM) | Unit/integration tests |
| Mockito | 5.x | Mocking |
| AssertJ | 3.x | Fluent assertions |
| Spring Boot Test | 4.1.2 | Context/integration tests |
| Testcontainers | via BOM | Elasticsearch, OpenSearch, OpenFGA, Zookeeper, DBs |
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
   - JPA provider is **OpenJPA**, annotations are `jakarta.persistence.*`
   - Entity interfaces live in `core/persistence-api/entity`, implementations in `core/persistence-jpa/entity`
   - DAO interfaces in `core/persistence-api/dao`, implementations in `core/persistence-jpa/dao`
   - Never mix raw SQL in business logic; use DAOs / JPQL / Criteria

3. **Security-first**
   - Use the existing Spring Security configuration (`core/spring`) and the domain auth modules
   - Validate inputs via Bean Validation (`jakarta.validation`) and existing validators
   - Use parameterized JPA/JPQL queries only; never concatenate user input
   - Never log secrets, passwords, tokens, or key material

4. **Testing**
   - Unit tests: JUnit Jupiter + Mockito in the same module under `src/test/java`
   - Integration/FIT: `fit/core-reference`, `fit/console-reference`, `fit/enduser-reference`, `fit/wa-reference`
   - Embedded DB tests: `fit/persistence-embedded`
   - Prefer meaningful coverage of logic-bearing classes; exceptions, annotations, enums and
     Spring `@Configuration` classes are lower priority
   - Test-only changes: run the affected module's tests and Checkstyle
   - Production behavior changes: also run `fit/core-reference` when feasible
   - Tests must be independent: never rely on class execution order

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

There are **no** `@RestResource` / `@RestMethod` annotations in 4.2; use standard JAX-RS plus the `JAXRSService` interfaces.

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

The Console and Enduser are **Apache Wicket 10** applications on Spring Boot
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

# Full build with unit tests
mvn clean install

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

# Static checks used by CI
mvn checkstyle:check tools:verify-legal-files modernizer:modernizer apache-rat:check

# Site / docs (AsciiDoc reference guide)
mvn clean generate-resources site-deploy -Psite

# Docker profile (adds docker/ module)
mvn clean install -Pdocker
```

### Profiles

| Profile | Purpose |
|---------|---------|
| `skipTests` | Skip tests via profile (used by CI build step) |
| `docker` | Adds the `docker` module / image build |
| `site` | AsciiDoc site generation and deployment |
| `jdk27` | Auto-activates on JDK >= 27 to add ByteBuddy deps |
| `use-apache-snapshots` | Needed to resolve `openjpa 4.2.0-SNAPSHOT` |

OpenJPA is a **snapshot** dependency: if resolution fails, ensure the Apache snapshots
repository is enabled (CI uses `-P 'skipTests,all,use-apache-snapshots'`).

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
- `fit_*.yml` - FIT suites (Tomcat, PostgreSQL, Elasticsearch, OpenSearch, LiveSync, WA OIDCC4UI, WA OpenFGA, WA SAML2SP4UI, Zookeeper)
- `mysql.yml`, `mariadb.yml`, `oracle.yml`, `neo4j.yml` - DB-specific runs

Quality tooling: Checkstyle, Modernizer, Apache RAT, JaCoCo, Maven Enforcer.

## Troubleshooting

1. **JDK errors** - build requires JDK 25 (`targetJdk=25`); older JDKs fail at compile.
2. **OpenJPA snapshot not found** - enable Apache snapshots (`-Puse-apache-snapshots`) or build OpenJPA locally.
3. **Checkstyle/Modernizer/RAT failures** - run the same goals as CI (see Build Commands) before submitting.
4. **FIT failures** - most need Testcontainers/Docker; DB-specific suites need the matching DB.
5. **Docker stack issues** - use the compose files under `docker/src/main/resources/docker-compose/`
   and check `SPRING_PROFILES_ACTIVE` matches the chosen database.
6. **UI changes** - Console/Enduser are Wicket apps; rebuild `client/<domain>` and use the
   `*-reference` FIT modules for regression checks.
7. **Test order interference** - if a test fails only in the full module run, look for global
   static state (`ApplicationContextProvider`, `SecurityContextHolder`, JCache) that was not
   saved and restored around the test.

## Repository Information

- **Main Repository:** https://github.com/apache/syncope/
- **CI:** GitHub Actions - https://github.com/apache/syncope/actions
- **Issue Tracker:** https://issues.apache.org/jira/browse/SYNCOPE
- **Mailing Lists:** `user@syncope.apache.org`, `dev@syncope.apache.org`, `commits@syncope.apache.org`
  - Archives: https://lists.apache.org/list.html?dev@syncope.apache.org
- **Website & Docs:** https://syncope.apache.org/

## For AI Agents Working with Syncope

1. **Respect the domain layering**: `common/<domain>/rest-api` → `core/<domain>/rest-cxf` → `core/<domain>/logic` → DAO.
2. **JPA is OpenJPA + Jakarta Persistence**: do not introduce Hibernate-specific APIs.
3. **No Shiro**: authentication/authorization is Spring Security (core/clients) plus Pac4j and OpenFGA.
4. **UIs are Apache Wicket**, not SPA frameworks.
5. **Optional features belong in `ext/`**; core must not depend on them.
6. **Keep JDK 25 compatibility** and run Checkstyle/Modernizer/RAT-compatible code.
7. **Keep unit tests order-independent** and restore global static state
   (`ApplicationContextProvider`, `SecurityContextHolder`, JCache) after each test.

---

*This AGENTS.md was generated for Apache Syncope 4.2.0-SNAPSHOT to assist AI agents and developers working with the codebase.*
