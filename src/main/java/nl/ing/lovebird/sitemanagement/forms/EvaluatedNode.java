package nl.ing.lovebird.sitemanagement.forms;

import lombok.Value;

/**
 * See {@link FormValidator} for an explanation.
 */
@Value
public class EvaluatedNode  {

    FormExpressionTreeNode formExpressionTreeNode;
    FormExpressionTreeNode.EvaluationResult evaluationResult;
}
