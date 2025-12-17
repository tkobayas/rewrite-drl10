package org.drools.rewrite.drl;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class DrlMigrationRecipesTest implements RewriteTest {

    @Test
    void halfConstraintFilled() {
        rewriteRun(
                spec -> spec.recipe(new HalfConstraintRecipe()),
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

    @Test
    void halfConstraintComplex() {
        rewriteRun(
                spec -> spec.recipe(new HalfConstraintRecipe()),
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

    @Test
    void halfConstraintNotTouchedWhenExplicit() {
        rewriteRun(
                spec -> spec.recipe(new HalfConstraintRecipe()),
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

    @Test
    void prefixesCustomOperator() {
        rewriteRun(
                spec -> spec.recipe(new PrefixCustomOperatorRecipe()),
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

    @Test
    void prefixesCustomOperatorComplex() {
        rewriteRun(
                spec -> spec.recipe(new PrefixCustomOperatorRecipe()),
                text(
                        """
                        rule R
                        when
                            Person(addresses supersetOf $alice.addresses,
                                   orders subsetOf $bob.orders)
                        then
                        end
                        """,
                        """
                        rule R
                        when
                            Person(addresses ##supersetOf $alice.addresses,
                                   orders ##subsetOf $bob.orders)
                        then
                        end
                        """
                )
        );
    }

    @Test
    void leavesBuiltInOperatorUntouched() {
        rewriteRun(
                spec -> spec.recipe(new PrefixCustomOperatorRecipe()),
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

    @Test
    void replacesLhsLogicalAndOr() {
        rewriteRun(
                spec -> spec.recipe(new LhsLogicalOperatorRecipe()),
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

    @Test
    void replacesLhsLogicalAndOrComplex() {
        rewriteRun(
                spec -> spec.recipe(new LhsLogicalOperatorRecipe()),
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

    @Test
    void doesNotRewriteConstraintLogicalOperators() {
        rewriteRun(
                spec -> spec.recipe(new LhsLogicalOperatorRecipe()),
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

    @Test
    void doesNotRewriteConstraintLogicalOperatorsComplex() {
        rewriteRun(
                spec -> spec.recipe(new LhsLogicalOperatorRecipe()),
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

    @Test
    void compositeRecipeAppliesAll() {
        rewriteRun(
                spec -> spec.recipe(new DrlMigrationRecipe()),
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

    @Test
    void rewriteAgendaGroupAttribute() {
        rewriteRun(
                spec -> spec.recipe(new AgendaGroupToRuleflowGroupRecipe()),
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
}
