package com.kb.wms;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

@AnalyzeClasses(packages = "com.kb.wms", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnFrameworkOrAdapter = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "..adapter..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapter = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule noJpaAssociations = noFields()
            .should().beAnnotatedWith("jakarta.persistence.ManyToOne")
            .orShould().beAnnotatedWith("jakarta.persistence.OneToMany")
            .orShould().beAnnotatedWith("jakarta.persistence.OneToOne")
            .orShould().beAnnotatedWith("jakarta.persistence.ManyToMany");

    private static final List<String> DOMAINS = List.of(
            "auth", "dashboard", "inbound", "inventory", "outbound", "product", "statushistory", "storeorder", "store",
            "warehouse");

    /** 다른 도메인에서는 {@code application.port.in}만 쓸 수 있다. 같은 도메인 안과 common은 제한하지 않는다. */
    @ArchTest
    static final ArchRule otherDomainsOnlyThroughPortIn = classes()
            .that(DescribedPredicate.describe("reside in a domain package",
                    (JavaClass c) -> domainOf(c.getPackageName()) != null))
            .should(new ArchCondition<JavaClass>("depend on other domains only through application.port.in") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    String from = domainOf(javaClass.getPackageName());
                    for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                        JavaClass target = dependency.getTargetClass();
                        String to = domainOf(target.getPackageName());
                        if (to != null && !to.equals(from)
                                && !target.getPackageName().contains(".application.port.in")) {
                            events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                        }
                    }
                }
            });

    private static String domainOf(String packageName) {
        String prefix = "com.kb.wms.";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String rest = packageName.substring(prefix.length());
        String name = rest.contains(".") ? rest.substring(0, rest.indexOf('.')) : rest;
        return DOMAINS.contains(name) ? name : null;
    }
}
