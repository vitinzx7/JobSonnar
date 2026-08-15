package jobsonnar.dto.serper;

public class SerperSearchRequestDto {
    private String q;
    private String gl;
    private String hl;
    private int num;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getGl() {
        return gl;
    }

    public void setGl(String gl) {
        this.gl = gl;
    }

    public String getHl() {
        return hl;
    }

    public void setHl(String hl) {
        this.hl = hl;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
