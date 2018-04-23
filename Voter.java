import java.time.Clock;

public class Voter {
    private String name;
    private String vnumber;
    private boolean voted;
    private String voteTime;

    public Voter(String nameIn, String vnumberIn) {
        this.name = nameIn;
        this.vnumber = vnumberIn;
        this.voted = false;
        this.voteTime = "";
    }

    public String getName() {
        return this.name;
    }

    public String getVnumber() {
        return this.vnumber;
    }

    public boolean getVoted() {
        return this.voted;
    }

    public String getVoteTime() {
        return this.voteTime;
    }

    public void setVoted() {
        this.voted = true;
    }

    public void setVoteTime() {
        this.voteTime = Clock.systemUTC().instant().toString();
    }

    public void setVoteTime(String voteTimeIn) {
        this.voteTime = voteTimeIn;
    }
}
