package org.drools.rewrite.drl;

import org.drools.rewrite.drl.ast.AstDrlMigrationRecipe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class RealisticBatchMigrationTest implements RewriteTest {

    @ParameterizedTest
    @MethodSource("migrationRecipes")
    void migratesFiveRulesWithPatternsAndRhsUntouched(Recipe recipe) {
        rewriteRun(
                spec -> spec.recipe(recipe)
                        .expectedCyclesThatMakeChanges(1),
                text(
                        // before
                        """
                        rule R1
                        agenda-group "customers"
                        when
                            Person(name == "Mark" || == "Mario")
                            Address(city == "Boston")
                        then
                            // RHS should not change
                            System.out.println(name == "Mark" || == "Mario");
                        end

                        rule R2
                        agenda-group "orders"
                        when
                            Person(addresses supersetOf $alice.addresses)
                            Order(total > 100 && status == "NEW")
                        then
                            // RHS should not change
                            if (order.supersetOf($alice.addresses) && status == "NEW") { doSomething(); }
                        end

                        rule R3
                        when
                            (Person() && Pet())
                            Car(model == "Civic" || == "Accord")
                        then
                            // RHS should not change
                            boolean ok = (a && b) || (c && d);
                        end

                        rule R4
                        when
                            Person(name == "Bob" || name == "Alice")
                            Account(balance > 1000 || == 500)
                        then
                            // RHS should not change
                            if (balance > 1000 || balance == 500) { reward(); }
                        end

                        rule R5
                        agenda-group "vip"
                        when
                            Order(items subsetOf $catalog.items || == $vip.items)
                            Customer(score > 80 || < 60)
                        then
                            // RHS should not change
                            log.info("subsetOf? {}", items subsetOf $catalog.items || == $vip.items);
                        end
                        """,
                        // after
                        """
                        rule R1
                        ruleflow-group "customers"
                        when
                            Person(name == "Mark" || name == "Mario")
                            Address(city == "Boston")
                        then
                            // RHS should not change
                            System.out.println(name == "Mark" || == "Mario");
                        end

                        rule R2
                        ruleflow-group "orders"
                        when
                            Person(addresses ##supersetOf $alice.addresses)
                            Order(total > 100 && status == "NEW")
                        then
                            // RHS should not change
                            if (order.supersetOf($alice.addresses) && status == "NEW") { doSomething(); }
                        end

                        rule R3
                        when
                            (Person() and Pet())
                            Car(model == "Civic" || model == "Accord")
                        then
                            // RHS should not change
                            boolean ok = (a && b) || (c && d);
                        end

                        rule R4
                        when
                            Person(name == "Bob" || name == "Alice")
                            Account(balance > 1000 || balance == 500)
                        then
                            // RHS should not change
                            if (balance > 1000 || balance == 500) { reward(); }
                        end

                        rule R5
                        ruleflow-group "vip"
                        when
                            Order(items ##subsetOf $catalog.items || items == $vip.items)
                            Customer(score > 80 || score < 60)
                        then
                            // RHS should not change
                            log.info("subsetOf? {}", items subsetOf $catalog.items || == $vip.items);
                        end
                        """
                )
        );
    }

    static java.util.stream.Stream<Recipe> migrationRecipes() {
        return java.util.stream.Stream.of(new AstDrlMigrationRecipe());
    }
}
