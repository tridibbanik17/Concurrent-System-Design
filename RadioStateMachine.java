// RadioStateMachine.java
// Compile: javac RadioStateMachine.java
// Run:     java RadioStateMachine

import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class RadioStateMachine extends JFrame {
    enum State { OFF, TOP, SCANNING, TUNED, BOTTOM }

    private State state = State.OFF;
    private double frequency = 108.0;
    private static final double STEP = 0.5;
    private static final double MAX_FREQ = 108.0;
    private static final double MIN_FREQ = 88.0;

    private Timer scanTimer;

    private final JLabel stateLabel = new JLabel();
    private final JLabel freqLabel = new JLabel();
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
        setResizable(true); // allow full screen

        // Top: current state
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Current state: "));
        stateLabel.setFont(stateLabel.getFont().deriveFont(Font.BOLD, 16f));
        topPanel.add(stateLabel);
        add(topPanel, BorderLayout.NORTH);

        // Middle-top: frequency
        JPanel freqPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        freqPanel.add(new JLabel("Current frequency: "));
        freqLabel.setFont(freqLabel.getFont().deriveFont(Font.BOLD, 16f));
        freqPanel.add(freqLabel);
        add(freqPanel, BorderLayout.BEFORE_FIRST_LINE);

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

        setupShortcuts();

        updateUIFromState();

        // Default size (wider) + still resizable
        setSize(900, 600);
        setLocationRelativeTo(null); // center on screen
        setVisible(true);
    }

    private void setupShortcuts() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke('o'), "on");
        im.put(KeyStroke.getKeyStroke('s'), "scan");
        im.put(KeyStroke.getKeyStroke('r'), "reset");
        im.put(KeyStroke.getKeyStroke('f'), "off");
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
                if ("on".equals(event)) {
                    state = State.TOP;
                    frequency = MAX_FREQ;
                }
                break;
            case TOP:
                if ("scan".equals(event)) {
                    state = State.SCANNING;
                    startScanning();
                } else if ("reset".equals(event)) {
                    frequency = MAX_FREQ;
                    state = State.TOP;
                } else if ("off".equals(event)) {
                    state = State.OFF;
                }
                break;
            case SCANNING:
                if ("reset".equals(event)) {
                    stopScanning();
                    frequency = MAX_FREQ;
                    state = State.TOP;
                } else if ("off".equals(event)) {
                    stopScanning();
                    state = State.OFF;
                } else if ("lock".equals(event)) {
                    stopScanning();
                    state = State.TUNED;
                } else if ("end".equals(event)) {
                    stopScanning();
                    frequency = MIN_FREQ;
                    state = State.BOTTOM;
                }
                break;
            case TUNED:
                if ("scan".equals(event)) {
                    state = State.SCANNING;
                    startScanning();
                } else if ("reset".equals(event)) {
                    frequency = MAX_FREQ;
                    state = State.TOP;
                } else if ("off".equals(event)) {
                    state = State.OFF;
                }
                break;
            case BOTTOM:
                if ("scan".equals(event)) {
                    // explicitly do nothing, but button stays enabled
                } else if ("reset".equals(event)) {
                    frequency = MAX_FREQ;
                    state = State.TOP;
                } else if ("off".equals(event)) {
                    state = State.OFF;
                }
                break;
        }

        if (old != state) {
            log(String.format("Event '%s' : %s -> %s", event, old, state));
        } else {
            log(String.format("Event '%s' : no transition (remains %s)", event, state));
        }
        updateUIFromState();
    }

    private void startScanning() {
        stopScanning();
        scanTimer = new Timer();

        // Drop one step immediately
        frequency -= STEP;
        if (frequency <= MIN_FREQ) {
            frequency = MIN_FREQ;
            handleEvent("end");
            return;
        }
        updateUIFromState();

        scanTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (state != State.SCANNING) {
                    stopScanning();
                    return;
                }
                frequency -= STEP;
                if (frequency <= MIN_FREQ) {
                    frequency = MIN_FREQ;
                    SwingUtilities.invokeLater(() -> handleEvent("end"));
                }
                SwingUtilities.invokeLater(() -> updateUIFromState());
            }
        }, 500, 500);
    }

    private void stopScanning() {
        if (scanTimer != null) {
            scanTimer.cancel();
            scanTimer = null;
        }
    }

    private void updateUIFromState() {
        stateLabel.setText(state.toString());
        freqLabel.setText(String.format("%.1f MHz", frequency));

        btnOn.setEnabled(state == State.OFF);
        btnScan.setEnabled(state != State.OFF); 
        btnReset.setEnabled(state != State.OFF);
        btnOff.setEnabled(true);
        btnLock.setEnabled(state == State.SCANNING);
        btnEnd.setEnabled(state == State.SCANNING);

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
        SwingUtilities.invokeLater(() -> {
            RadioStateMachine app = new RadioStateMachine();

            // Optional: Uncomment this line to start in full screen
            // app.setExtendedState(JFrame.MAXIMIZED_BOTH);
        });
    }
}
