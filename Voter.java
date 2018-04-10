public class Voter {
    private String voterinfo;
    private boolean voted;
    private String voteTime;

    public Voter(String voterinfoIn) {
        this.voterinfo = voterinfoIn;
        this.voted = false;
        this.voteTime = "";
    }

    public String getVoterinfo() {
        return this.voterinfo;
    }

    public String getName() {
        String name = new String();
        return name;
    }

    public String getNumber() {
        String vnumber = new String();
        return vnumber;
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
        //this.voteTime = Clock.systemUTC().instant().toString();
        this.voteTime = "1";
    }

    public void setVoteTime(String voteTime) {
        this.voteTime = voteTime;
    }
}
