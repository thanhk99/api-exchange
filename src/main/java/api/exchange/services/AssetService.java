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

    public Map<String, Object> getAssetOverview(String uid) {
        List<FundingWallet> fundingWallets = fundingWalletRepository.findAllByUid(uid);
        List<SpotWallet> spotWallets = spotWalletRepository.findAllByUid(uid);
        List<EarnWallet> earnWallets = earnWalletRepository.findAllByUid(uid);

        Map<String, Object> fundingData = processFundingWallets(fundingWallets);
        Map<String, Object> spotData = processSpotWallets(spotWallets);
        Map<String, Object> earnData = processEarnWallets(earnWallets);

        BigDecimal totalAssetUsd = BigDecimal.ZERO;
        totalAssetUsd = totalAssetUsd.add((BigDecimal) fundingData.get("totalUsd"));
        totalAssetUsd = totalAssetUsd.add((BigDecimal) spotData.get("totalUsd"));
        totalAssetUsd = totalAssetUsd.add((BigDecimal) earnData.get("totalUsd"));

        Map<String, Object> response = new HashMap<>();
        response.put("totalAssetUsd", totalAssetUsd);
        response.put("funding", fundingData);
        response.put("spot", spotData);
        response.put("earn", earnData);
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

    private BigDecimal getPrice(String currency) {
        if ("USDT".equalsIgnoreCase(currency) || "USD".equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }
        Optional<coinModel> coinOpt = coinRepository.findById(currency);
        return coinOpt.map(coinModel::getCurrentPrice).orElse(BigDecimal.ZERO);
    }
}
