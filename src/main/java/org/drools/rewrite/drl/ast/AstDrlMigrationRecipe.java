package org.drools.rewrite.drl.ast;

import org.openrewrite.Recipe;

import java.time.Duration;
import java.util.List;

/**
 * Composite recipe applying token/AST-based DRL10 migrations.
 */
public class AstDrlMigrationRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "AST: Migrate DRL syntax to DRL 10";
    }

    @Override
    public String getDescription() {
        return "Token-based DRL 10 migrations (agenda-group, half-constraints, custom operators, logical operators) on LHS.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new AstAgendaGroupRecipe(),
                new AstHalfConstraintRecipe(),
                new AstLhsLogicalOperatorRecipe(),
                new AstPrefixCustomOperatorRecipe()
        );
    }
}
