package nl.ing.lovebird.sitemanagement.forms;

public interface FormFieldDTO extends FormComponentDTO {

    FieldType getFieldType();

    enum FieldType {
        TEXT, PASSWORD, SELECT, EXPLANATION, DATE, NUMBER, IBAN, RADIO, TEXT_AREA, IMAGE, FLICKER_CODE
    }

}
