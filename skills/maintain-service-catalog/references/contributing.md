# Maintain the catalog repository

Read this when setting up the checkout, extending its modules or reviewing a change.
Use the canonical [README commands](../../../README.md#build-and-verify).

## Source and command map

| Location | Responsibility |
|---|---|
| [Catalog module](../../../src/work.archaic.service.catalog/module-info.java) | Public package exports; no provider dependency. |
| [Catalog source](../../../src/work.archaic.service.catalog/work/archaic/service) | Versioned declarations, Javadoc and contract-owned mechanics. |
| [Test module](../../../src/work.archaic.service.catalog.test/module-info.java) | Standalone checks and exported reusable provider-compliance registrations. |
| [Test source](../../../src/work.archaic.service.catalog.test/work/archaic/service/catalog/test) | Actual check entry points and portable cases. |
| [cmd/compile](../../../cmd/compile) | Compiles both modules with release 25 into `out/`. |
| [cmd/test](../../../cmd/test) | Runs the standalone catalog launcher with assertions enabled. |
| [Capability guides](../../../docs) | Connected behavior, examples and the SQLite guarantee/test ledger. |

The current build needs a full JDK 25 and no sibling checkout, external dependency or
provider. The test module requires `jdk.httpserver` for compilation of HTTP contract
usage and declares `uses ...Sqlite` for its separate provider launcher. Do not infer
that launching `cmd/test` supplies or verifies SQLite. Package exports are the public
boundary; source directories not exported by the module are not advertised capabilities.

## Work through a change

1. Read applicable AGENTS.md instructions and the skill's relevant task/contract links.
   Check worktree status and both `java --version` and `javac --version`.
2. Identify whether the fact belongs in shared conventions, the catalog or a provider/
   consumer project with [ownership](ownership.md). For contract changes, follow
   [evolution](evolution.md) before editing APIs or portable expectations.
3. Change the owning declarations, guide or cases together. Update descriptors and
   command files for module changes; keep provider dependencies outside the catalog.
4. Run the README compile/test commands. Consult [conformance](conformance.md) for
   provider execution needed beyond those checks. No application run entry point is
   required for this library-only catalog.
5. Inspect the diff and report checks actually run. Exclude generated classes, temporary
   fixtures and downloaded tools from commits.

Use Archaic Java for shared engineering conventions when available: JDK-first code,
named modules, explicit commands, compatible versioned APIs and inline assertions with
explanations. Preserve purpose-built main-method checks when they independently exercise
catalog defaults or compatibility; do not migrate test infrastructure incidentally.

## Maintain documentation

Keep README purpose and commands short. Keep skill metadata, foundation and task/contract
routing compact; link to the owning guide or declaration rather than restating its full
specification. Keep existing `docs/` URLs stable when reorganizing navigation. Add a table
of contents to long guides so humans and agents can inspect their scope quickly.

For documentation-only changes, validate skill metadata, local Markdown paths/anchors,
and index coverage against module exports. Check behavioral claims and check coverage
against source, distinguishing planned work from executed tests. Compile/run when code,
examples or changed guarantees need runtime verification. State an unavailable JDK as a
limitation rather than claiming checks passed.
