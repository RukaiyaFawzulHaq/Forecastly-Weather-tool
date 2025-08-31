package app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Enumeration;



public final class Theme {



    public static final Font APP;
    public static final Font TITLE;
    public static final Font CARD;
    public static final Font CARD_BOLD;


    public static final Color OXFORD  = hex("#192338");
    public static final Color SPACE   = hex("#1E2E4F");
    public static final Color YINMN   = hex("#31487A");
    public static final Color JORDY   = hex("#8FB3E2");
    public static final Color LAVENDER= hex("#D9E1F1");
    public static final Color WHITE   = Color.WHITE;
    public static final Color LAVENDER_TOP = hex("#E9EEF8");




    static {
        APP       = pickFont(14f, Font.PLAIN,
                "Poppins","Inter","SF Pro Display","Segoe UI","Roboto");
        TITLE     = pickFont(44f, Font.BOLD,
                "Poppins","Inter","SF Pro Display","Segoe UI","Roboto");
        CARD      = pickFont(13f, Font.PLAIN,
                "Poppins","Inter","SF Pro Display","Segoe UI","Roboto");
        CARD_BOLD = pickFont(14f, Font.BOLD,
                "Poppins","Inter","SF Pro Display","Segoe UI","Roboto");
    }

    private Theme(){}

    public static void applyUIFont(Font base){
        for (Enumeration<?> e = UIManager.getDefaults().keys(); e.hasMoreElements();) {
            Object k = e.nextElement();
            if (UIManager.get(k) instanceof Font) UIManager.put(k, base);
        }
    }

    public static Font emojiFont(int size){
        String[] names = {"Segoe UI Emoji","Segoe UI Symbol","Noto Color Emoji",
                "Apple Color Emoji","Twemoji Mozilla"};
        for (String n: names){
            Font f = new Font(n, Font.PLAIN, size);
            if (f.getFamily().equals(n)) return f;
        }
        return new Font("SansSerif", Font.PLAIN, size);
    }

    public static JLabel pill(String text){
        JLabel l=new JLabel(text);
        l.setOpaque(true);
        l.setBackground(new Color(255,255,255,28));
        l.setForeground(Color.WHITE);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255,255,255,40),1,true),
                new EmptyBorder(6,10,6,10)
        ));
        return l;
    }

    public static class GradientPanel extends JPanel {
        private final Color c1,c2;
        public GradientPanel(Color c1, Color c2){ this.c1=c1; this.c2=c2; setOpaque(false); }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0,0,c1,getWidth(),getHeight(),c2);
            g2.setPaint(gp); g2.fillRect(0,0,getWidth(),getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class BackgroundPanel extends JPanel {
        private final Image img;
        private final boolean cover;
        private final Color overlay;

        public BackgroundPanel(Image img, boolean cover, Color overlay) {
            this.img = img;
            this.cover = cover;
            this.overlay = overlay;
            setOpaque(false);
            setLayout(new BorderLayout());
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img == null) return;

            int w = getWidth(), h = getHeight();
            int iw = img.getWidth(this), ih = img.getHeight(this);

            int sw = w, sh = h, x = 0, y = 0;
            if (cover && iw > 0 && ih > 0) {
                double s = Math.max((double) w / iw, (double) h / ih);
                sw = (int) Math.round(iw * s);
                sh = (int) Math.round(ih * s);
                x = (w - sw) / 2;
                y = (h - sh) / 2;
            }
            g.drawImage(img, x, y, sw, sh, this);

            if (overlay != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(overlay);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        }
    }


    public static class RoundPanel extends JPanel {
        private final Color fill, shadow;
        public RoundPanel(Color fill, Color shadow){ this.fill=fill; this.shadow=shadow; setOpaque(false); }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            g2.setColor(shadow); g2.fillRoundRect(2,3,w-4,h-6,18,18);
            g2.setColor(fill);   g2.fillRoundRect(0,0,w-4,h-6,18,18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void styleSeg(AbstractButton b, boolean selected){
        if (selected) {
            b.setBackground(WHITE);
            b.setForeground(OXFORD);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(YINMN, 1, true),
                    new EmptyBorder(8,18,8,18)
            ));
        } else {
            b.setBackground(JORDY);
            b.setForeground(Color.WHITE);
            b.setBorder(new EmptyBorder(9,19,9,19));
        }
    }

    public static Color hex(String s){
        return new Color(
                Integer.valueOf(s.substring(1,3),16),
                Integer.valueOf(s.substring(3,5),16),
                Integer.valueOf(s.substring(5,7),16)
        );
    }


    private static Font pickFont(float size, int style, String... names){
        for (String n: names){
            Font f = new Font(n, style, Math.round(size));
            if (f.getFamily().equals(n)) return f.deriveFont(size);
        }
        return new Font("SansSerif", style, Math.round(size));
    }


}
