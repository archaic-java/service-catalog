# Archaic Dependency Injection

To "work archaic" means to avoid third party code. But it does not mean to never use libraries.

This repo holds a "service catalog". Several services are described in it. Service providers can
decide to implement their interface. Javas service loader will then be able to identify and
instantiate the service provided and inject it into modules requesting the service.

This provides us with a decoupling between library and its users. This means it starts to be easy to
swap a library as the app using it, is only depending on the service defined in the service catalog.

## Characteristics
- An application can depend on service of more than one service catalog
- Tests are part of the service definition in the catalog
- The tests in the catalog serve as proof for conformance of the providers implementation
- Service definitions in the catalog are versioned and each version is treated immutable

## Goal-scoped diagnostics

[Logging v02](docs/logging-v02.md) defines application goals, per-attempt trails and a log for
immediate information and completed failure reports. It uses Diagnostics, run-only Goal,
Log.write, Goal.newExecutor and FailureReport.goalName.
[Logging v01](docs/logging-v01.md) remains available unchanged for existing consumers. Runtime implementations belong in a
separate provider library.

Compile the catalog and its contract checks with JDK 25, then run with assertions enabled:

```sh
javac @cmd/compile
java @cmd/test
```


## Explicit test cases

[Testing v02](docs/test-v02.md) defines suites that register case instances through a mutable
collection, record-based data-driven cases, and per-execution failure trails. The existing
annotation-based testing v01 contracts remain unchanged.
