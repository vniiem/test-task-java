// © Denis Khmel (dhmel@yandex.ru), 2024

package vniiem;

import java.time.LocalDateTime;
import java.util.*;

import static vniiem.TLMReceiver.crc_16_CCITT_False;
import static vniiem.TimeManipulation.getDateTimeFromUnixUTC;

public class TLMPacket {

    private static final int TLM_OBJECT_SIZE = 26;
    private final String marker;
    private final long counter;
    private final LocalDateTime dateTime;
    private final double data;
    private final String crc;
    private boolean broken;

    public static final String decodedPacketsTableName = "Расшифровка телеметрии";
    public static final Map<String, Integer> columns = new LinkedHashMap<>();
    static {
        columns.put("Маркер", 60); //Задание столбцов: заголовок, ширина
        columns.put("Счётчик", 70);
        columns.put("Время", 150);
        columns.put("Данные", 140);
        columns.put("КС", 40);
        columns.put("Повреждён", 70);
    }

    public TLMPacket(List<String> packetThread) {

        Map<String, Integer> packetStructure = new LinkedHashMap<>();
        packetStructure.put("marker", 4);
        packetStructure.put("counter", 4);
        packetStructure.put("dateTime", 8);
        packetStructure.put("data", 8);
        packetStructure.put("crc", 2);

        Map<String, String> curPacket = new LinkedHashMap<>(); //Для хранения полученного пакета (ByteOrder.LITTLE_ENDIAN)
        Map<String, String> curPacketR = new LinkedHashMap<>(); //Для хранения исходного пакета (ByteOrder.reverse)

        for (String field : packetStructure.keySet()) {

            List<String> elements = packetThread.subList(0, packetStructure.get(field));

            curPacket.put(field, String.join("", elements));
            Collections.reverse(elements);
            curPacketR.put(field, String.join("", elements));
            elements.clear(); //Удаление обработанной части пакета из потока
        }

        marker = curPacketR.get("marker");
        counter = Long.parseUnsignedLong(curPacketR.get("counter"), 16);
        dateTime = getDateTimeFromUnixUTC(Double.longBitsToDouble(Long.parseUnsignedLong(curPacketR.get("dateTime"), 16)));
        data = Double.longBitsToDouble(Long.parseUnsignedLong(curPacketR.get("data"), 16));
        crc = curPacketR.get("crc");

        //Вычисление контрольной суммы полученного пакета (ByteOrder.LITTLE_ENDIAN)
        String receivedCrc = crc_16_CCITT_False(
                String.join("", curPacket.values()).substring(0, (TLM_OBJECT_SIZE - packetStructure.get("crc")) * 2)
        );

        //Установка признака повреждения пакета
        broken = Long.parseUnsignedLong(crc, 16) != Long.parseUnsignedLong(receivedCrc, 16);
    }

    public Object[] getView() {
        return new Object[]{
                getMarker(),
                getCounter(),
                getDateTime(),
                getData(),
                getCrc(),
                isBroken() ? "да" : ""
        };
    }

    public String getMarker() {
        return marker;
    }

    public long getCounter() {
        return counter;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public double getData() {
        return data;
    }

    public String getCrc() {
        return crc;
    }

    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean status) {
        this.broken = status;
    }
}
