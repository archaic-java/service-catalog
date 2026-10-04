# Evolve a service contract

Read this before adding a capability, changing a public expectation or tightening a
provider-compliance assertion. Use [ownership](ownership.md) to place each part of a
cross-project change and [conformance](conformance.md) to choose verification.

## Establish the existing promise

Read the exported package, its Javadoc, the capability guide and available compliance
cases at the consumer/provider's selected catalog revision. State what consumers may
assume and providers must supply. Distinguish that promise from observed behavior of
one provider and from open decisions in a test ledger.

Use one catalog per namespace; keep `work.archaic` capabilities in this catalog.
Applications may combine catalogs from different namespaces. Add a service abstraction
when implementation independence is useful; keep ordinary intrinsic code in its project.
A catalog can also contain the data types and small shared mechanics needed to express
its contracts, as the current logging Configuration, Context and TextOutput demonstrate.

## Choose the version and surface

Preserve published versioned signatures and behavior. Introduce a new versioned package
for incompatible API or semantic changes and retain the old one for existing consumers.
Treat additions of abstract methods, changed failure behavior, stricter lifetime rules
and newly required concurrency behavior as compatibility questions, not documentation
cleanup. Obtain an explicit compatibility decision before modifying a published version;
an unpublished-version exception must be established rather than assumed.

For a new capability, define its data types, public operations, ownership/lifetime,
concurrency, failure identity and cleanup, and provider selection policy. State what
remains unspecified. Avoid implying a durability, retry or ordering guarantee without
a defined promise. Use ServiceLoader where the capability requires replaceable service
selection; preserve explicit construction where that is its documented composition.

Keep provider-specific tuning, platform machinery and deployment outside the contract.
Update module exports with the API. Update the skill's capability index if adding or
retiring an exported package. Do not advertise a source-only package as public merely
because its directory exists.

## Verify and document the change

Add data/default-method checks for catalog-owned mechanics. Add reusable compliance
cases when they can exercise portable observable behavior; providers supply fixtures
and execute them. Record unsupported or undecided scenarios explicitly rather than
presenting proposed cases as implemented coverage.

For a changed promise, identify provider and consumer follow-up work and the revisions
used to verify compatibility. Keep contract meaning and reusable expectations here;
put migration wiring and provider regression machinery in those projects. Run catalog
checks using the README commands; run provider checks separately when the claim needs
an actual implementation. Do not claim provider compliance from compiling interfaces.
