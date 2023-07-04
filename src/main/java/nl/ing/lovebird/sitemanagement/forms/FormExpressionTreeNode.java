package nl.ing.lovebird.sitemanagement.forms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues;

import java.util.List;


@Slf4j
@Getter
@AllArgsConstructor
public abstract class FormExpressionTreeNode {

    private boolean isOptional;

    private List<FormExpressionTreeNode> children;

    private String id;

    protected abstract EvaluationResult eval(FilledInUserSiteFormValues filledInUserSiteFormValues);

    public EvaluationResult evaluate(FilledInUserSiteFormValues filledInUserSiteFormValues) {
        EvaluationResult result = this.eval(filledInUserSiteFormValues);
        log.trace("Node {} evaluated to {}", id, result);
        return result;
    }

    enum EvaluationResult {
        CHOSEN,
        NOT_CHOSEN,
        MAYBE,
        INVALID
    }

}
