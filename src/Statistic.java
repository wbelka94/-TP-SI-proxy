public class Statistic {
    private String domain;
    private int bytes;
    private int count;


    public Statistic(String line) {
        String[] parts = line.split(",");
        domain = parts[0];
        count = Integer.parseInt(parts[1]);
        bytes = Integer.parseInt(parts[2]);
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

    public void addBytes(int bytes){
        this.bytes += bytes;
    }

    public String toString(){
        return domain+","+count+","+bytes+",";
    }
}
