package dev.malloy.wayfinder;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Enforces the layering ADR-0001 commits to.
 *
 * This class exists because of a specific mistake. A comment in DestinationTest previously
 * claimed that a framework leaking into the domain would stop the test suite compiling.
 * That claim was tested by adding a Spring import to the domain package: it compiled
 * without complaint. The claim was false, and nothing in the build would have caught the
 * leak.
 *
 * A project whose thesis is that assertions must be falsifiable cannot carry an
 * unfalsifiable assertion about its own architecture. These rules are the falsification
 * mechanism.
 *
 * Carries T-ARCH-001 through T-ARCH-005.
 */
class ArchitectureTest {

    private static final String ROOT = "dev.malloy.wayfinder";
    private static final String DOMAIN = ROOT + ".domain..";
    private static final String PERSISTENCE = ROOT + ".persistence..";
    private static final String WEB = ROOT + ".web..";

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
    }

    @Test
    @DisplayName("T-ARCH-001: the domain depends on no framework")
    void domainDependsOnNoFramework() {
        ArchRule rule = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta..",
                "javax..",
                "org.hibernate..",
                "tools.jackson..",
                "com.fasterxml.jackson..",
                "org.flywaydb..",
                "org.testcontainers..",
                "java.sql..",
                "javax.sql..")
            .because("ADR-0001 keeps the domain independent of Spring, JPA and the database "
                + "so the ranking invariants in US-03 can be tested without starting either");

        rule.check(productionClasses);
    }

    @Test
    @DisplayName("T-ARCH-002: the dependency arrow points inward, never out of the domain")
    void domainDependsOnNoOuterLayer() {
        ArchRule rule = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE, WEB)
            .because("the domain declares the ports it needs and the outer layers supply them; "
                + "a dependency in the other direction would make the domain untestable in isolation");

        rule.check(productionClasses);
    }

    @Test
    @DisplayName("T-ARCH-003: the web layer reaches persistence only through a domain port")
    void webDoesNotDependOnPersistence() {
        ArchRule rule = noClasses()
            .that().resideInAPackage(WEB)
            .should().dependOnClassesThat().resideInAPackage(PERSISTENCE)
            .because("controllers depend on the DestinationCatalog port, so the storage "
                + "implementation can change without touching the HTTP contract");

        rule.check(productionClasses);
    }

    @Test
    @DisplayName("T-ARCH-004: storage types do not escape the persistence package")
    void storageTypesStayInPersistence() {
        ArchRule rule = noClasses()
            .that().resideOutsideOfPackage(PERSISTENCE)
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Entity")
            .because("a JPA entity leaking into the web or domain layer is how a storage "
                + "concern quietly becomes part of a published contract")
            .allowEmptyShould(true);

        rule.check(productionClasses);
    }

    @Test
    @DisplayName("T-ARCH-005: no field injection anywhere")
    void noFieldInjection() {
        ArchRule rule = fields()
            .should().notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .because("constructor injection makes dependencies explicit and keeps classes "
                + "constructible in a plain unit test")
            .allowEmptyShould(true);

        rule.check(productionClasses);
    }
}
