package br.edu.atitus.agendaapi.servicesimpl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorizationService {
    public void requireCustomer(Integer userType) {
        if (userType == null || userType != 1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão para esta operação");
    }

    public void requireProvider(Integer userType) {
        if (userType == null || userType != 2)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão para esta operação");
    }
}
