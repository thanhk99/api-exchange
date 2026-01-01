package api.exchange.services;

import api.exchange.dtos.Response.KlinesSpotResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class RingBufferService {

    private static final int BUFFER_SIZE = 72; // Giới hạn 72 nến cho mỗi symbol

    private final Map<String, SpotRingBuffer<KlinesSpotResponse>> symbolBuffers = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void addKlineData(KlinesSpotResponse klineData) {
        if (klineData == null || klineData.getSymbol() == null) {
            return;
        }

        String symbol = klineData.getSymbol().toUpperCase();

        lock.writeLock().lock();
        try {
            SpotRingBuffer<KlinesSpotResponse> buffer = symbolBuffers.computeIfAbsent(
                    symbol,
                    k -> new SpotRingBuffer<>(BUFFER_SIZE));

            KlinesSpotResponse last = buffer.peekLast();
            if (last != null && last.getStartTime() == klineData.getStartTime()) {
                // Trùng timestamp, cập nhật nến hiện tại (giữ giá open, update giá
                // close/high/low/volume)
                buffer.updateLast(klineData);
            } else if (last == null || klineData.getStartTime() > last.getStartTime()) {
                // Nến mới hoàn toàn
                buffer.add(klineData);
            }
            // Bỏ qua nếu tin nhắn cũ hơn nến cuối (hiếm khi xảy ra nhưng giúp ổn định biểu
            // đồ)
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Lấy tất cả dữ liệu kline của một symbol
     */
    public List<KlinesSpotResponse> getKlineData(String symbol) {
        if (symbol == null) {
            return Collections.emptyList();
        }

        String upperSymbol = symbol.toUpperCase();

        lock.readLock().lock();
        try {
            SpotRingBuffer<KlinesSpotResponse> buffer = symbolBuffers.get(upperSymbol);
            if (buffer == null) {
                return Collections.emptyList();
            }
            return buffer.getAll();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Lấy số lượng nến hiện tại của một symbol
     * 
     * @param symbol Symbol cần kiểm tra
     * @return Số lượng nến hiện tại
     */
    public int getCurrentSize(String symbol) {
        if (symbol == null) {
            return 0;
        }

        String upperSymbol = symbol.toUpperCase();

        lock.readLock().lock();
        try {
            SpotRingBuffer<KlinesSpotResponse> buffer = symbolBuffers.get(upperSymbol);
            return buffer != null ? buffer.size() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Lấy danh sách tất cả symbols có dữ liệu
     * 
     * @return Set các symbols
     */
    public Set<String> getAllSymbols() {
        lock.readLock().lock();
        try {
            return new HashSet<>(symbolBuffers.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Xóa dữ liệu của một symbol
     * 
     * @param symbol Symbol cần xóa
     */
    public void clearSymbolData(String symbol) {
        if (symbol == null) {
            return;
        }

        String upperSymbol = symbol.toUpperCase();

        lock.writeLock().lock();
        try {
            symbolBuffers.remove(upperSymbol);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Xóa tất cả dữ liệu
     */
    public void clearAllData() {
        lock.writeLock().lock();
        try {
            symbolBuffers.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * SpotRingBuffer implementation để lưu trữ dữ liệu với giới hạn kích thước
     */
    public static class SpotRingBuffer<T> {
        private final T[] buffer;
        private final int capacity;
        private int head = 0;
        private int tail = 0;
        private int count = 0;

        @SuppressWarnings("unchecked")
        public SpotRingBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = (T[]) new Object[capacity];
        }

        public synchronized void add(T item) {
            buffer[tail] = item;
            tail = (tail + 1) % capacity;

            if (count < capacity) {
                count++;
            } else {
                head = (head + 1) % capacity;
            }
        }

        public synchronized T peekLast() {
            if (count == 0)
                return null;
            int lastIndex = (tail - 1 + capacity) % capacity;
            return buffer[lastIndex];
        }

        public synchronized void updateLast(T item) {
            if (count == 0) {
                add(item);
                return;
            }
            int lastIndex = (tail - 1 + capacity) % capacity;
            buffer[lastIndex] = item;
        }

        public synchronized List<T> getAll() {
            List<T> result = new ArrayList<>(count);

            if (count == 0) {
                return result;
            }

            if (count < capacity) {
                // Buffer chưa đầy, lấy từ đầu đến tail
                for (int i = 0; i < count; i++) {
                    result.add(buffer[i]);
                }
            } else {
                // Buffer đã đầy, lấy từ head đến tail
                for (int i = 0; i < capacity; i++) {
                    int index = (head + i) % capacity;
                    result.add(buffer[index]);
                }
            }

            return result;
        }

        public synchronized int size() {
            return count;
        }

        public synchronized boolean isEmpty() {
            return count == 0;
        }

        public synchronized boolean isFull() {
            return count == capacity;
        }
    }
}
