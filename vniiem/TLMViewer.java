// © Denis Khmel (dhmel@yandex.ru), 2024

package vniiem;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class TLMViewer extends JFrame {

    private final DefaultTableModel model = new DefaultTableModel();

    public DefaultTableModel getModel() {
        return model;
    }

    public TLMViewer(String title, Map<String, Integer> columns) {

        model.setColumnIdentifiers(columns.keySet().toArray());
        JTable table = new JTable(model);
        int frameWidth = 50;
        for (String col : columns.keySet()) {
            table.getColumn(col).setPreferredWidth(columns.get(col));
            frameWidth += columns.get(col);
        }

        JPanel upperPanel = new JPanel();
        upperPanel.add(table.getTableHeader());

        JPanel tablePanel = new JPanel();
        tablePanel.add(table);

        setLayout(new BorderLayout());
        add(upperPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(tablePanel);
        add(scrollPane, BorderLayout.CENTER);
        add(getButtonPanel(), BorderLayout.SOUTH);

        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(frameWidth, (int) (frameWidth * 0.75)); //4:3
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel getButtonPanel() {

        JButton clearButton = new JButton("Сбросить ");
        clearButton.addActionListener(e -> IntStream
                .range(0, model.getRowCount())
                .map(i -> 0)
                .forEach(model::removeRow)
        );

        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> {
            File fileToSaveTLM = showFileSelector();
            if (fileToSaveTLM == null) return;

            String fullName = fileToSaveTLM.getPath();
            fullName = fullName.endsWith(".csv") ? fullName : fullName + ".csv";

            try {
                Files.writeString(Path.of(fullName), createCSV(model), Charset.forName("windows-1251"));
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(clearButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(12, 0)));
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    private String createCSV(DefaultTableModel model) {
        StringBuilder csv = new StringBuilder();

        for (int i = 0; i < model.getColumnCount(); i++) {
            csv.append(model.getColumnName(i)).append(";");
        }

        for (int i = 0; i < model.getRowCount(); i++) {
            csv.append("\n");

            for (int j = 0; j < model.getColumnCount(); j++) {
                csv.append(model.getValueAt(i, j)).append(";");
            }
        }
        return csv.toString();
    }

    private static void setWindowTheme() {

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Windows Classic".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            Logger.getLogger(UIManager.class.getName()).log(Level.SEVERE, "Failed to apply Windows Classic theme", e);
        }
    }

    public static File showFileSelector() {

        TLMViewer.setWindowTheme();

        UIManager.put("FileChooser.saveInLabelText", "Сохранить в:");
        UIManager.put("FileChooser.fileNameLabelText", "Имя файла:");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Тип файла:");
        UIManager.put("FileChooser.saveButtonText", "Сохранить");
        UIManager.put("FileChooser.cancelButtonText", "Отмена");

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setDialogTitle("Сохранение таблицы в файл");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV (разделители - запятые)", "csv"));

        if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return fileChooser.getSelectedFile();
    }
}
