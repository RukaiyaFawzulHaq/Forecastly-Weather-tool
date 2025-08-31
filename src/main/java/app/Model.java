package app;

/** Data model: linked lists + builders + window scoring. */
public final class Model {
    private Model(){}

    // The linked lists for events
    public static class EventNameNode { public String name; public EventNameNode next; public EventNameNode(String n){name=n;} }
    public static class EventDayNode  { public String date; public EventNameNode headName; public EventDayNode next; public EventDayNode(String d){date=d;} }
    public static class EventDayList {
        public EventDayNode head;
        public EventDayNode findDay(String d){
            for (EventDayNode p=head; p!=null; p=p.next) if (p.date.equals(d)) return p;
            return null;
        }
        public void addEvent(String date, String name){
            EventDayNode day = findDay(date);
            if (day==null){
                day = new EventDayNode(date);
                if (head==null) head=day; else { EventDayNode t=head; while(t.next!=null) t=t.next; t.next=day; }
            }
            EventNameNode e = new EventNameNode(name);
            if (day.headName==null) day.headName=e; else { EventNameNode q=day.headName; while(q.next!=null) q=q.next; q.next=e; }
        }
    }

    // the Linked listes for forecast weather
    public static class HourlyNode {
        public String isoTime; public double temperatureC; public int rainProbPct; public double windKph; public int humidityPct;
        public HourlyNode next;
        public HourlyNode(String t,double c,int r,double w,int h){isoTime=t;temperatureC=c;rainProbPct=r;windKph=w;humidityPct=h;}
    }
    public static class DailyNode {
        public String date; public double tMaxC; public double tMinC; public int rainProbMaxPct; public double windMaxKph;
        public DailyNode next;
        public DailyNode(String d,double x,double n,int r,double w){date=d;tMaxC=x;tMinC=n;rainProbMaxPct=r;windMaxKph=w;}
    }
    public static class HourlyList {
        public HourlyNode head;
        public void add(HourlyNode n){ if(head==null) head=n; else { HourlyNode t=head; while(t.next!=null) t=t.next; t.next=n; } }
        public int size(){ int c=0; for(HourlyNode t=head;t!=null;t=t.next) c++; return c; }
    }
    public static class DailyList {
        public DailyNode head;
        public void add(DailyNode n){ if(head==null) head=n; else { DailyNode t=head; while(t.next!=null) t=t.next; t.next=n; } }
        public DailyNode findByDate(String d){ for(DailyNode t=head;t!=null;t=t.next) if(t.date.equals(d)) return t; return null; }
    }


    public static HourlyList buildHourly(Net.ForecastResponse fr){
        HourlyList list = new HourlyList();
        if (fr==null || fr.hourly==null || fr.hourly.time==null) return list;
        int n = Math.min(7*24, fr.hourly.time.size());
        for (int i=0;i<n;i++){
            list.add(new HourlyNode(
                    fr.hourly.time.get(i),
                    fr.hourly.temperature.get(i),
                    fr.hourly.rainProb.get(i),
                    fr.hourly.wind.get(i),
                    fr.hourly.humidity.get(i)
            ));
        }
        return list;
    }

    public static DailyList buildDaily(Net.ForecastResponse fr){
        DailyList list = new DailyList();
        if (fr==null || fr.daily==null || fr.daily.time==null) return list;
        int n = Math.min(7, fr.daily.time.size());
        for (int i=0;i<n;i++){
            list.add(new DailyNode(
                    fr.daily.time.get(i),
                    fr.daily.tmax.get(i),
                    fr.daily.tmin.get(i),
                    fr.daily.rainProbMax.get(i),
                    fr.daily.windMax.get(i)
            ));
        }
        return list;
    }


    public static class WindowResult {
        public int startIndex, length; public double score; public String startIso, endIso;
    }

    public static WindowResult bestWindow(HourlyList hours, int windowLen, int maxRain, double maxWind){
        int n=hours==null?0:hours.size();
        if(n==0 || windowLen<=0 || windowLen>n) return null;

        HourlyNode start=hours.head, end=advance(start, windowLen-1);
        if(end==null) return null;

        double sumRain=0,sumWind=0,sumHum=0; HourlyNode p=start;
        for(int k=0;k<windowLen;k++){ sumRain+=p.rainProbPct; sumWind+=p.windKph; sumHum+=p.humidityPct; p=p.next; }

        double bestScore=Double.POSITIVE_INFINITY; int bestStart=-1; String bestStartIso=start.isoTime,bestEndIso=end.isoTime; int idx=0;
        double avgRain=sumRain/windowLen, avgWind=sumWind/windowLen, avgHum=sumHum/windowLen;
        if(!(avgRain>maxRain || avgWind>maxWind)){ double s=avgRain*1.2+avgWind*0.7+(avgHum>80?5:0); bestScore=s; bestStart=0; }

        while(end.next!=null){
            HourlyNode leaving=start; start=start.next; end=end.next;
            sumRain += end.rainProbPct - leaving.rainProbPct;
            sumWind += end.windKph     - leaving.windKph;
            sumHum  += end.humidityPct - leaving.humidityPct;
            idx++;
            avgRain=sumRain/windowLen; avgWind=sumWind/windowLen; avgHum=sumHum/windowLen;
            if(avgRain>maxRain || avgWind>maxWind) continue;
            double s=avgRain*1.2+avgWind*0.7+(avgHum>80?5:0);
            if(s<bestScore){ bestScore=s; bestStart=idx; bestStartIso=start.isoTime; bestEndIso=end.isoTime; }
        }
        if(bestStart<0) return null;
        WindowResult wr=new WindowResult(); wr.startIndex=bestStart; wr.length=windowLen; wr.score=bestScore; wr.startIso=bestStartIso; wr.endIso=bestEndIso; return wr;
    }

    public static HourlyNode copy(HourlyNode h){ return new HourlyNode(h.isoTime,h.temperatureC,h.rainProbPct,h.windKph,h.humidityPct); }
    private static HourlyNode advance(HourlyNode n,int steps){ while(n!=null && steps>0){ n=n.next; steps--; } return n; }
}
