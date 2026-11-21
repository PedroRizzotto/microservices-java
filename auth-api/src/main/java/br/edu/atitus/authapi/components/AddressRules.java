package br.edu.atitus.authapi.components;

import br.edu.atitus.authapi.dtos.AddressRequest;
import br.edu.atitus.authapi.entities.AddressEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AddressRules {

    private AddressRules() {
    }

    public static AddressRequest normalize(AddressRequest r) {
        return new AddressRequest(
                trim(r.label()),
                r.zipCode() == null ? null : r.zipCode().replaceAll("\\D", ""),
                trim(r.street()),
                trim(r.number()),
                trim(r.complement()),
                trim(r.district()),
                trim(r.city()),
                r.state() == null ? null : r.state().trim().toUpperCase());
    }

    public static void validate(AddressRequest r) {
        if (tooLong(r.label(), 50) || tooLong(r.number(), 20) || tooLong(r.street(), 255)
                || tooLong(r.complement(), 255) || tooLong(r.district(), 255) || tooLong(r.city(), 255))
            throw bad("Campo excede o tamanho máximo");
        if (r.zipCode() == null || !r.zipCode().matches("\\d{8}"))
            throw bad("CEP deve ter 8 dígitos");
        if (isBlank(r.street()))
            throw bad("Rua é obrigatória");
        if (isBlank(r.number()))
            throw bad("Número é obrigatório");
        if (isBlank(r.district()))
            throw bad("Bairro é obrigatório");
        if (isBlank(r.city()))
            throw bad("Cidade é obrigatória");
        if (r.state() == null || !r.state().matches("[A-Z]{2}"))
            throw bad("UF deve ter 2 letras");
    }

    public static void requireOwner(AddressEntity a, Long userId) {
        if (a == null || !userId.equals(a.getUserId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço não encontrado");
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean tooLong(String s, int max) {
        return s != null && s.length() > max;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
