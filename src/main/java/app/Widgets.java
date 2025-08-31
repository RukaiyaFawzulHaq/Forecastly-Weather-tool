package app;

import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.border.EmptyBorder;

public final class Widgets {

    private Widgets(){}

    public static JComponent infoCard(String title, String sub){
        Theme.RoundPanel card = new Theme.RoundPanel(Color.WHITE, new Color(0,0,0,18));
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12,12,12,12));
        card.setPreferredSize(new Dimension(360, 90));

        JLabel t = new JLabel(title);
        t.setFont(Theme.CARD_BOLD);
        t.setForeground(Theme.OXFORD);

        JLabel s = new JLabel(sub);
        s.setFont(Theme.CARD);
        s.setForeground(Theme.SPACE);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(t);
        box.add(Box.createVerticalStrut(6));
        box.add(s);

        card.add(box, BorderLayout.CENTER);
        return card;
    }


    public static JPanel weatherCard(String title, String temp, String rain, String wind, Color c1, Color c2){
        JPanel card = new Theme.GradientPanel(c1, c2);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(10,12,10,12));

        JLabel t1 = new JLabel(title);
        t1.setForeground(Color.WHITE);
        t1.setFont(Theme.APP.deriveFont(Font.BOLD, 14f));

        JLabel t2 = new JLabel(temp);
        t2.setForeground(Color.WHITE);
        t2.setFont(Theme.APP.deriveFont(Font.BOLD, 24f));

        JLabel t3 = new JLabel(rain);
        t3.setForeground(Color.WHITE);
        t3.setFont(Theme.emojiFont(12));

        JLabel t4 = new JLabel(wind);
        t4.setForeground(Color.WHITE);
        t4.setFont(Theme.emojiFont(12));

        t1.setAlignmentX(0.5f);
        t2.setAlignmentX(0.5f);

        card.add(t1);
        card.add(Box.createVerticalStrut(2));
        card.add(t2);
        card.add(Box.createVerticalStrut(6));
        card.add(t3);
        card.add(t4);

        card.setPreferredSize(new Dimension(120, 110));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return card;
    }


  // to select date from calendar view type
    public static class DatePicker extends JDialog {
        private YearMonth shown; private LocalDate selected;
        private final JPanel grid = new JPanel(new GridLayout(0, 7, 4, 4));
        private final JLabel title = new JLabel("", SwingConstants.CENTER);

        public DatePicker(Window owner, LocalDate initial) {
            super(owner, "Pick a date", ModalityType.APPLICATION_MODAL);
            this.shown = YearMonth.from(initial);
            setLayout(new BorderLayout(6, 6));

            JButton prev = new JButton("<"), next = new JButton(">");
            prev.addActionListener(e -> { shown = shown.minusMonths(1); rebuild(); });
            next.addActionListener(e -> { shown = shown.plusMonths(1); rebuild(); });

            JPanel top = new JPanel(new BorderLayout());
            title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
            top.add(prev, BorderLayout.WEST); top.add(title, BorderLayout.CENTER); top.add(next, BorderLayout.EAST);

            add(top, BorderLayout.NORTH); add(grid, BorderLayout.CENTER);
            JButton cancel = new JButton("Cancel"); cancel.addActionListener(e -> { selected = null; dispose(); });
            add(cancel, BorderLayout.SOUTH);
            rebuild(); pack(); setLocationRelativeTo(owner);
        }

        private void rebuild() {
            title.setText(shown.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + shown.getYear());
            grid.removeAll();
            for (int i=0;i<7;i++){ int v=(i==0?7:i); DayOfWeek d=DayOfWeek.of(v);
                JLabel l=new JLabel(d.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
                l.setFont(l.getFont().deriveFont(Font.BOLD)); grid.add(l); }
            LocalDate first = shown.atDay(1);
            int lead = (first.getDayOfWeek().getValue() % 7);
            for (int i=0;i<lead;i++) grid.add(new JLabel(""));
            int len = shown.lengthOfMonth();
            for (int day=1; day<=len; day++){
                LocalDate date = shown.atDay(day);
                JButton b=new JButton(String.valueOf(day));
                b.addActionListener(e -> { selected = date; dispose(); });
                grid.add(b);
            }
            grid.revalidate(); grid.repaint();
        }

        public static java.util.Date pick(Component parent, java.util.Date current) {
            Window w = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
            LocalDate init = current.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            DatePicker dlg = new DatePicker(w, init);
            dlg.setVisible(true);
            if (dlg.selected == null) return null;
            return java.util.Date.from(dlg.selected.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
    }
}
