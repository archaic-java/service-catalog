# Archaic Service Catalog

`work.archaic.service.catalog` defines the shared capabilities of the `work.archaic`
namespace: versioned APIs, their data types and behavioral expectations. Consumers
and providers depend on these contracts to keep implementation choices independent.
A consumer may depend on catalogs from more than one namespace.

The catalog also contains reusable provider-conformance cases where available.
They check particular promises; passing them provides evidence of compliance, not
proof of every possible behavior. Runtime providers live in their own projects.
Provider selection is explicit, through ServiceLoader or construction as specified
by the capability. Published contract versions remain compatible.

## Find a contract or make a change

Start with the [catalog skill](skills/maintain-service-catalog/SKILL.md). Human readers
and coding agents use the same task map and contract index; no installation is needed
to follow these links. The skill covers consuming contracts, implementing providers,
evolving contracts and maintaining their compliance checks.

For the division of responsibility between Archaic Java, a project's maintenance
skill and this catalog, read [documentation ownership](skills/maintain-service-catalog/references/ownership.md).
Exact API declarations and Javadoc remain beside the source. Contract guides and
maintenance guidance live together in the skill's `references/` directory, loaded
as needed through its task map and capability index.

The [contract index](skills/maintain-service-catalog/SKILL.md#find-the-capability)
includes every exported package. Frequently used guides include
[logging v03](skills/maintain-service-catalog/references/logging-v03.md), [SQLite v01](skills/maintain-service-catalog/references/sqlite-v01.md),
[testing v02](skills/maintain-service-catalog/references/test-v02.md) and [compiler v01](skills/maintain-service-catalog/references/compiler-v01.md).

## Build and verify

Use a full JDK 25, including `javac`, and run from the repository root:

```sh
javac @cmd/compile
java @cmd/test
```

The command files compile the catalog and its test module, then run standalone
logging data/default-method and text-output checks. They require no provider or
external dependency. They do not execute the reusable provider-conformance cases.
For those entry points and provider-side verification, read
[conformance](skills/maintain-service-catalog/references/conformance.md).
Keep generated classes in ignored `out/`.
