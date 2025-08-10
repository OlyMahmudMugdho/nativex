import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class NativeXGui extends JFrame {
    private JTextField jreField = new JTextField(30);
    private JTextField jarField = new JTextField(30);
    private JComboBox<String> osBox = new JComboBox<>(new String[]{"linux", "windows", "darwin"});
    private JComboBox<String> archBox = new JComboBox<>(new String[]{"amd64", "arm64"});
    private JTextField buildNameField = new JTextField(20);
    private JTextField buildLocationField = new JTextField(30);
    private JTextArea outputArea = new JTextArea(15, 50);
    private JButton buildButton = new JButton("Build");

    // Path to your native executable
    private String nativeXPath = "./nativex"; // Change if needed

    public NativeXGui() {
        super("NativeX GUI Builder");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4,4,4,4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0 - JRE folder
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("JRE Folder:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(jreField, gbc);
        JButton jreBrowse = new JButton("Browse");
        gbc.gridx = 2;
        inputPanel.add(jreBrowse, gbc);

        // Row 1 - JAR file
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("JAR File:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(jarField, gbc);
        JButton jarBrowse = new JButton("Browse");
        gbc.gridx = 2;
        inputPanel.add(jarBrowse, gbc);

        // Row 2 - OS
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Target OS:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(osBox, gbc);

        // Row 3 - Arch
        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Architecture:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(archBox, gbc);

        // Row 4 - Build Name
        gbc.gridx = 0; gbc.gridy = 4;
        inputPanel.add(new JLabel("Build Name:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(buildNameField, gbc);

        // Row 5 - Build Location
        gbc.gridx = 0; gbc.gridy = 5;
        inputPanel.add(new JLabel("Build Location:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(buildLocationField, gbc);
        JButton buildLocBrowse = new JButton("Browse");
        gbc.gridx = 2;
        inputPanel.add(buildLocBrowse, gbc);

        // Row 6 - Build Button
        gbc.gridx = 1; gbc.gridy = 6;
        inputPanel.add(buildButton, gbc);

        add(inputPanel, BorderLayout.NORTH);

        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // Browse buttons actions
        jreBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fc.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                jreField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        jarBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("JAR Files", "jar"));
            int result = fc.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                jarField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        buildLocBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fc.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                buildLocationField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        // Build button action
        buildButton.addActionListener(e -> {
            outputArea.setText("");
            buildButton.setEnabled(false);

            SwingWorker<Void, String> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        runBuildCommand();
                    } catch (Exception ex) {
                        publish("Error: " + ex.getMessage());
                    }
                    return null;
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    for (String line : chunks) {
                        outputArea.append(line + "\n");
                    }
                }

                @Override
                protected void done() {
                    buildButton.setEnabled(true);
                }

                private void runBuildCommand() throws IOException, InterruptedException {
                    ProcessBuilder pb = new ProcessBuilder();
                    pb.command(
                        nativeXPath,
                        "--jre-path", jreField.getText(),
                        "--jar-path", jarField.getText(),
                        "--os-name", osBox.getSelectedItem().toString(),
                        "--arch", archBox.getSelectedItem().toString(),
                        "--build-name", buildNameField.getText(),
                        "--build-location", buildLocationField.getText()
                    );

                    pb.redirectErrorStream(true);
                    Process proc = pb.start();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            publish(line);
                        }
                    }

                    int exitCode = proc.waitFor();
                    publish("Process exited with code: " + exitCode);
                }
            };

            worker.execute();
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NativeXGui::new);
    }
}
