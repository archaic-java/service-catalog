# Documentation ownership

Read this when documenting a rule, designing a cross-project change, or resolving
contradictory guidance. Ownership follows the scope of the fact. A skill is an entry
point and workflow; it does not make all linked facts belong to that skill's project.

## Choose the authoritative home

| Information | Authoritative home | What other locations should contain |
|---|---|---|
| Shared engineering conventions: JDK-first design, JPMS, argument files, assertion style, documentation structure | Archaic Java skill | A link or short orientation, plus explicit local variations. |
| Public API signatures, data shapes and method-level invariants | Versioned catalog declarations and their Javadoc | Links and task-specific examples using that API. |
| Portable lifecycle, concurrency, failure and composition expectations connecting contract types | Catalog capability guide in `docs/` | A summary sufficient to recognize the constraint, with a link to its definition. |
| Portable provider-compliance assertions | Catalog conformance cases, tied to documented promises | Provider suite wiring and actual execution evidence. Tests cannot invent guarantees. |
| Provider algorithms, native dependencies, defaults allowed by the contract, performance and platform limits | Provider project's source and maintenance skill | Catalog links to an implementation where useful; no assumption all providers behave identically. |
| Consumer choice of provider/version, configuration, application policy and integration | Consumer project's source and maintenance skill | Catalog explanation of what the API permits or requires, without application-specific settings. |
| Repository build/test commands and checkout requirements | That repository's README and command files | Links from its maintenance skill and references. |
| Task routing, source ownership and relevant verification | Maintenance skill for the repository being worked on | Entry links from README and AGENTS.md. |

The service-catalog skill is itself a project maintenance skill. It adds navigation
and workflows around the shared contracts; it does not replace Archaic Java or the
provider's/consumer's local skill.

## Follow one fact across projects

**Testing.** Archaic Java recommends inline assertions and case records. The catalog
[testing v02 guide](../../../docs/test-v02.md) and API specify registration ownership,
independent cases, success/failure and trail lifetime. Minau's
[maintenance skill](https://github.com/archaic-java/minau/blob/main/skills/maintain-minau/SKILL.md)
owns its module scanning, CLI ordinals, virtual-thread scheduling and numeric trail
retention limits. A consumer project's skill owns its fixtures and test launch commands.
A catalog example can illustrate the API without becoming the definitive runner guide.

**Logging.** Archaic Java recommends the logging approach for new applications. The
catalog [logging v03 guide](../../../docs/logging-v03.md) owns context lifecycle and
configuration semantics. Culpa owns its provider mechanics and default configuration;
the consuming application owns its selected sinks, debug settings and scope boundaries.
The catalog's standard renderer and Configuration defaults are catalog-defined API
behavior, even though they include code: ownership follows the promise, not a blanket
rule that all implementations must live outside the catalog.

**SQLite.** The catalog [SQLite guide](../../../docs/sqlite-v01.md) owns scoped sessions,
transaction and failure expectations. ffm-sqlite owns Linux FFM/native mechanics and
provider-specific fault injection. The [ledger](../../../docs/sqlite-v01-guarantees.md)
explicitly separates portable guarantees, observed provider behavior and undecided
policy. An application owns its schema and chosen reader count.

## Keep links useful and resolve conflicts

Keep the full rule in its owning location. Elsewhere, write enough to identify its
relevance and link to it; concise orientation is useful even when it repeats a name
or fundamental constraint. Avoid parallel specifications that readers must reconcile.
Within the catalog, put method-specific details beside declarations and use guides
for the model spanning types. Conformance cases provide executable evidence for those
promises, with links back to the relevant API or guide.

For a concrete change, read the contract version in the actual checkout used by the
project. Record catalog/provider revisions in verification evidence; a moving `main`
link is navigation, not a compatibility pin. Historical examples may target older APIs.

If documents disagree, identify the fact's owner and compare its declarations, guide
and compliance assertions at that revision. Flag and correct inconsistencies in that
home; do not silently pick whichever document or test permits the desired behavior.
Provider behavior does not override a portable contract, and shared guidance cannot
add methods or guarantees to a versioned API. Clarifying an ambiguity must not conceal
a new incompatible requirement in a published contract.
