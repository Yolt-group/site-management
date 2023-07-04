package nl.ing.lovebird.sitemanagement.forms;

import nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * See {@link FormValidator} for an explanation.
 */
public class ChoiceXORTreeNode extends FormExpressionTreeNode {

    public ChoiceXORTreeNode(boolean isOptional, List<FormExpressionTreeNode> children, String id) {
        super(isOptional, children, id);
    }

    @Override
    public EvaluationResult eval(FilledInUserSiteFormValues filledInUserSiteFormValues) {
        List<FormExpressionTreeNode> children = this.getChildren();

        Map<EvaluationResult, List<EvaluatedNode>> evaluatedNodes = children.stream()
                .map(it -> new EvaluatedNode(it, it.evaluate(filledInUserSiteFormValues)))
                .collect(Collectors.groupingBy(EvaluatedNode::getEvaluationResult));

        List<EvaluatedNode> chosenNodes = evaluatedNodes.getOrDefault(EvaluationResult.CHOSEN, Collections.emptyList());
        List<EvaluatedNode> maybeChosenNodes = evaluatedNodes.getOrDefault(EvaluationResult.MAYBE, Collections.emptyList());
        List<EvaluatedNode> invalidNodes = evaluatedNodes.getOrDefault(EvaluationResult.INVALID, Collections.emptyList());

        if (!invalidNodes.isEmpty()) {
            // propagate as 'invalid'.
            return EvaluationResult.INVALID;
        }
        if (chosenNodes.size() == 1) {
            // propagate as 'chosen'.
            return EvaluationResult.CHOSEN;
        }
        // If there are more 'chosen'. It's invalid.
        if (!chosenNodes.isEmpty()) {
            return EvaluationResult.INVALID;
        }
        // From here nothing is explicitely 'chosen'.
        if (!maybeChosenNodes.isEmpty()) {
            return EvaluationResult.MAYBE;
        }
        return EvaluationResult.NOT_CHOSEN;
    }
}
