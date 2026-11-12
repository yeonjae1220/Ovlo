package me.yeonjae.ovlo.application.port.in.marketplace;

public interface CleanupMarketplacePhotosUseCase {
    record Result(int claimed, int removed, int failed) {}
    Result cleanup();
}
