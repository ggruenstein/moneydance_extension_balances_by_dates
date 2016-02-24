package com.moneydance.modules.features.mynetworth;

import com.moneydance.awt.DateField;
import com.moneydance.awt.JDateField;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gad
 * Date: 2/8/13
 * Time: 7:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeriodSelector extends JPanel {

    JComboBox dateRangeCombo;
    JComboBox groupByCombo;
    JTextField startDateText = new JTextField();
    JTextField endDateText = new JTextField();
    // TextField startDateText = new DateField(new java.text.SimpleDateFormat());
    // TextField endDateText = new DateField(new java.text.SimpleDateFormat());

    java.util.List<Integer> getDates() throws ParseException {
        java.util.List<Integer> dates = new ArrayList<Integer>();
        Calendar start = stringToCalendar(startDateText.getText());
        Calendar end = stringToCalendar(endDateText.getText());

        if (start.before(end))
            dates.add(MyUtils.dateIntFromCalendar(start));

        String group = (String)groupByCombo.getSelectedItem();
        if (group.equals("No Grouping")) {
            dates.add(MyUtils.dateIntFromCalendar(end));
        }
        else if (group.equals("Day")) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start.getTime());
            cal.add(cal.DAY_OF_MONTH,1);
            while(cal.compareTo(end) < 0) {
                dates.add(MyUtils.dateIntFromCalendar(cal));
                cal.add(cal.DAY_OF_MONTH,1);
            }
            dates.add(MyUtils.dateIntFromCalendar(end));
        }
        else if (group.equals("Week")) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start.getTime());
            cal.set(cal.DAY_OF_WEEK, cal.MONDAY);
            if (cal.compareTo(start) <= 0)
                cal.add(cal.DAY_OF_MONTH,7);
            while(cal.compareTo(end) < 0) {
                dates.add(MyUtils.dateIntFromCalendar(cal));
                cal.add(cal.WEEK_OF_YEAR,1);
            }
            dates.add(MyUtils.dateIntFromCalendar(end));
        }
        else if (group.equals("Month")) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start.getTime());
            cal.add(cal.MONTH,1);
            cal.set(cal.DAY_OF_MONTH, 1);
            while(cal.compareTo(end) < 0) {
                dates.add(MyUtils.dateIntFromCalendar(cal));
                cal.add(cal.MONTH,1);
            }
            dates.add(MyUtils.dateIntFromCalendar(end));
        }
        else if (group.equals("Quarter")) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start.getTime());
            cal.add(cal.MONTH, 2 - (cal.get(cal.MONTH) % 3));
            setLastDayOfMonth(cal);
            while(cal.compareTo(end) < 0) {
                if (!cal.equals(start))
                    dates.add(MyUtils.dateIntFromCalendar(cal));
                cal.add(cal.MONTH, 3);
                setLastDayOfMonth(cal);
            }
            dates.add(MyUtils.dateIntFromCalendar(end));
        }
        else if (group.equals("Year")) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start.getTime());
            cal.add(cal.YEAR,1);
            cal.set(cal.MONTH, 0);
            cal.set(cal.DAY_OF_MONTH, 1);
            while(cal.compareTo(end) < 0) {
                dates.add(MyUtils.dateIntFromCalendar(cal));
                cal.add(cal.YEAR,1);
            }
            dates.add(MyUtils.dateIntFromCalendar(end));
        }
        return dates;
    }

    static void freezeSize(Component c) {
        c.setMaximumSize(c.getPreferredSize());
//        javax.swing.border.Border blackline = BorderFactory.createLineBorder(Color.black);
//        c.setBorder(blackline);
    }

    static void setLastDayOfMonth(Calendar cal) {
        cal.set(cal.DAY_OF_MONTH,cal.getActualMaximum(cal.DAY_OF_MONTH));
    }

    void addSeparator()
    {
        add(Box.createRigidArea(new Dimension(5,0)));
    }

    static Calendar stringToCalendar(String s) throws ParseException {
        // java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("MM/dd/yyyy");
        DateFormat f = DateFormat.getDateInstance(DateFormat.SHORT);
        f.setLenient(true);
        Calendar cal = Calendar.getInstance();
        Date date = f.parse(s);
        cal.setTime(date);
        return cal;
    }

    ItemListener dataRangeComboListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == e.SELECTED) {
                String value = (String)e.getItem();
                if (value.equals("Year to date")) {
                    Calendar cal = Calendar.getInstance();
                    endDateText.setText(MyUtils.calToString(cal));
                    cal.set(cal.get(cal.YEAR),0,1);
                    startDateText.setText(MyUtils.calToString(cal));
                }
                else if (value.equals("12 month to date")) {
                    Calendar cal = Calendar.getInstance();
                    endDateText.setText(MyUtils.calToString(cal));
                    cal.add(cal.MONTH, -12);
                    cal.set(cal.DAY_OF_MONTH,1);
                    startDateText.setText(MyUtils.calToString(cal));
                }
                else if (value.equals("3 month to date")) {
                    Calendar cal = Calendar.getInstance();
                    endDateText.setText(MyUtils.calToString(cal));
                    cal.add(cal.MONTH, -3);
                    cal.set(cal.DAY_OF_MONTH,1);
                    startDateText.setText(MyUtils.calToString(cal));
                }
                else if (value.equals("Month to date")) {
                    Calendar cal = Calendar.getInstance();
                    endDateText.setText(MyUtils.calToString(cal));
                    cal.set(cal.DAY_OF_MONTH,1);
                    startDateText.setText(MyUtils.calToString(cal));
                }
                else if (value.equals("Week to date")) {
                    Calendar cal = Calendar.getInstance();
                    endDateText.setText(MyUtils.calToString(cal));
                    Calendar cal2 = Calendar.getInstance();
                    cal2.setTime(cal.getTime());
                    cal2.set(cal2.DAY_OF_WEEK, cal.MONDAY);
                    if (cal2.after(cal) || cal2.equals(cal)) {
                        cal2.add(cal2.DAY_OF_WEEK,-7);
                    }
                    startDateText.setText(MyUtils.calToString(cal2));
                }
                else if (value.equals("7 days to date")) {
                    Calendar cal = Calendar.getInstance();
                    endDateText.setText(MyUtils.calToString(cal));
                    cal.add(cal.DAY_OF_MONTH, -7);
                    startDateText.setText(MyUtils.calToString(cal));
                }
                else if (value.equals("day")) {
                    Calendar cal = Calendar.getInstance();
                    endDateText.setText(MyUtils.calToString(cal));
                    cal.add(cal.DAY_OF_MONTH, -1);
                    startDateText.setText(MyUtils.calToString(cal));
                }
                else if (value.equals("Last month")) {
                    Calendar cal = Calendar.getInstance();
                    cal.add(cal.MONTH, -1);
                    cal.set(cal.DAY_OF_MONTH, 1);
                    startDateText.setText(MyUtils.calToString(cal));
                    setLastDayOfMonth(cal);
                    endDateText.setText(MyUtils.calToString(cal));
                }
                else if (value.equals("Last year")) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(cal.MONTH,0);
                    cal.set(cal.DAY_OF_MONTH,1);
                    cal.add(cal.YEAR,-1);
                    startDateText.setText(MyUtils.calToString(cal));
                    cal.set(cal.MONTH,11);
                    setLastDayOfMonth(cal);
                    endDateText.setText(MyUtils.calToString(cal));
                }
                else if (value.equals("All dates")) {
                    Calendar cal = Calendar.getInstance();
                    endDateText.setText(MyUtils.calToString(cal));
                    cal.set(1990,0,1);
                    startDateText.setText(MyUtils.calToString(cal));
                }
                else if (value.equals("Custom dates")) {
                }
            }
        }
    };

    public PeriodSelector() {
        super.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        String[] dateRangeStrings = {
                "Year to date",
                "12 month to date",
                "3 month to date",
                "Month to date",
                "Week to date",
                "7 days to date",
                "day",
                "Last month",
                "Last year",
                "All dates",
                "Custom dates"};
        dateRangeCombo = new JComboBox(dateRangeStrings);
        dateRangeCombo.addItemListener(dataRangeComboListener);
        add(dateRangeCombo);
        freezeSize(dateRangeCombo);

        String[] groupByStrings = {"No Grouping", "Day","Week", "Month", "Quarter", "Year"};
        groupByCombo = new JComboBox(groupByStrings);
        groupByCombo.setSelectedIndex(0);
        add(groupByCombo);
        add(Box.createHorizontalBox());
        freezeSize(groupByCombo);

        add(Box.createHorizontalBox());
        JLabel startDateLabel = new JLabel("start: ");
        add(startDateLabel);
        freezeSize(startDateLabel);
        // startDateText.setColumns(10);
        startDateText.setText("22/12/2016"); // sets width
        add(startDateText);
        freezeSize(startDateText);

        addSeparator();
        JLabel endDateLabel = new JLabel("end: ");
        add(endDateLabel);
        freezeSize(endDateLabel);
        //zzz endDateText.setColumns(10);
        endDateText.setText("22/12/2016"); // sets width
        add(endDateText);
        freezeSize(endDateText);

        dateRangeCombo.setSelectedIndex(6);     // done at end to force change of dates
    }

    static class NumberRenderer extends DefaultTableCellRenderer {
        DecimalFormat formatter;
        public NumberRenderer() { super(); }

        public void setValue(Object value) {
            if (formatter==null) {
                formatter = new DecimalFormat("###,##0.00");
            }
            setText((value == null) ? "" : formatter.format(value));
        }
    }

    static public NumberRenderer d_numberRenderer = new NumberRenderer();

    private static void createAndShowGUI() {
        final JFrame frame = new JFrame("Period Selector");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel topRow = new JPanel();
        topRow.setLayout(new BoxLayout(topRow, BoxLayout.LINE_AXIS));
        Container topContainer = frame.getContentPane();
        topContainer.add(topRow, BorderLayout.PAGE_START);

        final DefaultTableModel dm = new DefaultTableModel();

        final JTable table = new JTable(dm);
        JScrollPane scrollPane = new JScrollPane(table);
        topContainer.add(scrollPane, BorderLayout.CENTER);

        final PeriodSelector periodSelector = new PeriodSelector();

        JButton generateButton = new JButton("Generate");
        topRow.add(generateButton, BorderLayout.PAGE_START);
        generateButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        try {
                            dm.setRowCount(0);
                            dm.setColumnCount(0);
                            boolean test = true;
                            if (test) {
                                dm.addColumn("col1");
                                table.getColumnModel().getColumn(0).setCellRenderer(d_numberRenderer);
                                java.util.List<Double> vals = new ArrayList<Double>();
                                vals.add(0.0);
                                vals.add(0.1);
                                vals.add(1.23);
                                vals.add(123.45);
                                vals.add(1234.567);
                                vals.add(2931502.50);


                                for (int i=0;i<6;++i) {
                                    Vector<Object> v = new Vector<Object>();
                                    v.add(vals.get(i));
                                    dm.addRow(v);
                                }
                            } else {
                                List<Integer> dates = periodSelector.getDates();
                                dm.addColumn("col1");
                                for (Integer date : dates) {
                                    Vector<Integer> v = new Vector<Integer>();
                                    v.add(date);
                                    dm.addRow(v);
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        topRow.add(periodSelector);

        frame.setSize(new Dimension(1200,1000));
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}

