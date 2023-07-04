package nl.ing.lovebird.sitemanagement.exception;

import lombok.Value;
import nl.ing.lovebird.errorhandling.ErrorInfo;

import java.util.function.Supplier;

@Value
public class MessageSupplierErrorDTO implements ErrorInfo {

    ErrorConstants errorConstant;
    Supplier<String> messageSupplier;

    @Override
    public String getCode() {
        return errorConstant.getCode();
    }

    @Override
    public String getMessage() {
        return messageSupplier.get();
    }
}
