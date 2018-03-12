package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

@JsonIgnoreProperties({"comparatorTime"})
public class Match {
    private String local;
    private String visitor;
    private DateTime matchDate;
    private String stadium;
    private String city;
    private String tvChannel;
    private int round;

    public void setLocal(String local) {
        this.local = local;
    }

    public void setVisitor(String visitor) {
        this.visitor = visitor;
    }

    public void setMatchDate(DateTime matchDate) {
        this.matchDate = matchDate;
    }

    public void setStadium(String stadium) {
        this.stadium = stadium;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setTvChannel(String tvChannel) {
        this.tvChannel = tvChannel;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getLocal() {

        return local;
    }

    public String getVisitor() {
        return visitor;
    }

    public DateTime getMatchDate() {
        return matchDate;
    }

    public String getStadium() {
        return stadium;
    }

    public String getCity() {
        return city;
    }

    public String getTvChannel() {
        return tvChannel;
    }

    public int getRound() {
        return round;
    }

    public long getComparatorTime() {
        return getMatchDate().getMillis() * getRound();
    }
}
