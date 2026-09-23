# SQLite v01

`work.archaic.service.sqlite.v01` is a deliberately small contract for a local SQLite database. Consumers declare `uses work.archaic.service.sqlite.v01.Sqlite` and resolve exactly one provider with `ServiceLoader`. The contract does not expose JDBC.

- `open(file, readers, wait)` creates the database if needed, opens one writer and a fixed number of readers, and enables foreign keys on each connection. `wait` bounds acquisition of a connection, not execution time.
- `read(work)` gives one request exclusive use of a read-only connection for the scope. `write(work)` exclusively leases the writer, starts an immediate transaction, and commits on normal return. A failure rolls the transaction back and escapes to the caller; the implementation does not retry application work.
- The session and any statement it prepares are valid only until the scope returns. The provider closes outstanding statements when a scope ends. Do not leak a session or statement into another thread or return a lazily evaluated result that uses one.
- `step()` advances once, returning `true` for a row and `false` at completion. Read columns only while positioned on a row. Parameter indexes start at 1, column indexes at 0. Native text and blob data are copied into Java values.
- SQLite values support NULL, INTEGER, REAL, TEXT, and BLOB. SQL determines column affinity and conversions; callers check `isNull()` where a nullable numeric value is possible. No temporal mapping or automatic SQL retry is prescribed by v01.
- A database must be closed after its scopes finish. Closing during an operation fails rather than invalidating native pointers.

The initial provider uses WAL and a short SQLite busy timeout for contention outside its Java writer lease. WAL still permits only one writer at a time; long read transactions can delay checkpoints. [SQLite WAL documentation](https://sqlite.org/wal.html).
