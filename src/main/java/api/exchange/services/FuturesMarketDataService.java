package api.exchange.services;

import api.exchange.models.FuturesCoinData;
import api.exchange.repository.FuturesCoinDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class FuturesMarketDataService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FuturesCoinDataRepository futuresCoinDataRepository;

    private static final String BINANCE_FUTURES_API = "https://fapi.binance.com/fapi/v1";

    // Only symbols available on Binance Futures (verified)
    private static final List<String> SYMBOLS = Arrays.asList(
            "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
            "ADAUSDT", "DOGEUSDT", "TRXUSDT", "DOTUSDT", "LTCUSDT",
            "BCHUSDT", "LINKUSDT", "XLMUSDT", "ATOMUSDT", "UNIUSDT",
            "AVAXUSDT", "NEARUSDT", "FILUSDT", "ICPUSDT", "ETCUSDT");

    // Logo URLs (reuse from CoinDataService)
    private static final Map<String, String> LOGO_URLS = Map.ofEntries(
            Map.entry("BTC", "https://assets.coingecko.com/coins/images/1/large/bitcoin.png"),
            Map.entry("ETH", "https://assets.coingecko.com/coins/images/279/large/ethereum.png"),
            Map.entry("BNB", "https://assets.coingecko.com/coins/images/825/large/bnb-icon2_2x.png"),
            Map.entry("SOL", "https://assets.coingecko.com/coins/images/4128/large/solana.png"),
            Map.entry("XRP", "https://assets.coingecko.com/coins/images/44/large/xrp-symbol-white-128.png"),
            Map.entry("ADA", "https://assets.coingecko.com/coins/images/975/large/cardano.png"),
            Map.entry("DOGE", "https://assets.coingecko.com/coins/images/5/large/dogecoin.png"),
            Map.entry("TRX", "https://assets.coingecko.com/coins/images/1094/large/tron-logo.png"),
            Map.entry("DOT", "https://assets.coingecko.com/coins/images/12171/large/polkadot.png"),
            Map.entry("SUI", "https://assets.coingecko.com/coins/images/26375/large/sui_asset.jpeg"),
            Map.entry("LTC", "https://assets.coingecko.com/coins/images/2/large/litecoin.png"),
            Map.entry("BCH", "https://assets.coingecko.com/coins/images/780/large/bitcoin-cash-circle.png"),
            Map.entry("LINK", "https://assets.coingecko.com/coins/images/877/large/chainlink-new-logo.png"),
            Map.entry("XLM", "https://assets.coingecko.com/coins/images/100/large/Stellar_symbol_black_RGB.png"),
            Map.entry("ATOM", "https://assets.coingecko.com/coins/images/1481/large/cosmos_hub.png"),
            Map.entry("UNI", "https://assets.coingecko.com/coins/images/12504/large/uni.jpg"),
            Map.entry("AVAX",
                    "https://assets.coingecko.com/coins/images/12559/large/Avalanche_Circle_RedWhite_Trans.png"),
            Map.entry("NEAR", "https://assets.coingecko.com/coins/images/10365/large/near.jpg"),
            Map.entry("FIL", "https://assets.coingecko.com/coins/images/12817/large/filecoin.png"),
            Map.entry("VET", "https://assets.coingecko.com/coins/images/1167/large/VeChain-Logo-768x725.png"),
            Map.entry("ALGO", "https://assets.coingecko.com/coins/images/4380/large/download.png"),
            Map.entry("ICP", "https://assets.coingecko.com/coins/images/14495/large/Internet_Computer_logo.png"),
            Map.entry("SHIB", "https://assets.coingecko.com/coins/images/11939/large/shiba.png"),
            Map.entry("TON", "https://assets.coingecko.com/coins/images/17980/large/ton_symbol.png"),
            Map.entry("ETC", "https://assets.coingecko.com/coins/images/453/large/ethereum-classic-logo.png"));

    @Scheduled(fixedRate = 60000) // Update every 1 minute
    public void updateFuturesMarketData() {
        for (String symbol : SYMBOLS) {
            try {
                fetchAndSaveFuturesData(symbol);
                Thread.sleep(100); // Rate limit
            } catch (Exception e) {
                System.err.println("❌ Error fetching Futures data for " + symbol + ": " + e.getMessage());
            }
        }
    }

    private void fetchAndSaveFuturesData(String symbol) {
        try {
            // 1. Get Mark Price & Funding Rate
            String premiumIndexUrl = BINANCE_FUTURES_API + "/premiumIndex?symbol=" + symbol;
            ResponseEntity<String> premiumResponse = restTemplate.getForEntity(premiumIndexUrl, String.class);

            // 2. Get 24h Ticker
            String tickerUrl = BINANCE_FUTURES_API + "/ticker/24hr?symbol=" + symbol;
            ResponseEntity<String> tickerResponse = restTemplate.getForEntity(tickerUrl, String.class);

            if (premiumResponse.getStatusCode().is2xxSuccessful() && tickerResponse.getStatusCode().is2xxSuccessful()) {
                JsonNode premiumData = objectMapper.readTree(premiumResponse.getBody());
                JsonNode tickerData = objectMapper.readTree(tickerResponse.getBody());

                FuturesCoinData coinData = new FuturesCoinData();
                coinData.setSymbol(symbol);

                // Mark Price & Index Price
                coinData.setMarkPrice(new BigDecimal(premiumData.get("markPrice").asText()));
                coinData.setIndexPrice(new BigDecimal(premiumData.get("indexPrice").asText()));

                // Funding Rate
                coinData.setFundingRate(new BigDecimal(premiumData.get("lastFundingRate").asText()));
                long nextFundingTimeMs = premiumData.get("nextFundingTime").asLong();
                coinData.setNextFundingTime(
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(nextFundingTimeMs), ZoneId.systemDefault()));

                // 24h Data
                coinData.setLastPrice(new BigDecimal(tickerData.get("lastPrice").asText()));
                coinData.setPriceChange24h(new BigDecimal(tickerData.get("priceChangePercent").asText()));
                coinData.setVolume24h(new BigDecimal(tickerData.get("volume").asText()));

                // Logo
                String coinId = symbol.replace("USDT", "");
                coinData.setLogoUrl(LOGO_URLS.getOrDefault(coinId, ""));

                futuresCoinDataRepository.save(coinData);
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing Futures data for " + symbol + ": " + e.getMessage());
        }
    }

    public List<FuturesCoinData> getAllFuturesMarkets() {
        return futuresCoinDataRepository.findAllByOrderByVolume24hDesc();
    }
}
