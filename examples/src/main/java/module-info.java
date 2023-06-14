module com.edgedb.examples {
    requires com.edgedb.driver;
    requires org.slf4j;
    requires org.jooq.joou;
    requires org.reflections;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;

    exports com.edgedb.examples;
}