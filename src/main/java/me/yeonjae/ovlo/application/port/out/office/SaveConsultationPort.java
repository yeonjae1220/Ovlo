package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Consultation;

public interface SaveConsultationPort {
    Consultation save(Consultation consultation);
}
