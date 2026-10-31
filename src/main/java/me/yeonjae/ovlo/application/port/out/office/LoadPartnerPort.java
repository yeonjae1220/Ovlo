package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Partner;

import java.util.List;
import java.util.Optional;

public interface LoadPartnerPort {
    Optional<Partner> findById(Long partnerId);
    List<Partner> findAll();
    List<Partner> findEnabled();
}
