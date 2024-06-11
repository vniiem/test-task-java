// © Denis Khmel (dhmel@yandex.ru), 2024

package vniiem;

import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

public class TLMReceiver {

    private static final int UDP_PORT = 15000;
    private static final int TLM_OBJECT_SIZE = 26;
    private static final byte[] buf = new byte[25];

    public static String crc_16_CCITT_False(String hexString) {

        byte[] destByte = new byte[hexString.length() / 2];
        int j = 0;
        for (int i = 0; i < destByte.length; i++) {
            byte high = (byte) (Character.digit(hexString.charAt(j), 16) & 0xff);
            byte low = (byte) (Character.digit(hexString.charAt(j + 1), 16) & 0xff);
            destByte[i] = (byte) (high << 4 | low);
            j += 2;
        }
        int crc = 0xffff; // initial value
        int polynomial = 0x1021; // poly value
        for (byte b : destByte) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit)
                    crc ^= polynomial;
            }
        }
        crc &= 0xffff;
        return Integer.toHexString(crc).toUpperCase();
    }

    public static void listenLoopUDP(DefaultTableModel model) throws SocketException {

        DatagramSocket socket = new DatagramSocket(UDP_PORT);
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        List<String> marker = List.of("78", "56", "34", "12");

        List<String> packetThread = new LinkedList<>(); //Поток байт из packet
        long counter = 0; //Переменная для проверки корректности счётчика пакета

        while (true) {

            while (packetThread.size() < TLM_OBJECT_SIZE) {

                try {
                    socket.receive(packet);
                    for (byte b : packet.getData()) packetThread.add(String.format("%02X", b));

                    int mark = Collections.indexOfSubList(packetThread, marker); //Определение позиции первого маркера
                    if (mark > 0)
                        packetThread.subList(0, mark).clear(); //Удаление части сообщения перед первым маркером
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    socket.close();
                    break;
                }
            }

            if (socket.isClosed()) break;

            TLMPacket currPacket = new TLMPacket(packetThread); //Выделение и обработка пакета

            //Проверка корректности счётчика
            if (model.getRowCount() == 0) {
                counter = currPacket.getCounter();
            } else {
                long last = currPacket.getCounter();
                if (last == counter + 1 || last == 1) {
                    counter = last;
                } else {
                    currPacket.setBroken(true);
                }
            }
            model.addRow(currPacket.getView());
        }
    }

    public static void main(String[] args) {

        TLMViewer tlmTable = new TLMViewer(TLMPacket.decodedPacketsTableName, TLMPacket.columns);

        try {
            listenLoopUDP(tlmTable.getModel());
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
    }
}
