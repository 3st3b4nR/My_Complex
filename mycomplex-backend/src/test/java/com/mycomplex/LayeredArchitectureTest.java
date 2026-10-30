package com.mycomplex;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

class LayeredArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.mycomplex");

    @Test
    void domainDoesNotDependOnFrameworkOrAdapters() {
        noClasses().that().resideInAPackage("..domain..")
                .and().doNotHaveSimpleName("package-info")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "..infrastructure.."
                ).check(classes);
    }

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .check(classes);
    }

    @Test
    void restAdaptersDoNotDependOnPersistenceRepositories() {
        noClasses().that().resideInAPackage("..adapter.in.rest..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter.out.persistence.repository..")
                .check(classes);
    }

    @Test
    void propertiesDoesNotDependOnUsers() {
        noClasses().that().resideInAPackage("..properties..")
                .should().dependOnClassesThat()
                .resideInAPackage("..users..")
                .check(classes);
    }

    @Test
    void usersDoesNotUsePropertiesInfrastructure() {
        noClasses().that().resideInAPackage("..users..")
                .should().dependOnClassesThat()
                .resideInAPackage("..properties.infrastructure..")
                .check(classes);
    }
}
