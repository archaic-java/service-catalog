---
name: maintain-service-catalog
description: "Find, consume, implement, evolve, or review work.archaic service contracts and provider-conformance checks. Use for the archaic-java/service-catalog repository, questions about ownership between Archaic Java and project documentation, contract compatibility, logging, testing, SQLite, compiler, CLI, build metadata, Markdown, or site-generation capabilities. Complements Archaic Java and provider/consumer project skills."
---

# Maintain Archaic Service Catalog

Use this shared entry point for human contributors and coding agents. Read the
foundation, select the task, and open the capability you need. Read other contracts
only when the change crosses their boundaries.

## Establish the foundation

- Treat this repository as the catalog for the `work.archaic` namespace. Keep APIs,
  required data types and portable expectations here; consumers may use other catalogs.
- Preserve published versioned contracts. Require a new version for incompatible
  signatures or semantics; do not derive a new promise from one provider's behavior.
- Keep provider implementations and deployment choices in their owning projects.
  Follow each capability's composition rules rather than assuming every interface
  uses ServiceLoader.
- Use a full JDK 25, named JPMS modules and the README's canonical commands. Keep
  provider dependencies out of the catalog.
- Distinguish standalone catalog checks from reusable provider-conformance cases.
  Do not claim coverage for a capability merely because the catalog compiles.
- Read [documentation ownership](references/ownership.md) when deciding whether a
  fact belongs in Archaic Java, a project skill, catalog guidance or API Javadoc.

## Choose the task

| Task | Read next |
|---|---|
| Choose or consume a capability | Its guide/API below, then your consumer project's composition guidance. |
| Implement or verify a provider | Its guide/API below and [conformance](references/conformance.md). |
| Add or evolve a contract | [Contract evolution](references/evolution.md), then the relevant guide/API and conformance entry point. |
| Extend compliance cases or interpret test evidence | [Conformance](references/conformance.md); link each assertion to an existing public promise. |
| Build, diagnose or review this repository | [Contributing](references/contributing.md). |
| Decide where information belongs or resolve conflicting guidance | [Ownership](references/ownership.md). |

## Find the capability

Use `work.archaic.service.<capability>.<version>` for the packages below. Follow the
API link for declarations and precise method-level Javadoc. A guide explains the
connected behavioral model; an absent guide does not imply an undocumented guarantee.
The [module descriptor](../../src/work.archaic.service.catalog/module-info.java)
identifies exported packages.

| Capability/version | Guide | API source |
|---|---|---|
| logging v03: configured contexts | [Current logging](../../docs/logging-v03.md) | [logging/v03](../../src/work.archaic.service.catalog/work/archaic/service/logging/v03) |
| logging v02: goals and Diagnostics | [Logging v02](../../docs/logging-v02.md) | [logging/v02](../../src/work.archaic.service.catalog/work/archaic/service/logging/v02) |
| logging v01: goals and GoalProvider | [Logging v01](../../docs/logging-v01.md) | [logging/v01](../../src/work.archaic.service.catalog/work/archaic/service/logging/v01) |
| test v02: registered cases and evidence | [Testing v02](../../docs/test-v02.md) | [test/v02](../../src/work.archaic.service.catalog/work/archaic/service/test/v02) |
| test v01: annotated suites | Package Javadoc at the API link | [test/v01](../../src/work.archaic.service.catalog/work/archaic/service/test/v01) |
| sqlite v01: scoped local database access | [SQLite](../../docs/sqlite-v01.md), [guarantee/test ledger](../../docs/sqlite-v01-guarantees.md) | [sqlite/v01](../../src/work.archaic.service.catalog/work/archaic/service/sqlite/v01) |
| compiler v01: in-memory syntax analysis | [Compiler](../../docs/compiler-v01.md) | [compiler/v01](../../src/work.archaic.service.catalog/work/archaic/service/compiler/v01) |
| cli v01: command invocation | API source | [cli/v01](../../src/work.archaic.service.catalog/work/archaic/service/cli/v01) |
| build v01: repository metadata | API Javadoc | [build/v01](../../src/work.archaic.service.catalog/work/archaic/service/build/v01) |
| html.sitegenerator v01: site generation | API Javadoc | [html/sitegenerator/v01](../../src/work.archaic.service.catalog/work/archaic/service/html/sitegenerator/v01) |
| markdown v01: Markdown conversion | API Javadoc | [markdown/v01](../../src/work.archaic.service.catalog/work/archaic/service/markdown/v01) |

## Complete the change

Update the authoritative declaration, behavioral guide or compliance case for the
changed fact. Keep the capability index aligned with exports. Run the relevant
checks from the contributor guide and report actual results, including provider
revisions and coverage limitations when claiming provider compliance.
