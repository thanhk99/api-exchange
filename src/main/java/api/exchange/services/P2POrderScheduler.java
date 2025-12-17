package api.exchange.services;

import api.exchange.models.P2POrderDetail;
import api.exchange.models.P2POrderDetail.P2PTransactionStatus;
import api.exchange.models.FundingWallet;
import api.exchange.models.P2PAd;
import api.exchange.repository.P2POrderDetailRepository;
import api.exchange.repository.FundingWalletRepository;
import api.exchange.repository.P2PAdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class P2POrderScheduler {

    @Autowired
    private P2POrderDetailRepository orderRepository;

    @Autowired
    private FundingWalletRepository fundingWalletRepository;

    @Autowired
    private P2PAdRepository p2pAdRepository;

    /**
     * Auto-cancel expired orders every minute
     * Orders that are AWAITING_PAYMENT and past expiresAt will be cancelled
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void autoCancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        // Find all orders that are awaiting payment
        List<P2POrderDetail> allOrders = orderRepository.findAll();

        for (P2POrderDetail order : allOrders) {
            // Check if order is awaiting payment and has expired
            if (order.getStatus() == P2PTransactionStatus.AWAITING_PAYMENT
                    && order.getExpiresAt() != null
                    && now.isAfter(order.getExpiresAt())) {

                // Cancel the order
                order.setStatus(P2PTransactionStatus.CANCELLED);
                orderRepository.save(order);

                // Release locked coins back to seller
                FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(
                        order.getSellerId(),
                        order.getAsset());

                if (sellerWallet != null) {
                    sellerWallet.setLockedBalance(sellerWallet.getLockedBalance().subtract(order.getCryptoAmount()));
                    sellerWallet.setBalance(sellerWallet.getBalance().add(order.getCryptoAmount()));
                    fundingWalletRepository.save(sellerWallet);
                }

                // Return available amount to ad
                P2PAd ad = order.getAd();
                if (ad != null) {
                    ad.setAvailableAmount(ad.getAvailableAmount().add(order.getCryptoAmount()));
                    p2pAdRepository.save(ad);
                }

                System.out.println("Auto-cancelled expired order: " + order.getId());
            }
        }
    }
}
