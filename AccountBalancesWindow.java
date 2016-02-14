package com.moneydance.modules.features.mynetworth;

/**
 * User: gad
 * Date: 1/27/13
 * Time: 1:55 PM
 */
import com.moneydance.awt.*;
import com.moneydance.apps.md.model.*;

import java.io.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;

public class AccountBalancesWindow
        extends JFrame
        implements ActionListener
{
    private Main extension;
    private JTextArea mainListArea;
    JCheckBox diffsCheckBox;
    JCheckBox subCheckBox;
    JCheckBox centsCheckBox;
    JCheckBox headersCheckBox;

    JTable dataTable;
    final DefaultTableModel dm;

    AccountsData displayedData = new AccountsData();

    static private boolean debug = false;
    static private boolean debug2 = false;

    static StringBuffer myPrintBuf = new StringBuffer();

    static void myPrint(String format, Object... args)
    {
//        myPrintBuf.append(String.format(format, args)).append("\n");
        System.err.println(String.format(format, args));
    }

    static void myPrintDone() {
        if (myPrintBuf.length() > 0)
            System.err.print(myPrintBuf.toString());
    }

    static void myDebug(String format, Object... args)
    {
        if (debug)  myPrint(format, args);
    }

    static void myDebug2(String format, Object... args)
    {
        if (debug2)  myPrint(format, args);
    }

    static String formatAmount(long amount) {
        return String.format("%.2f",(float)amount/100);
    }

    static String getString(Calendar cal)
    {
        return String.format("calendar: %d %d %d weekday %d",
                cal.get(cal.YEAR),
                cal.get(cal.MONTH)+1,
                cal.get(cal.DAY_OF_MONTH),
                cal.get(cal.DAY_OF_WEEK)
        );
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    class AccountsData {
        java.util.List<MyAccount> myAccounts;
        java.util.List<Integer> dates;
        DefaultTableModel balances;

        AccountsData() {
             clear();
        }

        void clear() {
            myAccounts = new ArrayList<MyAccount>();
            dates = new ArrayList<Integer>();
            balances = new DefaultTableModel();
        }
    }

    private static boolean isMainAccount(Account a) {
        MyAccount mya = new MyAccount(a);
        return ((a instanceof AssetAccount)
                || (a instanceof BankAccount)
                || (a instanceof CreditCardAccount)
                || (a instanceof InvestmentAccount)
                || (a instanceof LiabilityAccount)
                || (a instanceof LoanAccount)
                || (a instanceof SecurityAccount)
        );
    }

    private static void sortTransactions(TxnSet txnSet) {

        AccountUtil.sortTransactions(txnSet, AccountUtil.DATE);
    }

    private static class MyAccount {
        private Account account;
        private TxnSet txnSet = null;
        private int lastIndex;
        long lastBalance;
        private java.util.List<MyAccount> subAccounts = new ArrayList<MyAccount>();
        boolean cashOnly;

        public MyAccount(Account account, boolean cashOnly) {
            this.account = account;
            lastBalance = account.getStartBalance();
            lastIndex = -1;
            txnSet =  account.getRootAccount().getTransactionSet().getTransactionsForAccount(account).cloneTxns();
            sortTransactions(txnSet);
            if (cashOnly) {
                this.cashOnly = true;
            } else {
                for (int i = 0; i < account.getSubAccountCount(); i++) {
                    subAccounts.add(new MyAccount(account.getSubAccount(i)));
                }
            }
        }

        public MyAccount(Account account) {
            this(account, false);
        }

        public String getAccountName() {
            return account.getAccountName();
        }

        public Account getAccount() {
            return account;
        }

        public String getAccountTypeName() {
            int type = account.getAccountType();
            if (type == Account.ACCOUNT_TYPE_ROOT) return "ROOT";
            if (type == Account.ACCOUNT_TYPE_BANK) return "BANK";
            if (type == Account.ACCOUNT_TYPE_CREDIT_CARD) return "CREDIT_CARD";
            if (type == Account.ACCOUNT_TYPE_INVESTMENT) return "INVESTMENT";
            if (type == Account.ACCOUNT_TYPE_SECURITY) return "SECURITY";
            if (type == Account.ACCOUNT_TYPE_ASSET) return "ASSET";
            if (type == Account.ACCOUNT_TYPE_LIABILITY) return "LIABILITY";
            if (type == Account.ACCOUNT_TYPE_LOAN) return "LOAN";
            if (type == Account.ACCOUNT_TYPE_EXPENSE) return "EXPENSE";
            if (type == Account.ACCOUNT_TYPE_INCOME) return "INCOME";
            return "unknown";
        }

        private long getBalanceForSecurity(int date) {
            boolean test = true;
            long positions = 0;
            CurrencyType currencytype = account.getCurrencyType();
            for (int i=0;i<txnSet.getSize();++i) {
                AbstractTxn t = txnSet.getTxn(i);
                int txnDate = t.getDateInt();
                if (txnDate > date)
                    break;
                long adjustedVal = currencytype.adjustValueForSplitsInt(txnDate,t.getValue(),date);
                positions += adjustedVal;
            }
            double rate = currencytype.getRawRateByDateInt(date);

            myDebug2("positions %d rate %f div %f long div %d round div %d", positions, rate, positions/rate,
                    (long)(positions/rate), Math.round(positions/rate));
            if (Math.round(positions/rate) != (long)(positions/rate)) {
                myDebug2("debug: mismatch");
            }
            long balance = Math.round(positions/rate);
            balance += getAccount().getStartBalance();
            long startBalance = getAccount().getStartBalance();
            balance += startBalance;
            if ( startBalance != 0) {
                myPrint("start balance is not zero: ");
            }
            myDebug("account on %d positions %d rate %f value %d", date, positions, rate, balance);
            return balance;
        }

        public long getCashBalance(int date) {
            if (lastIndex >=0 && txnSet.getTxn(lastIndex).getDateInt() > date )
            {
                lastIndex = -1;
                lastBalance = account.getStartBalance();
            }
            myDebug("inital last index %d last balance %d", lastIndex, lastBalance);

            for (int i = lastIndex; i<txnSet.getSize(); ++i) {
                if (i+1 == txnSet.getSize() || txnSet.getTxn(i+1).getDateInt() > date) {
                    break;
                }

                ++lastIndex;
                AbstractTxn txn = txnSet.getTxn(lastIndex);
                lastBalance += txn.getValue();
            }
            myDebug2("out of loop top balance=%d", lastBalance);
            return lastBalance;
        }

        public long getBalance(int date) {
            if (account.getAccountType() == Account.ACCOUNT_TYPE_SECURITY) {
                return getBalanceForSecurity(date);
            }

            long balance = getCashBalance(date);
            for (int i=0; i<subAccounts.size(); ++i) {
                long subBalance = subAccounts.get(i).getBalance(date);
                balance += subBalance;
                myDebug2("%s added sub account %s sub balance %d new total balance %d",
                        account.getAccountName(), subAccounts.get(i).getAccountName(), subBalance, balance);
            }
            myDebug2("returning balance with sub accounts =%d", balance);
            return balance;
        }
    }

    public java.util.List<MyAccount> getAccounts(int level)
    {
           return getAccounts(level, extension.getUnprotectedContext().getRootAccount());
    }

    public java.util.List<MyAccount> getAccounts(int level, Account parentAccount) {
        java.util.List<MyAccount> resultAccounts = new ArrayList<MyAccount>();

        if (parentAccount.getSubAccountCount() == 0) {
            if (isMainAccount(parentAccount)) {
                resultAccounts.add(new MyAccount(parentAccount));
            }
        } else {
            for (int i = 0; i < parentAccount.getSubAccountCount(); i++) {
                Account acct = parentAccount.getSubAccount(i);
                if (isMainAccount(acct)) {
                    if (level == 1) {
                        resultAccounts.add(new MyAccount(acct));
                    }
                    else
                    {
                        assert level > 1;
                        resultAccounts.addAll(getAccounts(level-1, acct));

                    }
                }
            }
            resultAccounts.add(new MyAccount(parentAccount,true));
        }
        return resultAccounts;
    }

    static class NumberRenderer extends DefaultTableCellRenderer {
        private DecimalFormat formatterWithCents = new DecimalFormat("###,##0.00");
        private DecimalFormat formatterNoCents = new DecimalFormat("###,##0");
        private boolean showCents;

        public NumberRenderer() { super(); }

        public void setShowCents(boolean val) {
            this.showCents = val;
        }

        public void setValue(Object value) {
            try {
                if (showCents)
                    setText(formatterWithCents.format(value));
                else
                    setText(formatterNoCents.format(value));

            } catch (Exception e) {
                e.printStackTrace();
                setText((value==null) ? "" : value.toString());
            }
        }
    }

    public NumberRenderer d_numberRenderer = new NumberRenderer();

    public void displayData() {

        DefaultTableModel dm = (DefaultTableModel)dataTable.getModel();
        dm.setRowCount(0);
        dm.setColumnCount(0);

        boolean subs = subCheckBox.isSelected();

        Vector<String> vh = new Vector<String>();
        vh.add("Account Name");
        vh.add("Account Type");
        int freezeColumns = 2;
        if (subs)
        {
            vh.add("Parent");
            freezeColumns = 3;
        }
        for (int date: displayedData.dates) {
            vh.add(MyUtils.dateIntToString(date));
        }
        for(String name : vh) {
            dm.addColumn(name);
        }
        if (headersCheckBox.isSelected())
            dm.addRow(vh);

        d_numberRenderer.setShowCents(centsCheckBox.isSelected());
        for(int i=0;i<dm.getColumnCount();++i) {
            if (i>=freezeColumns)
            {
                d_numberRenderer.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
                dataTable.getColumnModel().getColumn(i).setCellRenderer(d_numberRenderer);
                dataTable.getColumnModel().getColumn(i).setMinWidth(100);
            }
        }
        dataTable.getColumnModel().getColumn(0).setMinWidth(150);
        dataTable.getColumnModel().getColumn(1).setMinWidth(80);
        if (subs)
            dataTable.getColumnModel().getColumn(2).setMinWidth(80);
        Vector<Long> totals = new Vector<Long>();
        for (int i=0; i<displayedData.dates.size();++i)
            totals.add(0L);
        for(int i=0; i<displayedData.myAccounts.size(); ++i) {
            MyAccount accnt = displayedData.myAccounts.get(i);
            java.util.List<Long> rowBalances = new ArrayList<Long>();
            boolean hasBalance = false;
            for (int j=0; j<displayedData.dates.size(); ++j) {
                Long balance = (Long)displayedData.balances.getValueAt(i,j);
                rowBalances.add(balance);
                if (balance != 0)
                    hasBalance = true;
            }
            if (hasBalance) {
                if (diffsCheckBox.isSelected()) {
                    for (int k=rowBalances.size()-1; k>0; --k) {
                        rowBalances.set(k, rowBalances.get(k) - rowBalances.get(k-1));
                    }
                }
                Vector<Object> v = new Vector<Object>();
                v.add(accnt.getAccountName());
                v.add(accnt.getAccountTypeName());
                if (subs)
                    v.add(accnt.getAccount().getParentAccount().getAccountName());
                for(int k=0; k<rowBalances.size(); ++k) {
                    long balance = rowBalances.get(k);
                    totals.set(k, totals.get(k) + balance);
                    v.add((double)balance/100);
                }
                dm.addRow(v);
            }
        }
        Vector<Object> hv = new Vector<Object>();
        hv.add("total");
        hv.add("TOTAL");
        if (subs)
            hv.add("");
        for (long total: totals) {
            hv.add((double)total/100);
        }
        dm.addRow(hv);
    }

    public static void getBalances(AccountsData accountsData) {
        DefaultTableModel result = accountsData.balances;
        java.util.List<Integer> dates = accountsData.dates;
        java.util.List<MyAccount> accounts = accountsData.myAccounts;
        result.setRowCount(0);
        result.setColumnCount(dates.size());

        for(MyAccount account: accounts) {
            Vector<Long> rowBalances = new Vector<Long>();
            for(Integer date : dates) {
                rowBalances.add(account.getBalance(date));
            }
            result.addRow(rowBalances);
        }
    }

    ItemListener itemsListener = new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            displayData();
        }
    };

    public AccountBalancesWindow(final Main extension) {
        super("Balances by Dates");
        this.extension = extension;

        final Container topContainer = getContentPane();
        topContainer.setLayout(new BorderLayout());

        final JPanel topRow = new JPanel();
        topRow.setLayout(new BoxLayout(topRow, BoxLayout.LINE_AXIS));
        topContainer.add(topRow, BorderLayout.PAGE_START);

        dm = new DefaultTableModel();
        dataTable = new JTable(dm);
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(dataTable);
        topContainer.add(scrollPane, BorderLayout.CENTER);
        topContainer.add(scrollPane, BorderLayout.CENTER);

        final PeriodSelector periodSelector = new PeriodSelector();

        JButton generateButton = new JButton("Generate");
        topRow.add(generateButton);
        generateButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        dm.setColumnCount(0);
                        dm.setRowCount(0);
                        try {
                            displayedData.clear();
                            displayedData.dates = periodSelector.getDates();
                            displayedData.myAccounts = getAccounts(subCheckBox.isSelected() ? 2 : 1);
                            getBalances(displayedData);
                            displayData();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        topRow.add(periodSelector);

        diffsCheckBox = new JCheckBox("diffs");
        diffsCheckBox.setSelected(true);
        diffsCheckBox.addItemListener(itemsListener);
        diffsCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        topRow.add(diffsCheckBox);

        subCheckBox = new JCheckBox("subs");
        subCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        topRow.add(subCheckBox);

        centsCheckBox = new JCheckBox("cents");
        centsCheckBox.addItemListener(itemsListener);
        centsCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        topRow.add(centsCheckBox);

        headersCheckBox = new JCheckBox("headers");
        headersCheckBox.addItemListener(itemsListener);
        headersCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        topRow.add(headersCheckBox);

        JButton helpButton = new JButton("Help");
        topRow.add(helpButton);
        helpButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        JOptionPane.showMessageDialog(topContainer, "Version 1.0\nFor questions or suggestions please contact ggruenstein@gmail.com");
                    }
                }
        );

        topRow.add(Box.createHorizontalGlue());

        setSize(1200,800);
        AwtUtil.centerWindow(this);
    }

    public final void processEvent(AWTEvent evt) {
        if(evt.getID()==WindowEvent.WINDOW_CLOSING) {
            extension.closeConsole();
            return;
        }
        if(evt.getID()==WindowEvent.WINDOW_OPENED) {
        }
        super.processEvent(evt);
    }

    private class ConsoleStream
            extends OutputStream
            implements Runnable
    {
        public void write(int b)
                throws IOException
        {
            mainListArea.append(String.valueOf((char) b));
            repaint();
        }

        public void write(byte[] b)
                throws IOException
        {
            mainListArea.append(new String(b));
            repaint();
        }
        public void run() {
            mainListArea.repaint();
        }
    }

    void goAway() {
        setVisible(false);
        dispose();
    }
}