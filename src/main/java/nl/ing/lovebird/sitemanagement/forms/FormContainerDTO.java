package nl.ing.lovebird.sitemanagement.forms;

import java.util.List;

public interface FormContainerDTO extends FormComponentDTO {

    ContainerType getContainerType();

    List<FormComponentDTO> getChildComponents();

    void addChildComponent(FormComponentDTO formContainerDTO);

    enum ContainerType {
        CHOICE, MULTI, FORM, SECTION
    }

}
