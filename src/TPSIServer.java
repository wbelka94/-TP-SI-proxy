import com.sun.deploy.util.BlackList;
import com.sun.net.httpserver.*;
import sun.misc.IOUtils;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

public class TPSIServer {
    private static HttpServer server;
    private static List BlackList;
    public static void main(String[] args) throws Exception {
        loadBlackList("src/blacklist.txt");
        int port = 8000;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());

       // System.out.println("Starting server on port: " + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            //System.out.println(String.valueOf(new InetSocketAddress(InetAddress.getLocalHost(),8000)));
            System.out.println(exchange.getRequestURI().toString());
            //System.out.println(exchange.getRemoteAddress().toString());

            if(onBlackList(exchange.getRequestURI().toString())){
                System.out.println("on blacklist");
                return;
            }

            URL url = new URL(exchange.getRequestURI().toString());
            String c_l = exchange.getRequestHeaders().getFirst("Content-Length");
            addStatistics(url.getHost(), c_l == null ? 0 : Integer.parseInt(c_l),true);
            HttpURLConnection yc = (HttpURLConnection) url.openConnection();
            yc.setInstanceFollowRedirects(false);
            yc.setRequestProperty("Via",server.getAddress().getHostString());
            yc.setRequestMethod(exchange.getRequestMethod());
            System.out.println(exchange.getRequestMethod());
            //yc.setRequestProperty("Client-IP",exchange.getRemoteAddress().toString());

            Map request_headers = exchange.getRequestHeaders();
            for (Iterator iterator = request_headers.keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                if(key == null ){
                    continue;
                }
                //System.out.print(key + " = ");
                List values = (List) request_headers.get(key);
                String v_str = "";
                for (int i = 0; i < values.size(); i++) {
                    Object v = values.get(i);
                    if(v == null){
                        continue;
                    }
                    if(i != 0){
                        v_str += ", ";
                    }
                    v_str += v.toString();
                }
                  // System.out.println(v_str);
                yc.setRequestProperty(key, v_str);
            }

            byte[] body = readFromStream(exchange.getRequestBody());
            System.out.println(exchange.getRequestBody().read());
            if(body.length > 0){
                yc.setDoOutput(true);
                OutputStream os = yc.getOutputStream();
                os.write(body);
                os.close();
            }
            InputStream is;
            if(yc.getResponseCode() > 400){
                is = yc.getErrorStream();
            }else{
                is = yc.getInputStream();
            }

            byte[] response_bytes = readFromStream(is);

            Map headerFields = yc.getHeaderFields();
            for (Iterator iterator = headerFields.keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                if(key == null || key.equals("Transfer-Encoding")){
                    continue;
                }
               // System.out.print(key + " = ");
                List values = (List) headerFields.get(key);
                String v_str = "";
                for (int i = 0; i < values.size(); i++) {
                    Object v = values.get(i);
                    if(v == null){
                        continue;
                    }
                    if(i != 0){
                        v_str += ", ";
                    }
                    v_str += v.toString();
                }
             //   System.out.println(v_str);
                exchange.getResponseHeaders().set(key, v_str);
            }

            addStatistics(url.getHost(),response_bytes.length,false);

            //byte[] response_bytes = response.getBytes();
            System.out.println(yc.getResponseCode());
            exchange.sendResponseHeaders(yc.getResponseCode(), response_bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response_bytes);
            os.close();

        }
    }

    private static byte[] readFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    private static boolean onBlackList(String url){
        for (Iterator iterator = BlackList.iterator(); iterator.hasNext();) {
            String element = (String) iterator.next();
            if(url.startsWith(element)){
                return true;
            }
        }
        return false;
    }

    private static void loadBlackList(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        BlackList = new ArrayList<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            BlackList.add(line);
        }
        bufferedReader.close();
    }

    private static void addStatistics(String domain, int bytes, boolean isRequest) throws IOException {
        FileReader fileReader = new FileReader("src/statistics.csv");
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<Statistic> Statistics = new ArrayList<Statistic>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            Statistics.add(new Statistic(line));
        }
        bufferedReader.close();
        if(Statistics.size() == 0){
            int count = isRequest ? 1 : 0;
            Statistics.add(new Statistic(domain+","+count+","+bytes+","));
            System.out.println(domain+","+count+","+bytes+",");
        }
        for (Iterator iterator = Statistics.iterator(); iterator.hasNext();) {
            System.out.println("for loop");
            Statistic element = (Statistic) iterator.next();
            if(element.checkDomain(domain)){
                if(isRequest){
                    element.addCount();
                }
                element.addBytes(bytes);
                break;
            }
            else if(!iterator.hasNext()){
                int count = isRequest ? 1 : 0;
                Statistics.add(new Statistic(domain+","+count+","+bytes+","));
                System.out.println(domain+","+count+","+bytes+",");
                break;
            }
        }

        PrintWriter writer = new PrintWriter("src/statistics.csv", "UTF-8");
        for (Iterator iterator = Statistics.iterator(); iterator.hasNext();) {
            Statistic element = (Statistic) iterator.next();
            writer.println(element.toString());
        }
        writer.close();

    }
}