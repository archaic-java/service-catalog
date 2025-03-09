import org.jspecify.annotations.NullMarked;

@NullMarked
module work.archaic.contract {
    requires transitive org.jspecify;
    exports work.archaic.contract.log.v01;
    exports work.archaic.contract.test.v01;
    exports work.archaic.contract.log.v02;
}
