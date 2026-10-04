# Verify contracts and providers

Read this when adding compliance cases, wiring them into a provider suite, or reporting
verification. Read the capability guide and API first: test assertions attest a public
promise; they do not create one. For scope boundaries, read [ownership](ownership.md).

## Distinguish the execution surfaces

| Surface | Current entry point | What a pass establishes |
|---|---|---|
| Standalone catalog checks | [LoggingContractTest](../../../src/work.archaic.service.catalog.test/work/archaic/service/catalog/test/LoggingContractTest.java), via the README commands | Logging v01/v02/v03 data/default-method checks and [TextOutputContractTest](../../../src/work.archaic.service.catalog.test/work/archaic/service/catalog/test/TextOutputContractTest.java). No concrete provider is verified. |
| Logging v03 reusable cases | [LoggingV03ProviderContract](../../../src/work.archaic.service.catalog.test/work/archaic/service/catalog/test/LoggingV03ProviderContract.java) | The supplied provider passes the registered context expectations. The fixture factory supplies independent provider, clock and observed thread-safe sinks per case. |
| SQLite v01 reusable cases | [SqliteCases](../../../src/work.archaic.service.catalog.test/work/archaic/service/catalog/test/SqliteCases.java) | The supplied provider passes the registered database expectations. Case-owned databases keep concurrent registrations independent. |
| SQLite compatibility launcher | [SqliteProviderContract](../../../src/work.archaic.service.catalog.test/work/archaic/service/catalog/test/SqliteProviderContract.java) | Runs the SQLite registrations sequentially against a service-loaded provider with assertions enabled; this is separate from `cmd/test`. |
| Provider integration, fault and platform checks | Provider repository's maintenance guidance | Implementation-specific behavior such as Linux native failures or deployment integration. |

The catalog test module exports reusable case registration APIs and depends on the
catalog, not provider classes or Minau. A provider-side public testing-v02 suite can
register those cases and run them with Minau. Use the test contract for case semantics
and Minau's own documentation for discovery, selection and report details.
The [logging guide](logging-v03.md#implement-and-verify-a-provider)
defines its fixture interface requirements; the
[SQLite ledger](sqlite-v01-guarantees.md) separates implemented scenarios,
planned work and unresolved policy. Read current registration source to know which
cases actually execute; older issue/PR links preserve context, not current coverage.

## Current interpretation limits

The [SQLite lifetime cases](../../../src/work.archaic.service.catalog.test/work/archaic/service/catalog/test/SqliteLifetimes.java)
currently catch `IllegalStateException` for expired handles, while the ledger leaves
that exact exception type unspecified. Treat this as an existing case/specification
mismatch to reconcile, not a portable promise created by the case. Likewise, the
ledger leaves predictable rejection of non-cancel cross-thread session misuse open;
the usage restriction alone does not specify an exception outcome. The SQLite
configuration case also requires WAL, whereas the guide describes WAL as an initial-
provider choice. Resolve that scope before treating its journal-mode assertion as a
universal provider requirement.

The SQLite compatibility launcher selects the first service-loaded provider and
prints notes directly through a lambda. It does not implement testing-v02 trail
retention, confinement or completed-trail validation. Use it for its SQLite checks,
not as an example of a compliant testing-v02 runner or exact-one provider selection.
Use the selected runner's normal suite path when verifying those test semantics.

## Add a portable case

Tie each assertion to a documented guarantee. Use contract interfaces rather than
provider internals and name the case after the expected behavior. Keep inputs immutable;
create mutable resources inside `run`, give concurrent cases independent fixtures and
finish asynchronous work before returning. Use inline assertions with an explanation,
catch expected failures explicitly, and retain diagnostic evidence with TestTrail.

For lifecycle or failure tests, verify the next caller's observable state where the
contract promises reuse. Check original exceptions and cleanup behavior without assuming
an unspecified exception type. Use synchronization and bounded waits rather than sleeps
to infer concurrency. Keep provider-specific faults in its tests unless the scenario can
be expressed through the portable API.

For SQLite, preserve the current case harness's intentional failed-database evidence
retention and trail path notes; clean successful temporary databases. Do not confuse
preserved evidence with resources that may remain actively open. Document comparable
ownership and cleanup when introducing another external-resource harness.

## Report compliance evidence

State which catalog revision, provider revision, platform and cases were executed.
Separate standalone catalog success, reusable-case success and provider-specific checks.
Keep known gaps explicit: not every exported capability currently has reusable cases.
In particular, there is no reusable testing-v02 runner conformance suite here; a new
runner needs its own verification against the test contract. Passing finite tests is
evidence of the assertions exercised, not exhaustive proof.
If a case exceeds the published promise, correct or relocate it instead of declaring
all providers incompatible. If an implementation violates a real promise, retain the
regression and fix the provider in its owning project.
