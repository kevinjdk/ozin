package com.ozin.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifica las reglas de dependencia de la arquitectura hexagonal (ADR-0001, ADR-0002).
 * Es una segunda línea de defensa además de la separación en módulos Maven.
 */
@AnalyzeClasses(packages = "com.ozin", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "com.ozin.domain..";
    private static final String APPLICATION = "com.ozin.application..";
    private static final String INFRASTRUCTURE = "com.ozin.infrastructure..";

    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_must_be_framework_free = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..", "tools.jackson..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_must_be_framework_free = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule rest_adapters_only_use_input_ports = noClasses()
            .that().resideInAPackage("..adapter.in.rest..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..", "..port.out..", "..application..service..");

    @ArchTest
    static final ArchRule jpa_stays_inside_persistence_adapters = noClasses()
            .that().resideOutsideOfPackages("..adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data.jpa..");
}
