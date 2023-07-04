package nl.ing.lovebird.sitemanagement.forms;

import nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues;

import java.util.List;

/**
 * See {@link FormValidator} for an explanation.
 */
public class FieldLeafTreeNode extends FormExpressionTreeNode {
    private final String fieldId;

    FieldLeafTreeNode(boolean isOptional, List<FormExpressionTreeNode> children, String fieldId) {
        super(isOptional, children, fieldId);
        this.fieldId = fieldId;
    }

    @Override
    public EvaluationResult eval(FilledInUserSiteFormValues filledInUserSiteFormValues) {
        if (filledInUserSiteFormValues.getValueMap().containsKey(fieldId)){
            return EvaluationResult.CHOSEN;
        }

        if (this.isOptional()) {
            return EvaluationResult.MAYBE;
        }

        return EvaluationResult.NOT_CHOSEN;
    }
}
