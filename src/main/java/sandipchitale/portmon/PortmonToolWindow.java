package sandipchitale.portmon;

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class PortmonToolWindow {
    private static final Logger LOG = Logger.getInstance(PortmonToolWindow.class);

    private static String[] netstatCommand;

    static {
        netstatCommand = new String[]{
                "netstat",
                "-anpt46",
        };
        if (SystemInfo.isWindows) {
            netstatCommand = new String[]{
                    "netstat",
                    "",
            };
        } else if (SystemInfo.isMac) {
            netstatCommand = new String[]{
                    "netstat",
                    "",
            };
        }
    }

    private final JPanel contentToolWindow;

    private final DefaultTableModel netstatTableModel;
    private final JBTable netstatTable;
    private final JCheckBox closeWaitCheckbox;
    private final JCheckBox establishedCheckbox;
    private final JCheckBox listeningCheckbox;
    private final JCheckBox timeWaitCheckbox;
    private final JTextField ports;
    private final Project project;

    public PortmonToolWindow(Project project) {
        this.project = project;
        this.contentToolWindow = new SimpleToolWindowPanel(true, true);

        this.netstatTableModel = new DefaultTableModel(
                new Object[]{
                        "Local Address",
                        "Local Port",
                        "Remote Address",
                        "Remote Port",
                        "State",
                        "PID",
                        " "
                }, 0) {

            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 1 || col == 3 || col == 5) {
                    return Integer.class;
                } else if (col == 6) {
                    return Icon.class;
                } else return String.class;  //other columns accept String values
            }
        };

        this.netstatTable = new JBTable(netstatTableModel);
        JBScrollPane scrollPane = new JBScrollPane(netstatTable);
        this.contentToolWindow.add(scrollPane, BorderLayout.CENTER);

        TableCellRenderer blankMinusOneTableCellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value.equals(-1)) {
                    label.setText("");
                }
                return label;
            }
        };

        TableColumn column = this.netstatTable.getColumnModel().getColumn(1);
        column.setMinWidth(140);
        column.setWidth(140);
        column.setMaxWidth(140);

        column = this.netstatTable.getColumnModel().getColumn(3);
        column.setMinWidth(140);
        column.setWidth(140);
        column.setMaxWidth(140);
        column.setCellRenderer(blankMinusOneTableCellRenderer);

        column = this.netstatTable.getColumnModel().getColumn(5);
        column.setMinWidth(140);
        column.setWidth(140);
        column.setMaxWidth(140);
        column.setCellRenderer(blankMinusOneTableCellRenderer);

        ActionListener callNetstat = (ActionEvent actionEvent) -> {
            netstat(project, contentToolWindow, netstatTable, netstatTableModel);
        };

        Action killProcessId = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                JTable table = (JTable) actionEvent.getSource();
                int modelRow = Integer.parseInt(actionEvent.getActionCommand());
                killProcessId((Integer) table.getModel().getValueAt(modelRow, 5));
            }
        };

        column = this.netstatTable.getColumnModel().getColumn(6);
        column.setMinWidth(100);
        column.setWidth(100);
        column.setMaxWidth(100);

        ButtonColumn buttonColumn = new ButtonColumn(this.netstatTable, killProcessId, 6);

        JPanel toolBars = new JPanel(new BorderLayout());

        JPanel topToolBar = new JPanel(new BorderLayout());
        toolBars.add(topToolBar, BorderLayout.NORTH);
        topToolBar.add(new JSeparator(), BorderLayout.SOUTH);

        JPanel topLeftToolBar = new JPanel(new BorderLayout());
        topToolBar.add(topLeftToolBar, BorderLayout.CENTER);

        JLabel portsLabel = new JLabel("Monitor Ports: ");
        portsLabel.setIcon(PortmonIcons.ToolWindow);
        topLeftToolBar.add(portsLabel, BorderLayout.WEST);

        ports = new JTextField();
        ports.setText(PortmonSettings.getPorts());
        topLeftToolBar.add(ports, BorderLayout.CENTER);
        ports.addActionListener(callNetstat);

        JButton nestatButton = new JButton(AllIcons.Actions.Refresh);
        topLeftToolBar.add(nestatButton, BorderLayout.EAST);
        nestatButton.addActionListener(callNetstat);

        JPanel topRightToolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topToolBar.add(topRightToolBar, BorderLayout.EAST);

        JLabel statesLabel = new JLabel("States: ");
        topRightToolBar.add(statesLabel);

        closeWaitCheckbox = new JCheckBox("Close wait", PortmonSettings.isCloseWait());
        topRightToolBar.add(closeWaitCheckbox);
        closeWaitCheckbox.addActionListener(callNetstat);
        establishedCheckbox = new JCheckBox("Established",  PortmonSettings.isEstablished());
        topRightToolBar.add(establishedCheckbox);
        establishedCheckbox.addActionListener(callNetstat);
        listeningCheckbox = new JCheckBox("Listening",  PortmonSettings.isListening());
        topRightToolBar.add(listeningCheckbox);
        listeningCheckbox.addActionListener(callNetstat);
        timeWaitCheckbox = new JCheckBox("Time wait", PortmonSettings.isTimeWait());
        topRightToolBar.add(timeWaitCheckbox);
        timeWaitCheckbox.addActionListener(callNetstat);

        this.contentToolWindow.add(toolBars, BorderLayout.NORTH);

        netstat(project, contentToolWindow, netstatTable, netstatTableModel);
    }

    private void netstat(Project project, JPanel contentToolWindow, JBTable netstatTable, DefaultTableModel netstatTableModel) {
        contentToolWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ApplicationManager.getApplication().runReadAction(() -> {
                netstatTableModel.setRowCount(0);
                Process netstatProcess = null;
                try {
                    netstatProcess = new ProcessBuilder().command(netstatCommand).start();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(netstatProcess.getInputStream()))) {
                        Set<Integer> portsToMonitor = new TreeSet<>();
                        String portsToMonitorString = ports.getText();
                        if (!portsToMonitorString.trim().isEmpty()) {
                            Arrays.stream(portsToMonitorString.split(","))
                                    .mapToInt(Integer::parseInt)
                                    .forEach(portsToMonitor::add);
                        }
                        // Remember
                        PortmonSettings.setPorts(portsToMonitorString);
                        PortmonSettings.setCloseWait(closeWaitCheckbox.isSelected());
                        PortmonSettings.setEstablished(establishedCheckbox.isSelected());
                        PortmonSettings.setListening(listeningCheckbox.isSelected());
                        PortmonSettings.setTimeWait(timeWaitCheckbox.isSelected());
                        reader.lines()
                                .dropWhile((String line) -> !line.startsWith("tcp"))
                                .map(String::trim)
                                .forEach((String trimmedLine) -> {
                                    NetstatLine netstatLine = NetstatLine.parse(System.currentTimeMillis(), trimmedLine);
                                    // Apply filters
                                    if (netstatLine.state().equals(NetstatLine.State.CLOSE_WAIT) && !closeWaitCheckbox.isSelected()) {
                                        return;
                                    } else if (netstatLine.state().equals(NetstatLine.State.ESTABLISHED) && !establishedCheckbox.isSelected()) {
                                        return;
                                    } else if (netstatLine.state().equals(NetstatLine.State.LISTENING) && !listeningCheckbox.isSelected()) {
                                        return;
                                    } else if (netstatLine.state().equals(NetstatLine.State.TIME_WAIT) && !timeWaitCheckbox.isSelected()) {
                                        return;
                                    }
                                    if (portsToMonitor.isEmpty() || portsToMonitor.contains(netstatLine.localPort())) {
                                        netstatTableModel.addRow(
                                                new Object[]{
                                                        netstatLine.localAddress(),
                                                        netstatLine.localPort(),
                                                        netstatLine.foreignAddress(),
                                                        netstatLine.foreignPort(),
                                                        netstatLine.state(),
                                                        netstatLine.pid(),
                                                        AllIcons.Actions.DeleteTag
                                                }
                                        );
                                    }
                                });

                    }
                } catch (IOException e) {
                    LOG.error("Failed to run `" + String.join(" ", netstatCommand) + "'", e);
                } finally {
                    if (netstatProcess != null) {
                        try {
                            int exitCode = netstatProcess.waitFor();
                            if (exitCode != 0) {
                                LOG.error("Command `" + String.join(" ", netstatCommand) + "' exited with code: " + exitCode);
                            }
                        } catch (InterruptedException ignore) {
                        }
                    }
                    contentToolWindow.setCursor(null);
                }
            });
        });
    }

    private void killProcessId(int pid) {
        if (pid == -1) {
            return;
        }

        int responseIndex = Messages.showYesNoDialog(project,
                "Kill Process: " + pid,
                "Kill Process",
                CommonBundle.getYesButtonText(),
                CommonBundle.getNoButtonText(),
                AllIcons.Actions.DeleteTag);
        if (responseIndex == Messages.YES) {
            killProcessIdImpl(pid);
        }
    }

    private void killProcessIdImpl(int pid) {
        if (pid == -1) {
            return;
        }

        ProcessHandle.of(pid)
                .ifPresentOrElse((ProcessHandle process) -> {
                    LOG.info("Killing process id: " + pid);
                    process.destroy();
                    LOG.info("Process with PID " + pid + " has been terminated.");
                    netstat(project, contentToolWindow, netstatTable, netstatTableModel);
                }, () -> {
                    LOG.info("No process found with PID " + pid);
                });
    }

    public JComponent getContent() {
        return this.contentToolWindow;
    }
}