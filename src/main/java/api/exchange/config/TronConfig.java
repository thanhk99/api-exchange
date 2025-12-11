package api.exchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tron.trident.core.ApiWrapper;

@Configuration
public class TronConfig {

    // Using Nile Testnet.
    // Trident uses gRPC, so we need the gRPC endpoints.
    public static final String TRON_FULL_NODE = "grpc.nile.trongrid.io:50051";
    public static final String TRON_SOLIDITY_NODE = "grpc.nile.trongrid.io:50052";
    public static final String TRON_EVENT_SERVER = "https://event.nile.trongrid.io";
    // JSON

    @Bean
    public ApiWrapper apiWrapper() {
        // Generates a random key for the wrapper context if none provided.
        // In a real app, you might want a meaningful default or use a specific key.
        return new ApiWrapper(TRON_FULL_NODE, TRON_SOLIDITY_NODE,
                "1111111111111111111111111111111111111111111111111111111111111111");
    }
}
