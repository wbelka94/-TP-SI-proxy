public class Statistic {
    private String domain;
    private int bytes_out;
    private int bytes_in;
    private int count;


    public Statistic(String line) {
        String[] parts = line.split(",");
        domain = parts[0];
        count = Integer.parseInt(parts[1]);
        bytes_out = Integer.parseInt(parts[2]);
        bytes_in = Integer.parseInt(parts[3]);
    }

    public boolean checkDomain(String domain){
        if(this.domain.equals(domain)){
            return true;
        }
        return false;
    }

    public void addCount(){
        count++;
    }

    public void addBytesOut(int bytes){
        this.bytes_out += bytes;
    }

    public void addBytesIn(int bytes){
        this.bytes_in += bytes;
    }

    public String toString(){
        return domain+","+count+","+bytes_out+","+bytes_in+",";
    }
}
