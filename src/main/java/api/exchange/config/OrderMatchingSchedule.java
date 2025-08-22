// package api.exchange.config;

// import java.util.Set;
// import java.util.concurrent.ConcurrentHashMap;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Component;

// import api.exchange.services.OrderBooksService;
// import lombok.extern.slf4j.Slf4j;

// @Component
// @Slf4j
// public class OrderMatchingSchedule {

//     private final OrderBooksService orderBookService;    

//     private final Set<String> symbolsToMatch = ConcurrentHashMap.newKeySet();
    
//     public void scheduleMatching(String symbol) {
//         symbolsToMatch.add(symbol);
//     }
    
//     @Scheduled(fixedRate = 1000) // Chạy mỗi giây
//     public void processScheduledMatching() {
//         if (symbolsToMatch.isEmpty()) {
//             return;
//         }
        
//         for (String symbol : symbolsToMatch) {
//             try {
//                 orderBooksService.matchOrders(symbol);
//             } catch (Exception e) {
//                 log.error("Error matching orders for symbol: {}", symbol, e);
//             }
//         }
        
//         symbolsToMatch.clear();
//     }
// }
