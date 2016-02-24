package com.moneydance.modules.features.mynetworth;

import com.moneydance.awt.*;
import com.infinitekind.moneydance.model.*;

import java.io.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    java.util.List<MyAccount> allAccounts;
    long lastTransactionsSize;

    static private boolean debug1 = true;
    static private boolean debug2 = false;

    static StringBuffer myPrintBuf = new StringBuffer();

    static void myPrint(String format, Object... args)
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        // myPrintBuf.append(timeStamp + " " + String.format(format, args)).append("\n");
        System.err.println(timeStamp + " " + String.format(format, args));
    }

    static void myPrintDone() {
        if (myPrintBuf.length() > 0)
        {
            System.err.print(myPrintBuf.toString());
            myPrintBuf = new StringBuffer();
        }
    }

    static void myDebug1(String format, Object... args)
    {
        if (debug1)  myPrint(format, args);
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
        DefaultTableModel recursiveBalances;

        AccountsData() {
             clear();
        }

        void clear() {
            myAccounts = new ArrayList<MyAccount>();
            dates = new ArrayList<Integer>();
            balances = new DefaultTableModel();
            recursiveBalances = new DefaultTableModel();
        }
    }

    private boolean isMainAccount(Account a) {
        Account.AccountType	type = a.getAccountType();
        return (
                type == Account.AccountType.ASSET
                || type == Account.AccountType.BANK
                || type == Account.AccountType.CREDIT_CARD
                || type == Account.AccountType.INVESTMENT
                || type == Account.AccountType.LIABILITY
                || type == Account.AccountType.LOAN
                || type == Account.AccountType.SECURITY
                );
    }

    private static void sortTransactions(TxnSet txnSet) {

        AccountUtil.sortTransactions(txnSet, AccountUtil.DATE);
    }
    private class MyAccount {
        private Account account;
        private TxnSet txnSet = null;
        private int lastIndex;
        long lastBalance;
        private java.util.List<MyAccount> subAccounts = new ArrayList<MyAccount>();

        public MyAccount(Account account) {
            this.account = account;
            lastBalance = account.getStartBalance();
            lastIndex = -1;
            // getting transactions takes long time. Try to optimize by preparing maps of account -> transactions
            txnSet = extension.getUnprotectedContext().getCurrentAccountBook().
                    getTransactionSet().getTransactionsForAccount(account).cloneTxns();
            sortTransactions(txnSet);
            for (Account sa : account.getSubAccounts())
            {
                if (isMainAccount(sa))
                    subAccounts.add(new MyAccount(sa));
            }
        }

        public String getAccountName() {
            return account.getAccountName();
        }

        public int getDepth() {
            return account.getDepth();
        }

        public String getParentName() {
            try {
                return account.getParentAccount().getAccountName();
            }
            catch(Exception e) {
                return "unknown";
            }
        }

        public Account getAccount() {
            return account;
        }

        public String getAccountTypeName() {
            Account.AccountType	type = account.getAccountType();
            try {
                return type.name();
            } catch (Exception e) {
                return "unknown";
            }
        }

        private long getBalanceForSecurity(int date) {
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

            myDebug2("positions %d rate %f div %f long div %d round div %d mismatch: %s", positions, rate, positions/rate,
                    (long)(positions/rate), Math.round(positions/rate), (Math.round(positions/rate) != (long)(positions/rate) ? "yes" : "no") );
            long balance = Math.round(positions/rate);
            balance += getAccount().getStartBalance();
            long startBalance = getAccount().getStartBalance();
            balance += startBalance;
            if ( startBalance != 0) {
                myPrint("start balance is not zero: ");
            }
            // myDebug1("account on %d positions %d rate %f value %d", date, positions, rate, balance);
            return balance;
        }

        public long getBalance(int date) {
            // myDebug1("entered getBalance account %s date %d", getAccountName() ,date);
            if (account.getAccountType() == Account.AccountType.SECURITY) {
                return getBalanceForSecurity(date);
            }

            if (lastIndex >=0 && txnSet.getTxn(lastIndex).getDateInt() > date )
            {
                myDebug1("restarting dates");
                lastIndex = -1;
                lastBalance = account.getStartBalance();
            }

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

        public long getRecursiveBalance(int date) {
            if (account.getAccountType() == Account.AccountType.SECURITY) {
                return getBalanceForSecurity(date);
            }

            long balance = getBalance(date);
            for (MyAccount subAccount : subAccounts) {
                long subBalance = subAccount.getRecursiveBalance(date);
                balance += subBalance;
                myDebug2("%s added sub account %s sub balance %d new total balance %d",
                        account.getAccountName(), subAccount.getAccountName(), subBalance, balance);
            }
            myDebug2("returning balance with sub accounts = %d", balance);
            return balance;
        }
    }

    public java.util.List<MyAccount> getAllAccounts() {
        boolean refresh = false;
        if (allAccounts == null)
        {
            refresh = true;
            lastTransactionsSize = extension.getUnprotectedContext().getCurrentAccountBook().getTransactionSet().getTransactionCount();
        }
        else
        {
            long size = extension.getUnprotectedContext().getCurrentAccountBook().getTransactionSet().getTransactionCount();
            if (size != lastTransactionsSize)
            {
                refresh = true;
                lastTransactionsSize = size;
            }
        }
        if (refresh)
            allAccounts =  getAccounts(extension.getUnprotectedContext().getRootAccount());
        return allAccounts;
    }

    public java.util.List<MyAccount> getAccounts(Account topAccount) {
        java.util.List<MyAccount> resultAccounts = new ArrayList<MyAccount>();
        resultAccounts.add(new MyAccount(topAccount));
        for (int i = 0; i < topAccount.getSubAccountCount(); i++) {
            Account acct = topAccount.getSubAccount(i);
            if (isMainAccount(acct)) {

                resultAccounts.addAll(getAccounts(acct));
            }
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

    private void displayData() {
        myDebug1("entered displayDate()");

        DefaultTableModel tableModel = (DefaultTableModel)dataTable.getModel();
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        boolean includeSubs = subCheckBox.isSelected();

        Vector<String> vh = new Vector<String>();
        vh.add("Account Name");
        vh.add("Account Type");
        int freezeColumns = 2;
        if (includeSubs)
        {
            vh.add("Parent");
            freezeColumns = 3;
        }
        for (int date: displayedData.dates) {
            vh.add(MyUtils.dateIntToString(date));
        }
        for(String name : vh) {
            tableModel.addColumn(name);
        }
        if (headersCheckBox.isSelected())
            tableModel.addRow(vh);

        d_numberRenderer.setShowCents(centsCheckBox.isSelected());
        for(int i=0;i< tableModel.getColumnCount();++i) {
            if (i>=freezeColumns)
            {
                d_numberRenderer.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
                dataTable.getColumnModel().getColumn(i).setCellRenderer(d_numberRenderer);
                dataTable.getColumnModel().getColumn(i).setMinWidth(100);
            }
        }
        dataTable.getColumnModel().getColumn(0).setMinWidth(150);
        dataTable.getColumnModel().getColumn(1).setMinWidth(80);
        if (includeSubs)
            dataTable.getColumnModel().getColumn(2).setMinWidth(80);
        Vector<Long> totals = new Vector<Long>();
        for (Integer ignored : displayedData.dates) totals.add(0L);
        for(int i=0; i<displayedData.myAccounts.size(); ++i) {
            MyAccount accnt = displayedData.myAccounts.get(i);
            if (includeSubs && accnt.getDepth()==2 || accnt.getDepth()==1) {
                java.util.List<Long> rowBalances = new ArrayList<Long>();
                boolean hasBalance = false;
                for (int j = 0; j < displayedData.dates.size(); ++j) {
                    // Long balance = (Long) displayedData.balances.getValueAt(i, j);
                    Long balance;
                    int date = displayedData.dates.get(j);
                    if (includeSubs)
                    {
                        balance = (Long) displayedData.balances.getValueAt(i, j);
                    }
                    else
                    {
                        balance = (Long) displayedData.recursiveBalances.getValueAt(i, j);
                    }
                    rowBalances.add(balance);
                    if (balance != 0)
                        hasBalance = true;
                }
                if (hasBalance) {
                    if (diffsCheckBox.isSelected()) {
                        for (int k = rowBalances.size() - 1; k > 0; --k) {
                            rowBalances.set(k, rowBalances.get(k) - rowBalances.get(k - 1));
                        }
                    }
                    Vector<Object> v = new Vector<Object>();
                    v.add(accnt.getAccountName());
                    v.add(accnt.getAccountTypeName());
                    if (includeSubs)
                        v.add(accnt.getParentName());
                    for (int k = 0; k < rowBalances.size(); ++k) {
                        long balance = rowBalances.get(k);
                        totals.set(k, totals.get(k) + balance);
                        v.add((double) balance / 100);
                    }
                    tableModel.addRow(v);
                }
            }
        }
        Vector<Object> hv = new Vector<Object>();
        hv.add("total");
        hv.add("TOTAL");
        if (includeSubs)
            hv.add("");
        for (long total: totals) {
            hv.add((double)total/100);
        }
        tableModel.addRow(hv);
    }

    public  void getBalances(AccountsData accountsData) {
        DefaultTableModel balances = accountsData.balances;
        DefaultTableModel recursiveBalances = accountsData.recursiveBalances;
        java.util.List<Integer> dates = accountsData.dates;
        java.util.List<MyAccount> accounts = accountsData.myAccounts;
        balances.setRowCount(0);
        balances.setColumnCount(dates.size());
        recursiveBalances.setRowCount(0);
        recursiveBalances.setColumnCount(dates.size());
        for (MyAccount account : accounts) {
            Vector<Long> rowBalances = new Vector<Long>();
            Vector<Long> rowRecursiveBalances = new Vector<Long>();
            for (Integer date : dates) {
                rowBalances.add(account.getBalance(date));
                rowRecursiveBalances.add(account.getRecursiveBalance(date));
            }
            balances.addRow(rowBalances);
            recursiveBalances.addRow(rowRecursiveBalances);
        }
    }

    ItemListener itemsListener = new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            myDebug1("entered listener of " + e.toString());
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
                        myDebug1("entered actionListener of generate button");
                        dm.setColumnCount(0);
                        dm.setRowCount(0);
                        try {
                            topContainer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            displayedData.clear();
                            displayedData.dates = periodSelector.getDates();
                            displayedData.myAccounts = getAllAccounts();
                            myDebug1("calling displayedDate");
                            getBalances(displayedData);
                            myDebug1("after calling displayedDate");
                            displayData();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        finally {
                            topContainer.setCursor(Cursor.getDefaultCursor());
                            myDebug1("exiting actionListener of generate button");
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
        subCheckBox.addItemListener(itemsListener);
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