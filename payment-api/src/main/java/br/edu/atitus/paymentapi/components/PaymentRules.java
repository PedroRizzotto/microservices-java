package br.edu.atitus.paymentapi.components;

import br.edu.atitus.paymentapi.dtos.PaymentRequest;
import br.edu.atitus.paymentapi.entities.PaymentMethod;
import br.edu.atitus.paymentapi.entities.PaymentReferenceType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class PaymentRules {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static PaymentReferenceType parseReferenceType(String value) {
        if (value != null) {
            for (PaymentReferenceType type : PaymentReferenceType.values()) {
                if (type.name().equalsIgnoreCase(value.trim()))
                    return type;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de referência inválido");
    }

    public static PaymentMethod parseMethod(String value) {
        if (value != null) {
            for (PaymentMethod method : PaymentMethod.values()) {
                if (method.name().equalsIgnoreCase(value.trim()))
                    return method;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pagamento inválido");
    }

    public static void validate(PaymentRequest r) {
        parseReferenceType(r.referenceType());
        parseMethod(r.method());
        if (r.referenceId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referência do pagamento é obrigatória");
        if (r.userId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário do pagamento é obrigatório");
        if (r.amount() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor do pagamento deve ser maior que zero");
    }
}
