package me.yeonjae.ovlo.application.service.command;
import me.yeonjae.ovlo.application.port.in.office.EraseExpiredOfficeDataUseCase;
import me.yeonjae.ovlo.application.port.out.office.OfficePersonalDataPort;
import org.springframework.stereotype.Service;
@Service
public class OfficePersonalDataService implements EraseExpiredOfficeDataUseCase {
    private final OfficePersonalDataPort data;
    public OfficePersonalDataService(OfficePersonalDataPort data) { this.data=data; }
    public int eraseExpiredBatch() { return data.eraseExpiredBatch(); }
}
