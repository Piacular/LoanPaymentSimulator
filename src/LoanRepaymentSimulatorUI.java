import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;

public class LoanRepaymentSimulatorUI {

    static class Loan {
        BigDecimal balance;
        final BigDecimal rate;

        Loan(BigDecimal balance, BigDecimal rate) {
            this.balance = balance;
            this.rate = rate;
        }
    }

    static class Result {
        int months;
        BigDecimal totalInterest;
        List<String> output;

        Result(int months, BigDecimal totalInterest, List<String> output) {
            this.months = months;
            this.totalInterest = totalInterest;
            this.output = output;
        }
    }

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    public static void main(String[] args) {
        JFrame frame = new JFrame("Loan Repayment Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(8, 2));

        JTextField balanceA = new JTextField("500");
        JTextField balanceB = new JTextField("750");
        JTextField balanceC = new JTextField("1000");
        JTextField rateA = new JTextField("0.025");
        JTextField rateB = new JTextField("0.05");
        JTextField rateC = new JTextField("0.075");
        JTextField minPayment = new JTextField("5");
        JTextField monthlyBudget = new JTextField("100");

        inputPanel.add(new JLabel("Loan A Balance:")); inputPanel.add(balanceA);
        inputPanel.add(new JLabel("Loan B Balance:")); inputPanel.add(balanceB);
        inputPanel.add(new JLabel("Loan C Balance:")); inputPanel.add(balanceC);
        inputPanel.add(new JLabel("Loan A Interest Rate (monthly):")); inputPanel.add(rateA);
        inputPanel.add(new JLabel("Loan B Interest Rate (monthly):")); inputPanel.add(rateB);
        inputPanel.add(new JLabel("Loan C Interest Rate (monthly):")); inputPanel.add(rateC);
        inputPanel.add(new JLabel("Minimum Payment (all loans):")); inputPanel.add(minPayment);
        inputPanel.add(new JLabel("Total Monthly Budget:")); inputPanel.add(monthlyBudget);

        JButton runButton = new JButton("Run Simulation");

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(runButton, BorderLayout.SOUTH);
        frame.setVisible(true);

        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Map<String, Loan> loans = new HashMap<>();
                loans.put("A", new Loan(new BigDecimal(balanceA.getText()), new BigDecimal(rateA.getText())));
                loans.put("B", new Loan(new BigDecimal(balanceB.getText()), new BigDecimal(rateB.getText())));
                loans.put("C", new Loan(new BigDecimal(balanceC.getText()), new BigDecimal(rateC.getText())));

                BigDecimal minPay = new BigDecimal(minPayment.getText());
                BigDecimal budget = new BigDecimal(monthlyBudget.getText());

                JFrame outputFrame = new JFrame("Simulation Progress");
                outputFrame.setSize(800, 600);
                JTextArea outputArea = new JTextArea();
                outputArea.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(outputArea);
                outputFrame.add(scrollPane);
                outputFrame.setVisible(true);

                Thread simulationThread = new Thread(() -> {
                    Result snowball = simulateRepayment(Arrays.asList("A", "B", "C"), loans, minPay, budget, outputArea, "Snowball");
                    Result avalanche = simulateRepayment(Arrays.asList("C", "B", "A"), loans, minPay, budget, outputArea, "Avalanche");

                    SwingUtilities.invokeLater(() -> {
                        outputArea.append("\n\nFinal Results:\n");
                        outputArea.append("Snowball Method: " + snowball.months + " months, Total Interest: $" + snowball.totalInterest.setScale(2, RoundingMode.HALF_UP) + "\n");
                        outputArea.append("Avalanche Method: " + avalanche.months + " months, Total Interest: $" + avalanche.totalInterest.setScale(2, RoundingMode.HALF_UP) + "\n");
                    });
                });

                simulationThread.start();
            }
        });
    }

    public static Result simulateRepayment(List<String> order, Map<String, Loan> loanTemplate, BigDecimal minPayment, BigDecimal budget, JTextArea outputArea, String label) {
        Map<String, Loan> balances = new HashMap<>();
        for (Map.Entry<String, Loan> entry : loanTemplate.entrySet()) {
            balances.put(entry.getKey(), new Loan(entry.getValue().balance, entry.getValue().rate));
        }

        Map<String, BigDecimal> interestPaid = new HashMap<>();
        for (String k : balances.keySet()) interestPaid.put(k, BigDecimal.ZERO);

        List<String> output = new ArrayList<>();
        int months = 0;

        while (balances.values().stream().anyMatch(l -> l.balance.compareTo(BigDecimal.ZERO) > 0)) {
            months++;
            BigDecimal totalPayment = budget;

            Map<String, BigDecimal> monthlyInterest = new HashMap<>();
            Map<String, BigDecimal> monthlyPayment = new HashMap<>();

            for (String k : balances.keySet()) {
                Loan loan = balances.get(k);
                if (loan.balance.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal interest = loan.balance.multiply(loan.rate, MC);
                    loan.balance = loan.balance.add(interest, MC);
                    interestPaid.put(k, interestPaid.get(k).add(interest, MC));
                    monthlyInterest.put(k, interest);
                } else {
                    monthlyInterest.put(k, BigDecimal.ZERO);
                }
            }

            for (String k : balances.keySet()) {
                Loan loan = balances.get(k);
                if (loan.balance.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal payment = loan.balance.min(minPayment);
                    loan.balance = loan.balance.subtract(payment, MC);
                    totalPayment = totalPayment.subtract(payment, MC);
                    monthlyPayment.put(k, payment);
                } else {
                    monthlyPayment.put(k, BigDecimal.ZERO);
                }
            }

            for (String k : order) {
                Loan loan = balances.get(k);
                if (loan.balance.compareTo(BigDecimal.ZERO) > 0 && totalPayment.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal payment = loan.balance.min(totalPayment);
                    loan.balance = loan.balance.subtract(payment, MC);
                    totalPayment = totalPayment.subtract(payment, MC);
                    monthlyPayment.put(k, monthlyPayment.get(k).add(payment, MC));
                }
            }

            String line = String.format("%s - Month %d:\nBalances - A: $%.2f | B: $%.2f | C: $%.2f\nInterest - A: $%.2f | B: $%.2f | C: $%.2f\nPayments - A: $%.2f | B: $%.2f | C: $%.2f\n\n",
                    label, months,
                    balances.get("A").balance, balances.get("B").balance, balances.get("C").balance,
                    monthlyInterest.get("A"), monthlyInterest.get("B"), monthlyInterest.get("C"),
                    monthlyPayment.get("A"), monthlyPayment.get("B"), monthlyPayment.get("C"));

            output.add(line);
            SwingUtilities.invokeLater(() -> outputArea.append(line));

            try {
                Thread.sleep(100); // brief pause for visualization
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        BigDecimal totalInterest = interestPaid.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Result(months, totalInterest, output);
    }
}