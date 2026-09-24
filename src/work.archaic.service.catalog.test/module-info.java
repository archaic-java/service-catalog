module work.archaic.service.catalog.test {
    requires work.archaic.service.catalog;
    requires jdk.httpserver;
    uses work.archaic.service.sqlite.v01.Sqlite;
    exports work.archaic.service.catalog.test;
}
