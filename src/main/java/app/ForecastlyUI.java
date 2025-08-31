package app;

import app.Model.*;
import app.Widgets.DatePicker;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Objects;

public class ForecastlyUI extends JFrame {

    //to keep the events that is saved
    private static final String DATA_FILE =
            System.getProperty("user.home") + File.separator + ".forecastly_events.txt";


    private static final boolean MAP_IS_MERCATOR = false;


    // Linked list for map
    static final class StrNode { String v; StrNode next; StrNode(String v){ this.v=v; } }
    private static StrNode MAP_HEAD = link(
            "/app/world.png", "/app/world.jpg", "/app/p.jpg", "/app/map.png", "/app/map.jpg"
    );

    private static StrNode link(String... vals){
        StrNode head=null, tail=null;
        for (String s: vals){
            StrNode n = new StrNode(s);
            if (head==null){ head=n; tail=n; } else { tail.next=n; tail=n; }
        }
        return head;
    }



    private String tzName = "";
    private HourlyList hourly = new HourlyList();
    private DailyList  daily  = new DailyList();
    private final EventDayList events = new EventDayList();
    private final SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
    private String lastCity = "Colombo";


    private final JTextField cityField = new JTextField("Loading…", 18);
    private final JButton useMyLocBtn = new JButton("Use My Location");
    private final JButton fetchBtn    = new JButton("Fetch");
    private final JLabel headlineTemp = new JLabel("—°");
    private final JLabel headlineCity = new JLabel("Weather");
    private final JLabel headlineIcon = new JLabel("—");


    private final JPanel viewStack = new JPanel(new CardLayout());
    private final ButtonGroup tabGroup = new ButtonGroup();
    private final JToggleButton btnToday  = segButton("Today",  "today");
    private final JToggleButton btnDays   = segButton("7 Days", "days");
    private final JToggleButton btnEvents = segButton("Events","events");

    // for today tab
    private final JPanel todayRoot   = new Theme.GradientPanel(Theme.SPACE, Theme.YINMN);
    private final JPanel hourlyStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
    private final JPanel todayCards  = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
    private final JSpinner winH      = new JSpinner(new SpinnerNumberModel(2,1,12,1));
    private final JSpinner maxRain   = new JSpinner(new SpinnerNumberModel(40,0,100,5));
    private final JSpinner maxWind   = new JSpinner(new SpinnerNumberModel(25,0,200,1));
    private final JButton findSlot   = new JButton("Find Best Slot (Next 24h)");

    // for 7 day tab
    private final JPanel dayDetailCards = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));

    private final JPanel daysRoot = new Theme.GradientPanel(Theme.YINMN, Theme.JORDY);
    private final JPanel daysRows = new JPanel();
    private final JPanel rowTop   = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 18));
    private final JPanel rowBottom= new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 18));


    private final JPanel detailsHost = new JPanel(new BorderLayout());
    private final Theme.RoundPanel detailsCard = new Theme.RoundPanel(Color.WHITE, new Color(0,0,0,18));

    // for event tab
    private static final int EVENTS_COLS = 4;
    private final JPanel eventsGrid = new JPanel(new GridLayout(0, EVENTS_COLS, 18, 18));
    private final JPanel eventsGridWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 18));

    private final JPanel eventsRoot  = new Theme.GradientPanel(Theme.SPACE, Theme.YINMN);
    private final JButton pickBtn    = new JButton("Pick…");
    private final JSpinner dateSpin  = new JSpinner(new SpinnerDateModel(new java.util.Date(), null, null, Calendar.DAY_OF_MONTH));
    private final JComboBox<String> activityBox = new JComboBox<>(new String[]{"Jogging","Party","Shopping","Custom"});
    private final JTextField eventField = new JTextField(16);
    private final JButton recommendBtn = new JButton("Recommend Time");
    private final JButton addEvent     = new JButton("Add Event");


    public ForecastlyUI() {
        super("Forecastly — Java Swing");
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception ignore){}
        Theme.applyUIFont(Theme.APP);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(1024, 720));


        JPanel header = new Theme.GradientPanel(Theme.OXFORD, Theme.SPACE);
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(16,16,12,16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); left.setOpaque(false);
        left.add(Theme.pill("City:"));
        left.add(cityField);
        left.add(fetchBtn);
        left.add(useMyLocBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,16,0)); right.setOpaque(false);
        headlineIcon.setFont(Theme.emojiFont(44));
        headlineTemp.setFont(Theme.TITLE);
        headlineTemp.setForeground(Color.WHITE);
        headlineCity.setFont(Theme.APP.deriveFont(Font.PLAIN, 16f));
        headlineCity.setForeground(Color.WHITE);
        right.add(headlineIcon);
        right.add(headlineTemp);
        right.add(headlineCity);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);


        JPanel segBar = buildSegmentBar();
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Theme.LAVENDER);
        center.add(segBar, BorderLayout.NORTH);
        center.add(viewStack, BorderLayout.CENTER);
        viewStack.add(todayTab(),   "today");
        viewStack.add(sevenDaysTab(),"days");
        viewStack.add(eventsTab(),  "events");
        add(center, BorderLayout.CENTER);
        btnToday.setSelected(true);
        showCard("today");


        fetchBtn.addActionListener(e -> { lastCity = cityField.getText().trim(); saveEvents(); fetchCity(); });
        useMyLocBtn.addActionListener(e -> detectAndFetchMyLocation());
        findSlot.addActionListener(e -> onBestSlotNext24h());
        addEvent.addActionListener(e -> onAddEvent());
        recommendBtn.addActionListener(e -> onRecommendForDay());
        pickBtn.addActionListener(e -> {
            java.util.Date chosen = DatePicker.pick(this, (java.util.Date) dateSpin.getValue());
            if (chosen != null) dateSpin.setValue(chosen);
        });


        loadEvents();
        cityField.setText(lastCity);
        refreshEventsView();


        //this feches the weather when the user interface is loaded
        SwingUtilities.invokeLater(() -> {
            String c = cityField.getText().trim();
            if (!c.isBlank() && !"Loading…".equals(c)) fetchCity();
            else detectAndFetchMyLocation();
        });
        addWindowListener(new WindowAdapter(){ @Override public void windowClosing(WindowEvent e){ saveEvents(); }});

        getContentPane().setBackground(Theme.LAVENDER);
        pack();
        setLocationRelativeTo(null);
    }


    private JPanel buildSegmentBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        bar.setOpaque(false);
        tabGroup.add(btnToday);  tabGroup.add(btnDays);  tabGroup.add(btnEvents);
        bar.add(btnToday); bar.add(btnDays); bar.add(btnEvents);
        updateSegStyles(); return bar;
    }
    private JToggleButton segButton(String text, String key) {
        JToggleButton b = new JToggleButton(text);
        b.setFont(Theme.APP.deriveFont(Font.BOLD, 14f));
        b.setFocusPainted(false); b.setOpaque(true); b.setContentAreaFilled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140, 40));
        b.addActionListener(e -> showCard(key));
        Theme.styleSeg(b, false);
        return b;
    }
    private void showCard(String key) { ((CardLayout) viewStack.getLayout()).show(viewStack, key); updateSegStyles(); }
    private void updateSegStyles(){ Theme.styleSeg(btnToday, btnToday.isSelected()); Theme.styleSeg(btnDays, btnDays.isSelected()); Theme.styleSeg(btnEvents, btnEvents.isSelected()); }

      //ui for todays interface
    private JPanel todayTab() {
        todayRoot.setLayout(new BorderLayout());
        hourlyStrip.setOpaque(false);


        JScrollPane sc = new JScrollPane(hourlyStrip,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(BorderFactory.createEmptyBorder());


        todayCards.setOpaque(false);
        JScrollPane cardsScroll = new JScrollPane(todayCards);
        cardsScroll.getViewport().setBackground(Theme.LAVENDER);
        cardsScroll.setBorder(BorderFactory.createEmptyBorder());


        JScrollPane mapScroll = buildMapScroll();


        JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT,10,6));
        picker.setOpaque(false);
        picker.add(Theme.pill("Window (hrs):")); picker.add(winH);
        picker.add(Theme.pill("Max Rain %:"));   picker.add(maxRain);
        picker.add(Theme.pill("Max Wind km/h:"));picker.add(maxWind);
        picker.add(findSlot);


        JPanel mid = new JPanel(new BorderLayout());
        mid.setOpaque(false);
        mid.add(cardsScroll, BorderLayout.NORTH);
        mid.add(mapScroll,   BorderLayout.CENTER);

        todayRoot.add(sc,   BorderLayout.NORTH);
        todayRoot.add(mid,  BorderLayout.CENTER);
        todayRoot.add(picker, BorderLayout.SOUTH);
        return wrapPad(todayRoot, 12);
    }



    private JScrollPane buildMapScroll() {
        try {
            BufferedImage img = loadMapImage();
            if (img == null) throw new IOException("Map image not found in /app/");

            JLabel mapLabel = new JLabel(new ImageIcon(img));


            mapLabel.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int x = e.getX();
                    int y = e.getY();
                    int width = img.getWidth();
                    int height = img.getHeight();

                    double lon = (x / (double) width) * 360.0 - 180.0;
                    double lat;
                    if (MAP_IS_MERCATOR) {

                        double ry = y / (double) height;
                        double n = Math.PI * (1 - 2 * ry);
                        lat = Math.toDegrees(Math.atan(Math.sinh(n)));
                    } else {

                        lat = 90.0 - (y / (double) height) * 180.0;
                    }

                    try {

                        var geoOpt = Net.reverseGeocode(lat, lon);
                        if (geoOpt.isPresent()) {
                            Net.GeoResult g = geoOpt.get();
                            String city = g.name + (g.country != null ? ", " + g.country : "");
                            cityField.setText(city);
                            lastCity = city;


                            var frOpt = Net.fetchForecast(g.latitude, g.longitude);
                            if (frOpt.isPresent()) {
                                applyForecast(city, frOpt.get());
                                saveEvents();
                            } else {
                                JOptionPane.showMessageDialog(ForecastlyUI.this, "Forecast fetch failed for clicked point.");
                            }
                        } else {
                            JOptionPane.showMessageDialog(ForecastlyUI.this, "No location found at clicked point.");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ForecastlyUI.this, "Map click failed: " + ex.getMessage());
                    }
                }
            });

            JScrollPane sp = new JScrollPane(mapLabel);
            sp.setPreferredSize(new Dimension(900, 360));
            return sp;
        } catch (Exception e) {
            e.printStackTrace();
            return new JScrollPane(new JLabel("Map failed to load"));
        }
    }


    //to show the map picture
    private static BufferedImage loadMapImage() {
        for (StrNode p = MAP_HEAD; p != null; p = p.next) {
            try {
                var url = ForecastlyUI.class.getResource(p.v);
                if (url != null) return ImageIO.read(url);
            } catch (Exception ignore) {}
        }
        return null;
    }


    // ui for the 7 day
    private JPanel sevenDaysTab() {
        daysRoot.setLayout(new BorderLayout());


        daysRows.setOpaque(false);
        daysRows.setLayout(new BoxLayout(daysRows, BoxLayout.Y_AXIS));

        rowTop.setOpaque(false);
        rowBottom.setOpaque(false);

        daysRows.add(rowTop);
        daysRows.add(Box.createVerticalStrut(10));
        daysRows.add(rowBottom);


        JScrollPane gridScroll = new JScrollPane(
                daysRows,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        gridScroll.setBorder(BorderFactory.createEmptyBorder());
        gridScroll.getViewport().setBackground(Theme.LAVENDER);


        detailsCard.setLayout(new BorderLayout());
        detailsCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        detailsHost.setBackground(Theme.LAVENDER);
        detailsHost.setBorder(new EmptyBorder(12, 12, 12, 12));
        detailsHost.add(detailsCard, BorderLayout.CENTER);
        detailsHost.setPreferredSize(new Dimension(0, 220));


        detailsCard.removeAll();
        JLabel hint = new JLabel("Select a day to see weather & your events here.");
        hint.setFont(Theme.CARD);
        hint.setForeground(Theme.SPACE);
        detailsCard.add(hint, BorderLayout.CENTER);


        daysRoot.add(detailsHost, BorderLayout.SOUTH);

        daysRoot.add(gridScroll,  BorderLayout.CENTER);
        return daysRoot;
    }

    // event ui
    private JPanel eventsTab() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT,10,8));
        bar.setOpaque(false);
        ((JSpinner.DateEditor) dateSpin.getEditor()).getFormat().applyPattern("yyyy-MM-dd");
        bar.add(Theme.pill("Date:")); bar.add(dateSpin); bar.add(pickBtn);
        bar.add(Theme.pill("Activity:")); bar.add(activityBox);
        bar.add(Theme.pill("Event name:")); bar.add(eventField);
        bar.add(recommendBtn);
        bar.add(addEvent);

        eventsRoot.setLayout(new BorderLayout());


        eventsRoot.add(bar, BorderLayout.NORTH);


        eventsGrid.setOpaque(false);
        eventsGridWrap.setOpaque(false);
        eventsGridWrap.add(eventsGrid);


        JScrollPane sp = new JScrollPane(
                eventsGridWrap,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        sp.getViewport().setBackground(Theme.LAVENDER);
        sp.setBorder(BorderFactory.createEmptyBorder());

        eventsRoot.add(sp, BorderLayout.CENTER);
        return eventsRoot;

    }



    private void detectAndFetchMyLocation() {
        setBusy(true);
        new SwingWorker<Void,Void>() {
            String err=null; Net.IPInfo info=null; Net.ForecastResponse fr=null;
            @Override protected Void doInBackground() {
                try {
                    info = Net.fetchIP();
                    if (info==null) throw new RuntimeException("IP lookup failed");
                    var frOpt = Net.fetchForecast(info.lat, info.lon);
                    if (frOpt.isEmpty()) throw new RuntimeException("Forecast fetch failed");
                    fr = frOpt.get();
                } catch(Exception ex){ err = ex.getMessage(); }
                return null;
            }
            @Override protected void done() {
                if (err!=null){ toast("Location detect failed, type a city and Fetch.\n"+err); setBusy(false); return; }
                lastCity = info.city + (info.country==null?"":", "+info.country);
                cityField.setText(lastCity);
                applyForecast(lastCity, fr);
                saveEvents();
                setBusy(false);
            }
        }.execute();
    }

    private void fetchCity() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) { toast("Enter a city"); return; }
        setBusy(true);
        new SwingWorker<Void,Void>() {
            String err=null; Net.GeoResult loc; Net.ForecastResponse fr;
            @Override protected Void doInBackground() {
                try {
                    var g = Net.geocode(city);
                    if (g.isEmpty()) { err="City not found."; return null; }
                    loc = g.get();
                    var frOpt = Net.fetchForecast(loc.latitude, loc.longitude);
                    if (frOpt.isEmpty()) { err="Forecast fetch failed."; return null; }
                    fr = frOpt.get();
                } catch(Exception ex){ err = ex.getMessage(); }
                return null;
            }
            @Override protected void done() {
                if (err!=null){ toast("Error: "+err); setBusy(false); return; }
                String label = loc.name + (loc.country!=null?(", "+loc.country):"");
                lastCity = label; applyForecast(label, fr);
                setBusy(false);
            }
        }.execute();
    }

    private void applyForecast(String cityLabel, Net.ForecastResponse fr){
        tzName = fr.timezone==null?"":fr.timezone;
        hourly = Model.buildHourly(fr);
        daily  = Model.buildDaily(fr);


        headlineCity.setText(cityLabel);
        if (hourly.head!=null) {
            int t = (int)Math.round(hourly.head.temperatureC);
            headlineTemp.setText(t + "°");
            headlineIcon.setText(iconFor(hourly.head.rainProbPct, hourly.head.windKph));
        }


        hourlyStrip.removeAll(); hourlyStrip.setOpaque(false);
        int shown = Math.min(24, hourly.size());
        HourlyNode p = hourly.head;
        for (int i=0; i<shown && p!=null; i++, p=p.next) {
            JPanel card = weatherCard(
                    timeOnly(p.isoTime),
                    String.format("%d°", (int)Math.round(p.temperatureC)),
                    "💧"+p.rainProbPct+"%",
                    "🌬 "+String.format("%.0f",p.windKph)+" km/h",
                    Theme.SPACE, Theme.YINMN);
            hourlyStrip.add(card);
        }
        hourlyStrip.revalidate(); hourlyStrip.repaint();


        todayCards.removeAll();
        todayCards.add(infoCard("Found " + cityLabel, "TZ: " + tzName));
        if (hourly.head != null) {
            HourlyNode h = hourly.head;
            String sub = String.format("%.1f°C  •  Rain %d%%  •  Wind %.1f km/h  •  Hum %d%%",
                    h.temperatureC, h.rainProbPct, h.windKph, h.humidityPct);
            String title = "Now " + timeOnly(h.isoTime);
            todayCards.add(infoCard(title, sub));
        }
        todayCards.revalidate(); todayCards.repaint();


        rebuildDaysRows();
    }

    private void rebuildDaysRows() {
        rowTop.removeAll();
        rowBottom.removeAll();

        int i = 0;
        for (Model.DailyNode d = daily.head; d != null && i < 7; d = d.next, i++) {
            JPanel card = dayTile(d);
            if (i < 4) rowTop.add(card);
            else      rowBottom.add(card);
        }

        rowTop.revalidate();   rowTop.repaint();
        rowBottom.revalidate();rowBottom.repaint();
    }

    private void onBestSlotNext24h() {
        if (hourly.size()==0){ toast("Fetch forecast first."); return; }
        int w = (int)winH.getValue();
        int r = (int)maxRain.getValue();
        double wmax = ((Number)maxWind.getValue()).doubleValue();

        HourlyList slice = new HourlyList();
        HourlyNode p = hourly.head; int count=0;
        while(p!=null && count<24){ slice.add(Model.copy(p)); p=p.next; count++; }

        Model.WindowResult wr = Model.bestWindow(slice, w, r, wmax);
        if (wr==null){ toast("No suitable slot in next 24h."); return; }

        String subtitle = wr.startIso.substring(11,16) + " → " + wr.endIso.substring(11,16) +
                "  •  score " + String.format("%.2f", wr.score);
        todayCards.add(infoCard("Best "+w+"h slot (next 24h)", subtitle));
        todayCards.revalidate(); todayCards.repaint();
    }

    //to add event
    private void onAddEvent() {
        String date = ymd.format((java.util.Date)dateSpin.getValue());
        String name = eventField.getText().trim();
        if (name.isEmpty()) { toast("Type an event name."); return; }
        events.addEvent(date, name);
        eventField.setText("");
        toast("Event added for " + date);
        refreshEventsView();
        saveEvents();
    }

    //to find best time of the day today
    private void onRecommendForDay() {
        if (hourly.size()==0){ toast("Fetch forecast first."); return; }
        String date = ymd.format((java.util.Date)dateSpin.getValue());
        String activity = Objects.toString(activityBox.getSelectedItem(),"Custom");
        Preset pz = presets(activity);

        HourlyList dayList = new HourlyList();
        for (HourlyNode p=hourly.head; p!=null; p=p.next)
            if (p.isoTime.startsWith(date)) dayList.add(Model.copy(p));
        if (dayList.head==null){ toast("No hourly data for "+date+" (try a nearer date)."); return; }

        Model.WindowResult wr = Model.bestWindow(dayList, pz.windowHours, pz.maxRain, pz.maxWind);
        if (wr==null){
            JOptionPane.showMessageDialog(this,
                    "For "+activity+" on "+date+":\nNo good window found.",
                    "No Window", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String msg = String.format(
                "Best time for %s on %s:\n%s → %s (window %dh)\nLimits: rain ≤ %d%%, wind ≤ %.0f km/h",
                activity, date, wr.startIso.substring(11,16), wr.endIso.substring(11,16),
                pz.windowHours, pz.maxRain, pz.maxWind
        );
        JOptionPane.showMessageDialog(this, msg, "Recommended Time", JOptionPane.INFORMATION_MESSAGE);
    }

   //to keep the events according to the dates in order
    private void refreshEventsView() {
        eventsGrid.removeAll();


        EventDayNode sorted = null;
        for (EventDayNode d = events.head; d != null; d = d.next) {
            sorted = insertDaySorted(sorted, d);
        }


        for (EventDayNode d = sorted; d != null; d = d.next) {
            for (EventNameNode n = d.headName; n != null; n = n.next) {
                eventsGrid.add(eventCard(d.date, n.name));
            }
        }

        eventsGrid.revalidate();
        eventsGrid.repaint();
    }


    private static EventDayNode insertDaySorted(EventDayNode sortedHead, EventDayNode src){
        EventDayNode node = new EventDayNode(src.date);
        EventNameNode tail=null;
        for(EventNameNode q=src.headName;q!=null;q=q.next){
            EventNameNode nn=new EventNameNode(q.name);
            if(node.headName==null){ node.headName=nn; tail=nn; } else { tail.next=nn; tail=nn; }
        }
        if (sortedHead==null) return node;
        if (node.date.compareTo(sortedHead.date)<0){ node.next=sortedHead; return node; }
        EventDayNode prev=null, cur=sortedHead;
        while(cur!=null && node.date.compareTo(cur.date)>=0){ prev=cur; cur=cur.next; }
        prev.next=node; node.next=cur; return sortedHead;
    }


    //ui for one event card
    private JComponent eventCard(String date, String name){
        Theme.RoundPanel card = new Theme.RoundPanel(Color.WHITE, new Color(0,0,0,18));
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12,14,12,14));

        card.setPreferredSize(new Dimension(240, 120));

        JLabel title = new JLabel(name);
        title.setFont(Theme.CARD_BOLD);
        title.setForeground(Theme.OXFORD);

        JLabel sub = new JLabel("📅 " + date);
        sub.setFont(Theme.CARD);
        sub.setForeground(Theme.SPACE);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(sub);

        card.add(text, BorderLayout.CENTER);
        return card;
    }


    private JPanel dayTile(DailyNode d){
        JPanel tile = new Theme.GradientPanel(Theme.SPACE, Theme.YINMN); 

        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(new EmptyBorder(12,12,12,12));


        tile.setPreferredSize(new Dimension(200, 170));
        tile.setMaximumSize(new Dimension(200, 170));
        tile.setMinimumSize(new Dimension(200, 170));

        JLabel tDay = new JLabel(d.date);
        tDay.setForeground(Color.WHITE);
        tDay.setFont(Theme.APP.deriveFont(Font.BOLD, 14f));
        tDay.setAlignmentX(0.5f);

        JLabel tIcon = new JLabel(iconFor(d.rainProbMaxPct, d.windMaxKph));
        tIcon.setForeground(Color.WHITE);
        tIcon.setFont(Theme.emojiFont(30));
        tIcon.setAlignmentX(0.5f);

        JLabel tTemp = new JLabel((int)Math.round(d.tMaxC)+"° / "+(int)Math.round(d.tMinC)+"°");
        tTemp.setForeground(Color.WHITE);
        tTemp.setFont(Theme.APP.deriveFont(15f));
        tTemp.setAlignmentX(0.5f);

        tile.add(tDay);
        tile.add(Box.createVerticalStrut(10));
        tile.add(tIcon);
        tile.add(Box.createVerticalStrut(10));
        tile.add(tTemp);

        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tile.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){ onShowDayDetail(d); }});
        return tile;
    }

    private void onShowDayDetail(DailyNode d){
        detailsCard.removeAll();

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Date: " + d.date);
        title.setFont(Theme.CARD_BOLD);
        title.setForeground(Theme.OXFORD);

        JLabel weather = new JLabel(String.format(
                "Max %.1f°C, Min %.1f°C • Rain max %d%% • Wind max %.1f km/h",
                d.tMaxC, d.tMinC, d.rainProbMaxPct, d.windMaxKph
        ));
        weather.setFont(Theme.CARD);
        weather.setForeground(Theme.SPACE);

        body.add(title);
        body.add(Box.createVerticalStrut(6));
        body.add(weather);
        body.add(Box.createVerticalStrut(10));

        EventDayNode day = events.findDay(d.date);
        if (day == null || day.headName == null) {
            JLabel none = new JLabel("No events for this day.");
            none.setFont(Theme.CARD);
            none.setForeground(Theme.SPACE);
            body.add(none);
        } else {
            JLabel evh = new JLabel("Your events:");
            evh.setFont(Theme.CARD_BOLD);
            evh.setForeground(Theme.OXFORD);
            body.add(evh);
            for (EventNameNode n = day.headName; n != null; n = n.next) {
                body.add(bulletLabel(n.name));
            }
        }

        detailsCard.add(body, BorderLayout.CENTER);
        detailsCard.revalidate();
        detailsCard.repaint();
    }

    private static JPanel wrapPad(JComponent c,int pad){ JPanel p=new JPanel(new BorderLayout()); p.setOpaque(false); p.add(c,BorderLayout.CENTER); p.setBorder(new EmptyBorder(pad,pad,pad,pad)); return p; }
    private void setBusy(boolean b){ setCursor(b?Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR):Cursor.getDefaultCursor()); fetchBtn.setEnabled(!b); useMyLocBtn.setEnabled(!b); findSlot.setEnabled(!b); addEvent.setEnabled(!b); recommendBtn.setEnabled(!b); }
    private void toast(String m){ JOptionPane.showMessageDialog(this, m); }
    private static String iconFor(int rainPct, double wind){ if (rainPct >= 60) return "🌧"; if (rainPct >= 30) return "🌦"; if (wind >= 30) return "🌬"; return "☀️"; }
    private static String timeOnly(String iso){ int i=iso.indexOf('T'); return (i>0 && i+3<iso.length())?iso.substring(i+1,i+6):iso; }

    //hourly ui
    private JPanel weatherCard(String title, String temp, String rain, String wind, Color c1, Color c2){
        JPanel card = new Theme.GradientPanel(c1,c2);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(10,12,10,12));
        JLabel t1 = label(title, 14, Font.BOLD, Color.WHITE);
        JLabel t2 = label(temp, 24, Font.BOLD, Color.WHITE);
        JLabel t3 = label(rain, 12, Font.PLAIN, Color.WHITE); t3.setFont(Theme.emojiFont(12));
        JLabel t4 = label(wind, 12, Font.PLAIN, Color.WHITE); t4.setFont(Theme.emojiFont(12));
        t1.setAlignmentX(0.5f); t2.setAlignmentX(0.5f);
        card.add(t1); card.add(Box.createVerticalStrut(2)); card.add(t2);
        card.add(Box.createVerticalStrut(6)); card.add(t3); card.add(t4);
        card.setPreferredSize(new Dimension(120, 110));
        return card;
    }


    //info ui
    private JComponent infoCard(String title, String sub){
        Theme.RoundPanel card = new Theme.RoundPanel(Color.WHITE, new Color(0,0,0,18));
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12,12,10,12));
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


    private static JLabel label(String t, int size, int style, Color col){ JLabel l=new JLabel(t); l.setFont(Theme.APP.deriveFont(style, (float)size)); l.setForeground(col); return l; }

    private JComponent bulletLabel(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        JLabel dot = new JLabel("•"); dot.setFont(Theme.CARD_BOLD); dot.setForeground(Theme.SPACE);
        JLabel tx  = new JLabel(text); tx.setFont(Theme.CARD); tx.setForeground(Theme.OXFORD);
        row.add(dot); row.add(tx); return row;
    }

    //for event activity
    private static class Preset { int windowHours, maxRain; double maxWind; Preset(int h,int r,double w){windowHours=h;maxRain=r;maxWind=w;} }
    private static Preset presets(String a){ if("Jogging".equals(a))return new Preset(1,30,20); if("Party".equals(a))return new Preset(3,60,35); if("Shopping".equals(a))return new Preset(2,70,40); return new Preset(2,50,30); }


    private void saveEvents(){
        try (PrintWriter w = new PrintWriter(new FileWriter(DATA_FILE))) {
            w.println("#CITY=" + lastCity);
            for(EventDayNode d=events.head; d!=null; d=d.next)
                for(EventNameNode n=d.headName; n!=null; n=n.next)
                    w.println(d.date + "|" + n.name.replace('|','/'));
        } catch (Exception ignore){}
    }

    private void loadEvents(){
        lastCity = "Horsham";
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while((line=br.readLine())!=null){
                if (line.startsWith("#CITY=")){ String c=line.substring(6).trim(); if(!c.isBlank()) lastCity=c; }
                else {
                    int bar=line.indexOf('|'); if(bar>0){
                        String date=line.substring(0,bar); String name=line.substring(bar+1);
                        events.addEvent(date,name);
                    }
                }
            }
        } catch (Exception ignore){}
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ForecastlyUI().setVisible(true));
    }
}
