package api.exchange.proto;

import com.google.protobuf.CodedOutputStream;
import java.io.IOException;

public class MarketDataProto {

    public static byte[] serializeTicker(String symbol, String price, String priceChange,
            String high, String low, String volume,
            long eventTime, String marketCap) {
        // Calculate size
        int size = 0;
        size += computeStringSize(1, symbol);
        size += computeStringSize(2, price);
        size += computeStringSize(3, priceChange);
        size += computeStringSize(4, high);
        size += computeStringSize(5, low);
        size += computeStringSize(6, volume);
        size += com.google.protobuf.CodedOutputStream.computeInt64Size(7, eventTime);
        size += computeStringSize(8, marketCap);

        byte[] buffer = new byte[size];
        try {
            CodedOutputStream output = CodedOutputStream.newInstance(buffer);
            output.writeString(1, symbol);
            output.writeString(2, price);
            output.writeString(3, priceChange);
            output.writeString(4, high);
            output.writeString(5, low);
            output.writeString(6, volume);
            output.writeInt64(7, eventTime);
            output.writeString(8, marketCap);
            output.checkNoSpaceLeft();
        } catch (IOException e) {
            throw new RuntimeException("Serializing to a byte array threw an IOException (should never happen).", e);
        }
        return buffer;
    }

    private static int computeStringSize(int fieldNumber, String value) {
        return CodedOutputStream.computeStringSize(fieldNumber, value != null ? value : "");
    }
}
