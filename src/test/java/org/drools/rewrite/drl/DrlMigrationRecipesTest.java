package org.drools.rewrite.drl;

import org.drools.rewrite.drl.ast.AstDrlMigrationRecipe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class DrlMigrationRecipesTest implements RewriteTest {

    @ParameterizedTest
    @MethodSource("halfConstraintRecipes")
    void halfConstraintFilled(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            Person(name == "Mark" || == "Mario")
                        then
                        end
                        """,
                        """
                        rule R
                        when
                            Person(name == "Mark" || name == "Mario")
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("halfConstraintRecipes")
    void halfConstraintComplex(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            Person(name == "Mark" || == "Mario" || == "John",
                                   age > 30 || < 20)
                        then
                        end
                        """,
                        """
                        rule R
                        when
                            Person(name == "Mark" || name == "Mario" || name == "John",
                                   age > 30 || age < 20)
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("halfConstraintRecipes")
    void halfConstraintNotTouchedWhenExplicit(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            Person(name == "Mark" || name == "Mario")
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("customOperatorRecipes")
    void prefixesCustomOperator(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            Person(addresses supersetOf $alice.addresses)
                        then
                        end
                        """,
                        """
                        rule R
                        when
                            Person(addresses ##supersetOf $alice.addresses)
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("customOperatorRecipes")
    void prefixesNegativeCustomOperator(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            Person(addresses not supersetOf $alice.addresses)
                        then
                        end
                        """,
                        """
                        rule R
                        when
                            Person(addresses not ##supersetOf $alice.addresses)
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("customOperatorRecipes")
    void prefixesCustomOperatorComplex(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            Person(addresses supersetOf $alice.addresses,
                                   orders not subsetOf $bob.orders)
                        then
                        end
                        """,
                        """
                        rule R
                        when
                            Person(addresses ##supersetOf $alice.addresses,
                                   orders not ##subsetOf $bob.orders)
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("customOperatorRecipes")
    void leavesBuiltInOperatorUntouched(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            Person(addresses contains $alice.addresses)
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("logicalRecipes")
    void replacesLhsLogicalAndOr(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            (Person() && Pet())
                        then
                        end
                        """,
                        """
                        rule R
                        when
                            (Person() and Pet())
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("logicalRecipes")
    void replacesLhsLogicalAndOrComplex(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            (Person() && Pet())
                            || (Car() && House())
                        then
                        end
                        """,
                        """
                        rule R
                        when
                            (Person() and Pet())
                            or (Car() and House())
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("logicalRecipes")
    void doesNotRewriteConstraintLogicalOperators(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            Person(name == "Mark" || name == "Mario")
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("logicalRecipes")
    void doesNotRewriteConstraintLogicalOperatorsComplex(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        when
                            Person( (name == "Mark" && age > 30) || (name == "Mark" && age < 20) )
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("migrationRecipes")
    void compositeRecipeAppliesAll(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        agenda-group "legacy"
                        when
                            Person(name == "Mark" || == "Mario") || Person(addresses supersetOf $alice.addresses)
                        then
                        end
                        """,
                        """
                        rule R
                        ruleflow-group "legacy"
                        when
                            Person(name == "Mark" || name == "Mario") or Person(addresses ##supersetOf $alice.addresses)
                        then
                        end
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("regexRecipes")
    void rewriteAgendaGroupAttribute(Recipe toApply) {
        rewriteRun(
                spec -> spec.recipe(toApply),
                text(
                        """
                        rule R
                        agenda-group "legacy"
                        when
                        then
                        end
                        """,
                        """
                        rule R
                        ruleflow-group "legacy"
                        when
                        then
                        end
                        """
                )
        );
    }

    static java.util.stream.Stream<Recipe> migrationRecipes() {
        return java.util.stream.Stream.of(
                new DrlMigrationRecipe(),
                new AstDrlMigrationRecipe()
        );
    }

    static java.util.stream.Stream<Recipe> regexRecipes() {
        return java.util.stream.Stream.of(new DrlMigrationRecipe());
    }

    static java.util.stream.Stream<Recipe> halfConstraintRecipes() {
        return java.util.stream.Stream.of(
                new HalfConstraintRecipe(),
                new org.drools.rewrite.drl.ast.AstHalfConstraintRecipe()
        );
    }

    static java.util.stream.Stream<Recipe> customOperatorRecipes() {
        return java.util.stream.Stream.of(
                new PrefixCustomOperatorRecipe(),
                new org.drools.rewrite.drl.ast.AstPrefixCustomOperatorRecipe()
        );
    }

    static java.util.stream.Stream<Recipe> logicalRecipes() {
        return java.util.stream.Stream.of(
                new LhsLogicalOperatorRecipe(),
                new org.drools.rewrite.drl.ast.AstLhsLogicalOperatorRecipe()
        );
    }
}
