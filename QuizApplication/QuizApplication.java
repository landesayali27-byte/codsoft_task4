import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

// ==========================================
// 1. QUESTION MODEL
// ==========================================
class Question {
    private final String questionText;
    private final String[] options;
    private final int correctOptionIndex; // 0-indexed

    public Question(String questionText, String[] options, int correctOptionIndex) {
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String[] getOptions() {
        return options;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }
}

// User Answer Record for the Result Screen Summary
class QuizRecord {
    Question question;
    int selectedIndex; // -1 if timed out without selection

    public QuizRecord(Question question, int selectedIndex) {
        this.question = question;
        this.selectedIndex = selectedIndex;
    }

    public boolean isCorrect() {
        return selectedIndex == question.getCorrectOptionIndex();
    }
}

// ==========================================
// 2. MAIN APPLICATION GUI
// ==========================================
public class QuizApplication extends JFrame {

    // Palette Colors
    private static final Color BG_DARK = new Color(20, 22, 31);
    private static final Color CARD_BG = new Color(29, 33, 47);
    private static final Color ACCENT_BLUE = new Color(79, 110, 247);
    private static final Color TEXT_WHITE = new Color(240, 243, 250);
    private static final Color TEXT_MUTED = new Color(155, 162, 180);
    private static final Color COLOR_GREEN = new Color(56, 178, 172);
    private static final Color COLOR_RED = new Color(239, 68, 68);
    private static final Color BORDER_COLOR = new Color(45, 52, 74);

    private static final int TIME_PER_QUESTION = 15; // 15 seconds

    // Quiz Data
    private final List<Question> questionBank = new ArrayList<>();
    private final List<QuizRecord> userRecords = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;

    // Timer Variables
    private Timer timer;
    private int secondsRemaining;

    // Layout & Panes
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel containerPanel = new JPanel(cardLayout);

    // Active Quiz Components
    private JLabel questionNumberLabel;
    private JLabel timerLabel;
    private JProgressBar timerProgressBar;
    private JLabel questionTextLabel;
    private JRadioButton[] optionButtons;
    private ButtonGroup optionsGroup;
    private JButton submitButton;

    // Result Screen Components
    private JLabel finalScoreBadge;
    private JPanel summaryListPanel;

    public QuizApplication() {
        setTitle("Interactive Java Quiz Challenge");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(680, 620);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BG_DARK);

        loadSampleQuestions();

        containerPanel.add(buildQuizScreen(), "QUIZ");
        containerPanel.add(buildResultScreen(), "RESULT");

        add(containerPanel);
        displayQuestion(0);
    }

    private void loadSampleQuestions() {
        questionBank.add(new Question(
                "Which component of Java is responsible for running bytecode?",
                new String[]{"JDK", "JVM", "JRE", "JDB"},
                1
        ));
        questionBank.add(new Question(
                "Which collection class allows key-value pairs with null keys and values?",
                new String[]{"Hashtable", "ConcurrentHashMap", "HashMap", "TreeMap"},
                2
        ));
        questionBank.add(new Question(
                "What is the default value of a boolean primitive variable in Java?",
                new String[]{"true", "false", "0", "null"},
                1
        ));
        questionBank.add(new Question(
                "Which OOP concept is achieved using Interfaces and Abstract classes?",
                new String[]{"Polymorphism & Abstraction", "Encapsulation only", "Compilation", "Garbage Collection"},
                0
        ));
        questionBank.add(new Question(
                "Which of the following exceptions is an unchecked exception?",
                new String[]{"IOException", "SQLException", "NullPointerException", "ClassNotFoundException"},
                2
        ));
    }

    // ==========================================
    // SCREEN 1: ACTIVE QUIZ VIEW
    // ==========================================
    private JPanel buildQuizScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);

        // Header: Tracker, Timer, and Progress Bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BG_DARK);
        topPanel.setBorder(new EmptyBorder(20, 25, 10, 25));

        JPanel statsRow = new JPanel(new BorderLayout());
        statsRow.setOpaque(false);

        questionNumberLabel = new JLabel("Question 1 of " + questionBank.size());
        questionNumberLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        questionNumberLabel.setForeground(TEXT_MUTED);

        timerLabel = new JLabel("Time: " + TIME_PER_QUESTION + "s");
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        timerLabel.setForeground(COLOR_RED);

        statsRow.add(questionNumberLabel, BorderLayout.WEST);
        statsRow.add(timerLabel, BorderLayout.EAST);

        timerProgressBar = new JProgressBar(0, TIME_PER_QUESTION);
        timerProgressBar.setValue(TIME_PER_QUESTION);
        timerProgressBar.setPreferredSize(new Dimension(100, 6));
        timerProgressBar.setForeground(ACCENT_BLUE);
        timerProgressBar.setBackground(new Color(36, 42, 60));
        timerProgressBar.setBorderPainted(false);

        topPanel.add(statsRow, BorderLayout.NORTH);
        topPanel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        topPanel.add(timerProgressBar, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);

        // Center: Question card & Options
        JPanel centerCard = new JPanel();
        centerCard.setLayout(new BoxLayout(centerCard, BoxLayout.Y_AXIS));
        centerCard.setBackground(CARD_BG);
        centerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));

        questionTextLabel = new JLabel("Loading question...");
        questionTextLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        questionTextLabel.setForeground(TEXT_WHITE);
        questionTextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        centerCard.add(questionTextLabel);
        centerCard.add(Box.createVerticalStrut(25));

        optionButtons = new JRadioButton[4];
        optionsGroup = new ButtonGroup();

        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("SansSerif", Font.PLAIN, 14));
            optionButtons[i].setForeground(TEXT_WHITE);
            optionButtons[i].setBackground(CARD_BG);
            optionButtons[i].setFocusPainted(false);
            optionButtons[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            optionsGroup.add(optionButtons[i]);

            centerCard.add(optionButtons[i]);
            centerCard.add(Box.createVerticalStrut(12));
        }

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(BG_DARK);
        centerWrapper.setBorder(new EmptyBorder(10, 25, 10, 25));
        centerWrapper.add(centerCard, BorderLayout.CENTER);

        panel.add(centerWrapper, BorderLayout.CENTER);

        // Footer: Submit Button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(BG_DARK);
        bottomPanel.setBorder(new EmptyBorder(10, 25, 20, 25));

        submitButton = new JButton("Submit Answer");
        submitButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitButton.setBackground(ACCENT_BLUE);
        submitButton.setForeground(Color.WHITE);
        submitButton.setFocusPainted(false);
        submitButton.setPreferredSize(new Dimension(160, 42));
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(e -> processAnswer(false));

        bottomPanel.add(submitButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // SCREEN 2: FINAL SUMMARY & RESULT VIEW
    // ==========================================
    private JPanel buildResultScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(25, 25, 20, 25));

        // Result Header
        JPanel topResult = new JPanel(new GridLayout(2, 1, 0, 5));
        topResult.setOpaque(false);

        JLabel title = new JLabel("Quiz Completed!", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);

        finalScoreBadge = new JLabel("Score: 0 / 0", SwingConstants.CENTER);
        finalScoreBadge.setFont(new Font("SansSerif", Font.BOLD, 17));
        finalScoreBadge.setForeground(COLOR_GREEN);

        topResult.add(title);
        topResult.add(finalScoreBadge);
        panel.add(topResult, BorderLayout.NORTH);

        // Scrollable Breakdown List
        summaryListPanel = new JPanel();
        summaryListPanel.setLayout(new BoxLayout(summaryListPanel, BoxLayout.Y_AXIS));
        summaryListPanel.setBackground(BG_DARK);

        JScrollPane scrollPane = new JScrollPane(summaryListPanel);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Restart Button
        JButton restartBtn = new JButton("Restart Quiz");
        restartBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        restartBtn.setBackground(ACCENT_BLUE);
        restartBtn.setForeground(Color.WHITE);
        restartBtn.setFocusPainted(false);
        restartBtn.setPreferredSize(new Dimension(160, 40));
        restartBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        restartBtn.addActionListener(e -> restartQuiz());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        bottomPanel.add(restartBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // LOGIC & LIFECYCLE
    // ==========================================
    private void displayQuestion(int index) {
        if (index >= questionBank.size()) {
            showFinalResults();
            return;
        }

        Question q = questionBank.get(index);
        questionNumberLabel.setText("Question " + (index + 1) + " of " + questionBank.size());
        questionTextLabel.setText("<html><body style='width: 480px;'>" + (index + 1) + ". " + q.getQuestionText() + "</body></html>");

        optionsGroup.clearSelection();
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText(q.getOptions()[i]);
        }

        // Initialize Timer
        secondsRemaining = TIME_PER_QUESTION;
        timerProgressBar.setMaximum(TIME_PER_QUESTION);
        timerProgressBar.setValue(TIME_PER_QUESTION);
        timerLabel.setText("Time: " + secondsRemaining + "s");

        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                secondsRemaining--;
                timerProgressBar.setValue(secondsRemaining);
                timerLabel.setText("Time: " + secondsRemaining + "s");

                if (secondsRemaining <= 5) {
                    timerLabel.setForeground(COLOR_RED);
                } else {
                    timerLabel.setForeground(TEXT_MUTED);
                }

                if (secondsRemaining <= 0) {
                    timer.stop();
                    JOptionPane.showMessageDialog(QuizApplication.this, "Time's up for this question!", "Timeout", JOptionPane.WARNING_MESSAGE);
                    processAnswer(true);
                }
            }
        });
        timer.start();
    }

    private void processAnswer(boolean timedOut) {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        int selectedIdx = -1;
        if (!timedOut) {
            for (int i = 0; i < 4; i++) {
                if (optionButtons[i].isSelected()) {
                    selectedIdx = i;
                    break;
                }
            }
            if (selectedIdx == -1) {
                JOptionPane.showMessageDialog(this, "Please select an option before submitting.", "Option Required", JOptionPane.WARNING_MESSAGE);
                timer.start();
                return;
            }
        }

        Question currentQ = questionBank.get(currentQuestionIndex);
        QuizRecord record = new QuizRecord(currentQ, selectedIdx);
        userRecords.add(record);

        if (record.isCorrect()) {
            score++;
        }

        currentQuestionIndex++;
        displayQuestion(currentQuestionIndex);
    }

    private void showFinalResults() {
        cardLayout.show(containerPanel, "RESULT");
        finalScoreBadge.setText("You scored " + score + " out of " + questionBank.size() + " (" + ((score * 100) / questionBank.size()) + "%)");

        summaryListPanel.removeAll();

        for (int i = 0; i < userRecords.size(); i++) {
            QuizRecord rec = userRecords.get(i);
            JPanel row = new JPanel(new BorderLayout(8, 4));
            row.setBackground(CARD_BG);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(rec.isCorrect() ? COLOR_GREEN : COLOR_RED, 1, true),
                    new EmptyBorder(10, 12, 10, 12)
            ));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

            JLabel qTitle = new JLabel("Q" + (i + 1) + ": " + rec.question.getQuestionText());
            qTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
            qTitle.setForeground(TEXT_WHITE);

            String userAns = rec.selectedIndex == -1 ? "Timed Out" : rec.question.getOptions()[rec.selectedIndex];
            String correctAns = rec.question.getOptions()[rec.question.getCorrectOptionIndex()];

            JLabel feedback = new JLabel("<html>Your Answer: <b>" + userAns + "</b> | Correct: <b style='color:#38B2AC;'>" + correctAns + "</b></html>");
            feedback.setFont(new Font("SansSerif", Font.PLAIN, 11));
            feedback.setForeground(rec.isCorrect() ? COLOR_GREEN : COLOR_RED);

            row.add(qTitle, BorderLayout.NORTH);
            row.add(feedback, BorderLayout.SOUTH);

            summaryListPanel.add(row);
            summaryListPanel.add(Box.createVerticalStrut(8));
        }

        summaryListPanel.revalidate();
        summaryListPanel.repaint();
    }

    private void restartQuiz() {
        score = 0;
        currentQuestionIndex = 0;
        userRecords.clear();
        cardLayout.show(containerPanel, "QUIZ");
        displayQuestion(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QuizApplication quiz = new QuizApplication();
            quiz.setVisible(true);
        });
    }
}