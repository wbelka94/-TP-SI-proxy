import com.sun.net.httpserver.*;
import sun.misc.IOUtils;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TPSIServer {
    private static HttpServer server;
    public static void main(String[] args) throws Exception {
        int port = 8000;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());

       // System.out.println("Starting server on port: " + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            //System.out.println(String.valueOf(new InetSocketAddress(InetAddress.getLocalHost(),8000)));
            //System.out.println(exchange.getRequestURI().toString());
            //System.out.println(exchange.getRemoteAddress().toString());

            URL url = new URL(exchange.getRequestURI().toString());
            HttpURLConnection yc = (HttpURLConnection) url.openConnection();
            yc.setInstanceFollowRedirects(false);
            yc.setRequestProperty("Via",server.getAddress().getHostString());
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
            System.out.println(body);
            if(body.length > 0){
                yc.setDoOutput(true);
                OutputStream os = yc.getOutputStream();
                os.write(body);
                os.close();
            }

            byte[] response_bytes = readFromStream(yc.getInputStream());

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
}