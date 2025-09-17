// RadioStateMachine.java
// Compile: javac RadioStateMachine.java
// Run:     java RadioStateMachine

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;

public class RadioStateMachine extends JFrame {
    enum State { OFF, TOP, SCANNING, TUNED, BOTTOM }

    private State state = State.OFF;
    private final JLabel stateLabel = new JLabel();
    private final JTextArea log = new JTextArea(8, 40);
    private final JButton btnOn = new JButton("on");
    private final JButton btnScan = new JButton("scan");
    private final JButton btnReset = new JButton("reset");
    private final JButton btnOff = new JButton("off");
    private final JButton btnLock = new JButton("lock");
    private final JButton btnEnd = new JButton("end");

    public RadioStateMachine() {
        super("Radio State Machine");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8,8));
        setResizable(false);

        // Top: current state
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Current state: "));
        stateLabel.setFont(stateLabel.getFont().deriveFont(Font.BOLD, 16f));
        topPanel.add(stateLabel);
        add(topPanel, BorderLayout.NORTH);

        // Center: buttons
        JPanel center = new JPanel(new GridLayout(2, 3, 8, 8));
        center.add(btnOn);
        center.add(btnScan);
        center.add(btnReset);
        center.add(btnOff);
        center.add(btnLock);
        center.add(btnEnd);
        add(center, BorderLayout.CENTER);

        // Bottom: log
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(log);
        add(sp, BorderLayout.SOUTH);

        // Attach actions
        btnOn.addActionListener(e -> handleEvent("on"));
        btnScan.addActionListener(e -> handleEvent("scan"));
        btnReset.addActionListener(e -> handleEvent("reset"));
        btnOff.addActionListener(e -> handleEvent("off"));
        btnLock.addActionListener(e -> handleEvent("lock"));
        btnEnd.addActionListener(e -> handleEvent("end"));

        // Keyboard shortcuts
        setupShortcuts();

        // initialize UI
        updateUIFromState();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupShortcuts() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke('o'), "on");
        im.put(KeyStroke.getKeyStroke('s'), "scan");
        im.put(KeyStroke.getKeyStroke('r'), "reset");
        im.put(KeyStroke.getKeyStroke('f'), "off"); // 'f' for off
        im.put(KeyStroke.getKeyStroke('l'), "lock");
        im.put(KeyStroke.getKeyStroke('e'), "end");

        am.put("on", new AbstractAction(){ public void actionPerformed(ActionEvent e){ handleEvent("on"); }});
        am.put("scan", new AbstractAction(){ public void actionPerformed(ActionEvent e){ handleEvent("scan"); }});
        am.put("reset", new AbstractAction(){ public void actionPerformed(ActionEvent e){ handleEvent("reset"); }});
        am.put("off", new AbstractAction(){ public void actionPerformed(ActionEvent e){ handleEvent("off"); }});
        am.put("lock", new AbstractAction(){ public void actionPerformed(ActionEvent e){ handleEvent("lock"); }});
        am.put("end", new AbstractAction(){ public void actionPerformed(ActionEvent e){ handleEvent("end"); }});
    }

    private void handleEvent(String event) {
        State old = state;
        switch (state) {
            case OFF:
                if ("on".equals(event)) state = State.TOP;
                // other events in OFF do nothing except off (stay) or invalid
                break;
            case TOP:
                if ("scan".equals(event)) state = State.SCANNING;
                else if ("reset".equals(event)) state = State.TOP; // stays TOP
                else if ("off".equals(event)) state = State.OFF;
                break;
            case SCANNING:
                if ("scan".equals(event)) state = State.SCANNING; // stay scanning
                else if ("reset".equals(event)) state = State.TOP;
                else if ("off".equals(event)) state = State.OFF;
                else if ("lock".equals(event)) state = State.TUNED;
                else if ("end".equals(event)) state = State.BOTTOM;
                break;
            case TUNED:
                if ("scan".equals(event)) state = State.SCANNING;
                else if ("reset".equals(event)) state = State.TOP;
                else if ("off".equals(event)) state = State.OFF;
                break;
            case BOTTOM:
                if ("scan".equals(event)) state = State.BOTTOM; // stays BOTTOM
                else if ("reset".equals(event)) state = State.TOP;
                else if ("off".equals(event)) state = State.OFF;
                break;
        }

        // Log and update UI
        if (old != state) {
            log(String.format("Event '%s' : %s -> %s", event, old, state));
        } else {
            log(String.format("Event '%s' : no transition (remains %s)", event, state));
        }
        updateUIFromState();
    }

    private void updateUIFromState() {
        stateLabel.setText(state.toString());
        // enable/disable buttons depending on state (optional, but helpful)
        btnOn.setEnabled(state == State.OFF);
        btnScan.setEnabled(state == State.TOP || state == State.SCANNING || state == State.TUNED || state == State.BOTTOM);
        btnReset.setEnabled(state != State.OFF);
        btnOff.setEnabled(true); // off should always be usable
        btnLock.setEnabled(state == State.SCANNING);
        btnEnd.setEnabled(state == State.SCANNING);

        // Visual hint (background color per state)
        Color bg;
        switch (state) {
            case OFF: bg = Color.LIGHT_GRAY; break;
            case TOP: bg = new Color(200, 230, 255); break;
            case SCANNING: bg = new Color(255, 250, 205); break;
            case TUNED: bg = new Color(205, 255, 205); break;
            case BOTTOM: bg = new Color(255, 220, 220); break;
            default: bg = Color.WHITE;
        }
        getContentPane().setBackground(bg);
        stateLabel.setForeground(Color.DARK_GRAY);
    }

    private void log(String text) {
        String time = LocalTime.now().withNano(0).toString();
        log.append("[" + time + "] " + text + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    public static void main(String[] args) {
        // Schedule on EDT
        SwingUtilities.invokeLater(RadioStateMachine::new);
    }
}
