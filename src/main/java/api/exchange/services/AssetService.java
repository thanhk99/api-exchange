package api.exchange.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import api.exchange.models.EarnWallet;
import api.exchange.models.FundingWallet;
import api.exchange.models.SpotWallet;
import api.exchange.models.coinModel;
import api.exchange.repository.EarnWalletRepository;
import api.exchange.repository.FundingWalletRepository;
import api.exchange.repository.SpotWalletRepository;
import api.exchange.repository.coinRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import api.exchange.dtos.Request.TransferRequest;
import api.exchange.dtos.Response.FuturesWalletResponse;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetService {

    @Autowired
    private FundingWalletRepository fundingWalletRepository;

    @Autowired
    private SpotWalletRepository spotWalletRepository;

    @Autowired
    private EarnWalletRepository earnWalletRepository;

    @Autowired
    private coinRepository coinRepository;

    @Autowired
    private FuturesWalletService futuresWalletService;

    public Map<String, Object> getAssetOverview(String uid) {
        List<FundingWallet> fundingWallets = fundingWalletRepository.findAllByUid(uid);
        List<SpotWallet> spotWallets = spotWalletRepository.findAllByUid(uid);
        List<EarnWallet> earnWallets = earnWalletRepository.findAllByUid(uid);

        Map<String, Object> fundingData = processFundingWallets(fundingWallets);
        Map<String, Object> spotData = processSpotWallets(spotWallets);
        Map<String, Object> earnData = processEarnWallets(earnWallets);
        Map<String, Object> futuresData = processFuturesWallet(uid);

        BigDecimal totalAssetUsd = BigDecimal.ZERO;
        totalAssetUsd = totalAssetUsd.add((BigDecimal) fundingData.get("totalUsd"));
        totalAssetUsd = totalAssetUsd.add((BigDecimal) spotData.get("totalUsd"));
        totalAssetUsd = totalAssetUsd.add((BigDecimal) earnData.get("totalUsd"));
        totalAssetUsd = totalAssetUsd.add((BigDecimal) futuresData.get("totalUsd"));

        Map<String, Object> response = new HashMap<>();
        response.put("totalAssetUsd", totalAssetUsd);
        response.put("funding", fundingData);
        response.put("spot", spotData);
        response.put("earn", earnData);
        response.put("futures", futuresData);
        return response;
    }

    private Map<String, Object> processFundingWallets(List<FundingWallet> wallets) {
        BigDecimal totalUsd = BigDecimal.ZERO;
        List<Map<String, Object>> assetList = new ArrayList<>();
        for (FundingWallet wallet : wallets) {
            BigDecimal price = getPrice(wallet.getCurrency());
            BigDecimal balance = wallet.getBalance();
            BigDecimal valueUsd = balance.multiply(price);
            totalUsd = totalUsd.add(valueUsd);
            Map<String, Object> asset = new HashMap<>();
            asset.put("currency", wallet.getCurrency());
            asset.put("balance", balance);
            asset.put("locked", wallet.getLockedBalance());
            asset.put("valueUsd", valueUsd);
            assetList.add(asset);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsd", totalUsd);
        result.put("assets", assetList);
        return result;
    }

    private Map<String, Object> processSpotWallets(List<SpotWallet> wallets) {
        BigDecimal totalUsd = BigDecimal.ZERO;
        List<Map<String, Object>> assetList = new ArrayList<>();
        for (SpotWallet wallet : wallets) {
            BigDecimal price = getPrice(wallet.getCurrency());
            BigDecimal balance = wallet.getBalance();
            BigDecimal locked = wallet.getLockedBalance();
            BigDecimal totalBalance = balance.add(locked);
            BigDecimal valueUsd = totalBalance.multiply(price);
            totalUsd = totalUsd.add(valueUsd);
            Map<String, Object> asset = new HashMap<>();
            asset.put("currency", wallet.getCurrency());
            asset.put("balance", balance);
            asset.put("locked", locked);
            asset.put("total", totalBalance);
            asset.put("valueUsd", valueUsd);
            assetList.add(asset);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsd", totalUsd);
        result.put("assets", assetList);
        return result;
    }

    private Map<String, Object> processEarnWallets(List<EarnWallet> wallets) {
        BigDecimal totalUsd = BigDecimal.ZERO;
        List<Map<String, Object>> assetList = new ArrayList<>();
        for (EarnWallet wallet : wallets) {
            BigDecimal price = getPrice(wallet.getCurrency());
            BigDecimal totalBalance = wallet.getTotalBalance();
            BigDecimal valueUsd = totalBalance.multiply(price);
            totalUsd = totalUsd.add(valueUsd);
            Map<String, Object> asset = new HashMap<>();
            asset.put("currency", wallet.getCurrency());
            asset.put("totalBalance", totalBalance);
            asset.put("availableBalance", wallet.getAvailableBalance());
            asset.put("lockedBalance", wallet.getLockedBalance());
            asset.put("valueUsd", valueUsd);
            assetList.add(asset);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsd", totalUsd);
        result.put("assets", assetList);
        return result;
    }

    private Map<String, Object> processFuturesWallet(String uid) {
        try {
            FuturesWalletResponse futuresWallet = futuresWalletService.getWalletInfo(uid, "USDT");

            // Calculate total value including unrealized PnL
            BigDecimal totalValue = futuresWallet.getBalance().add(futuresWallet.getUnrealizedPnl());
            BigDecimal totalUsd = totalValue; // USDT = 1 USD

            Map<String, Object> asset = new HashMap<>();
            asset.put("currency", futuresWallet.getCurrency());
            asset.put("balance", futuresWallet.getBalance());
            asset.put("lockedBalance", futuresWallet.getLockedBalance());
            asset.put("availableBalance", futuresWallet.getAvailableBalance());
            asset.put("unrealizedPnl", futuresWallet.getUnrealizedPnl());
            asset.put("totalPositionValue", futuresWallet.getTotalPositionValue());
            asset.put("marginRatio", futuresWallet.getMarginRatio());
            asset.put("openPositionsCount", futuresWallet.getOpenPositionsCount());
            asset.put("totalValue", totalValue);
            asset.put("valueUsd", totalUsd);

            Map<String, Object> result = new HashMap<>();
            result.put("totalUsd", totalUsd);
            result.put("asset", asset);
            return result;
        } catch (Exception e) {
            // Return empty data if futures wallet doesn't exist or error occurs
            Map<String, Object> result = new HashMap<>();
            result.put("totalUsd", BigDecimal.ZERO);
            result.put("asset", null);
            return result;
        }
    }

    private BigDecimal getPrice(String currency) {
        if ("USDT".equalsIgnoreCase(currency) || "USD".equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }
        Optional<coinModel> coinOpt = coinRepository.findById(currency);
        return coinOpt.map(coinModel::getCurrentPrice).orElse(BigDecimal.ZERO);
    }

    @Transactional
    public void transferAsset(String uid, TransferRequest request) {
        String from = request.getFromWallet().toUpperCase();
        String to = request.getToWallet().toUpperCase();
        String asset = request.getAsset().toUpperCase();
        BigDecimal amount = request.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        if (from.equals(to)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet type");
        }

        if ("SPOT".equals(from) && "FUNDING".equals(to)) {
            // SPOT -> FUNDING
            SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, asset);
            if (spotWallet == null || spotWallet.getBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("Insufficient balance in SPOT wallet");
            }

            FundingWallet fundingWallet = fundingWalletRepository.findByUidAndCurrency(uid, asset);
            if (fundingWallet == null) {
                fundingWallet = new FundingWallet();
                fundingWallet.setUid(uid);
                fundingWallet.setCurrency(asset);
                fundingWallet.setBalance(BigDecimal.ZERO);
                fundingWallet.setLockedBalance(BigDecimal.ZERO);
            }

            spotWallet.setBalance(spotWallet.getBalance().subtract(amount));
            fundingWallet.setBalance(fundingWallet.getBalance().add(amount));

            spotWalletRepository.save(spotWallet);
            fundingWalletRepository.save(fundingWallet);

        } else if ("FUNDING".equals(from) && "SPOT".equals(to)) {
            // FUNDING -> SPOT
            FundingWallet fundingWallet = fundingWalletRepository.findByUidAndCurrency(uid, asset);
            if (fundingWallet == null || fundingWallet.getBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("Insufficient balance in FUNDING wallet");
            }

            SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, asset);
            if (spotWallet == null) {
                spotWallet = new SpotWallet();
                spotWallet.setUid(uid);
                spotWallet.setCurrency(asset);
                spotWallet.setBalance(BigDecimal.ZERO);
                spotWallet.setLockedBalance(BigDecimal.ZERO);
            }

            fundingWallet.setBalance(fundingWallet.getBalance().subtract(amount));
            spotWallet.setBalance(spotWallet.getBalance().add(amount));

            fundingWalletRepository.save(fundingWallet);
            spotWalletRepository.save(spotWallet);

        } else {
            throw new IllegalArgumentException("Invalid wallet type. Use 'SPOT' or 'FUNDING'");
        }
    }
}
