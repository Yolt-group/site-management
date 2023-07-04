package nl.ing.lovebird.sitemanagement.forms;

import nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MultiAndTreeNode extends FormExpressionTreeNode {

    public MultiAndTreeNode(boolean isOptional, List<FormExpressionTreeNode> children, String id) {
        super(isOptional, children, id);
    }

    @Override
    public EvaluationResult eval(FilledInUserSiteFormValues filledInUserSiteFormValues) {
        List<FormExpressionTreeNode> children = this.getChildren();

        Map<EvaluationResult, List<EvaluatedNode>> evaluatedNodes = children.stream()
                .map(it -> new EvaluatedNode(it, it.evaluate(filledInUserSiteFormValues)))
                .collect(Collectors.groupingBy(EvaluatedNode::getEvaluationResult));

        List<EvaluatedNode> chosenNodes = evaluatedNodes.getOrDefault(EvaluationResult.CHOSEN, Collections.emptyList());
        List<EvaluatedNode> notChosenNodes = evaluatedNodes.getOrDefault(EvaluationResult.NOT_CHOSEN, Collections.emptyList());
        List<EvaluatedNode> invalidNodes = evaluatedNodes.getOrDefault(EvaluationResult.INVALID, Collections.emptyList());
        if (!invalidNodes.isEmpty()) {
            // propagate as 'invalid'.
            return EvaluationResult.INVALID;
        }


        // A CHOSEN and NOT_CHOSEN cannot co-exist.
        if (!chosenNodes.isEmpty() && !notChosenNodes.isEmpty()) {
            return EvaluationResult.INVALID;
        }
        // If all children are NOT_CHOSEN, this is NOT_CHOSEN.
        if (!chosenNodes.isEmpty()) {
            return EvaluationResult.CHOSEN;
        }

        if (!notChosenNodes.isEmpty()) {
            return EvaluationResult.NOT_CHOSEN;
        }
        // otherwise, it's MAYBE (there are no CHOSEN, NOT_CHOSEN or INVALID members)
        return EvaluationResult.MAYBE;
    }
}
