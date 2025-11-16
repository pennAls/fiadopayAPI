package edu.ucsal.fiadopay.usecases;

import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.repo.MerchantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ValidateMerchantAuthUseCase {
    private final MerchantRepository merchants;

    public ValidateMerchantAuthUseCase(MerchantRepository merchants) {
        this.merchants = merchants;
    }

    public Merchant execute (String auth) {
        if (auth == null || !auth.startsWith("Bearer FAKE-")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var raw = auth.substring("Bearer FAKE-".length());
        long id;
        try {
            id = Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var merchant = merchants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (merchant.getStatus() != Merchant.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return merchant;
    }

}
