# SQLite v01

The [guarantee and failure-test ledger](sqlite-v01-guarantees.md) maps the published behavior to planned conformance and provider tests, and records unresolved policy questions.

`work.archaic.service.sqlite.v01` is a deliberately small contract for a local SQLite database. Consumers declare `uses work.archaic.service.sqlite.v01.Sqlite` and resolve exactly one provider with `ServiceLoader`. The contract does not expose JDBC.

- `open(file, readers, wait)` creates the database if needed, opens one writer and a fixed number of readers, and enables foreign keys on each connection. `wait` bounds acquisition of a connection, not execution time.
- `read(work)` gives one request exclusive use of a read-only connection for the scope. `write(work)` exclusively leases the writer, starts an immediate transaction, and commits on normal return. A failure rolls the transaction back and escapes to the caller; the implementation does not retry application work.
- A work callback may throw its own checked exception; the scope propagates that same failure after cleanup.
- The session and any statement it prepares are valid only until the scope returns. The provider closes outstanding statements when a scope ends. Do not leak a session or statement into another thread or return a lazily evaluated result that uses one.
- `Session.cancel()` is the only session operation allowed from another thread. It asks SQLite to interrupt running SQL; it may have no effect if the SQL has already finished. The requesting thread must arrange that the scope remains live until cancellation returns. An interrupted write may cause SQLite to roll back its transaction.
- `step()` advances once, returning `true` for a row and `false` at completion. Read columns only while positioned on a row. Parameter indexes start at 1, column indexes at 0. Native text and blob data are copied into Java values.
- SQLite values support NULL, INTEGER, REAL, TEXT, and BLOB. SQL determines column affinity and conversions; callers check `isNull()` where a nullable numeric value is possible. No temporal mapping or automatic SQL retry is prescribed by v01.
- A database must be closed after its scopes finish. Closing during an operation fails rather than invalidating native pointers.
- `backup(destination, limit)` copies through SQLite's online backup API into a temporary file next to the destination, then publishes a completed image with an atomic hard link. It refuses to replace an existing file and requires a local filesystem supporting hard links. The limit bounds the copy operation; it does not promise to stop an individual native call in progress.
- `checkpoint()` attempts a passive checkpoint and reports log and checkpointed frame counts. It can return incomplete while a reader retains an older snapshot; it never waits for readers to finish.

The initial provider uses WAL and a short SQLite busy timeout for contention outside its Java writer lease. WAL still permits only one writer at a time; long read transactions can delay checkpoints. [SQLite WAL documentation](https://sqlite.org/wal.html).
