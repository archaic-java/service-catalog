# Archaic Service Catalog

Use JDK 25 and named JPMS modules with the checked-in `cmd/` argument files. Do not
introduce Maven, Gradle, class-path fallbacks or dependencies on provider implementations.
The catalog and its checks live in `src/`; generated classes belong in ignored `out/`.

Read [README.md](README.md), then the foundation and relevant task/contract links in
[the catalog skill](skills/maintain-service-catalog/SKILL.md). Follow it directly when
local skills are not automatically discovered. Use Archaic Java for shared conventions.

Preserve published contracts, keep portable expectations distinct from provider
mechanics, and distinguish catalog checks from provider compliance evidence.
Follow the skill's ownership reference when choosing where to document a fact;
update the owning source or guide rather than adding a parallel specification here.
