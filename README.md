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

