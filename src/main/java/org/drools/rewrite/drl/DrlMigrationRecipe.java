package org.drools.rewrite.drl;

import org.openrewrite.Option;
import org.openrewrite.Recipe;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Composite recipe that applies all DRL 10 migrations currently supported.
 */
public class DrlMigrationRecipe extends Recipe {

    @Option(displayName = "Rewrite half-constraints", description = "Fill in missing left-hand side in chained constraints.", required = false)
    boolean rewriteHalfConstraints = true;

    @Option(displayName = "Prefix custom operators with ##", description = "Ensure custom operator names are prefixed with the DRL10 required ##.", required = false)
    boolean prefixCustomOperators = true;

    @Option(displayName = "Replace &&/|| in LHS", description = "Replace infix logical operators in LHS pattern composition with textual and/or.", required = false)
    boolean replaceLhsLogicalInfix = true;

    @Option(displayName = "Rewrite agenda-group to ruleflow-group", description = "DRL10 uses ruleflow-group; rewrite legacy agenda-group attributes.", required = false)
    boolean rewriteAgendaGroup = true;

    @Override
    public String getDisplayName() {
        return "Migrate DRL syntax to DRL 10";
    }

    @Override
    public String getDescription() {
        return "Normalizes legacy DRL syntax (half-constraints, custom operators, infix logical ops) to DRL 10-compatible forms.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                rewriteHalfConstraints ? new HalfConstraintRecipe() : null,
                prefixCustomOperators ? new PrefixCustomOperatorRecipe() : null,
                replaceLhsLogicalInfix ? new LhsLogicalOperatorRecipe() : null,
                rewriteAgendaGroup ? new AgendaGroupToRuleflowGroupRecipe() : null
        ).stream().filter(r -> r != null).toList();
    }
}
