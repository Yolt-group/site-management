package nl.ing.lovebird.sitemanagement.sites;

import lombok.Getter;

public enum DynamicFieldName {

    CREDITOR_AGENT_BIC("creditorAgentBic"),
    CREDITOR_AGENT_NAME("creditorAgentName"),
    REMITTANCE_INFORMATION_STRUCTURED("remittanceInformationStructured"),
    CREDITOR_POSTAL_ADDRESS_LINE("creditorPostalAddressLine"),
    CREDITOR_POSTAL_COUNTRY("creditorPostalCountry"),
    DEBTOR_NAME("debtorName");

    @Getter
    private final String value;

    DynamicFieldName(String value) {
        this.value = value;
    }
}

