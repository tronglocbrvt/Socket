package httpServer;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Main implements Runnable {
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String INFO_FILE = "info.html";
    static final File WEB_ROOT = new File("./template");
    static final int PORT = 80;
    private Socket socket;

    public Main(Socket c) {
        socket = c;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Server starting...\n");
        final InetAddress addr = InetAddress.getLocalHost();
        System.out.println(addr);
        final ServerSocket serverSocket = new ServerSocket(PORT, 50, addr);
        while (true) {
            Main myServer = new Main(serverSocket.accept());
            Thread thread = new Thread(myServer);
            thread.start();
        }
    }

    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileName = "";
        String method = "";
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // for headers writing
            out = new PrintWriter(socket.getOutputStream());
			
            // for requested data writing
            dataOut = new BufferedOutputStream(socket.getOutputStream());

            // get method and filename
            if (in.ready()) {
                String input = in.readLine();
                if (input != null) {
                    StringTokenizer parse = new StringTokenizer(input);
                    method = parse.nextToken().toUpperCase();
                    fileName = parse.nextToken().toLowerCase();
                    fileName = fileName.replaceAll("%","%20");
                }
                // only GET and POST method is supported
                // GET or POST method
                if (fileName.endsWith("/")) {
                    fileName += DEFAULT_FILE;
                }

                if (method.equals("GET")) { 
                    getRespone(out, dataOut, fileName);
                } else //method POST
                {
                    String headerLine = "";
                    while ((headerLine = in.readLine()).length() != 0) {
                    }

                    StringBuilder payload = new StringBuilder();
                    while (in.ready()) {
                        payload.append((char) in.read());
                    }
                    post(out, dataOut, fileName, payload.toString());
                }
            }
        } catch (FileNotFoundException e) {
            try {
                getRespone(out, dataOut, FILE_NOT_FOUND);
            } catch (IOException ioe) {
                System.err.println("Error with file not found exception : " + ioe.getMessage());
            }

        } catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        } finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                socket.close(); // close socket connection
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }
        }
    }

    private static byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }

    private static String getContentType(String fileName) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("jpg", "image/jpg");
        map.put("png","image/png");
        map.put("jpeg","image/jpeg");
        map.put("pdf","application/pdf");
        map.put("pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation");
        map.put("ppt","application/vnd.ms-powerpoint");
        map.put("docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        map.put("doc","application/msword");
        map.put("xls","application/vnd.ms-excel");
        map.put("xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        map.put("zip","application/zip");
        map.put("txt","text/plain");
        map.put("rar","application/vnd.rar");
        map.put("mp3","audio/mpeg");
        map.put("mp4","video/mp4");

        String content = "";
        fileName = fileName.substring(fileName.lastIndexOf(".") + 1);
        content = map.get(fileName);
        if (content == null)
            content = "text/html";
        return content;
    }

    private static void post(PrintWriter out, OutputStream dataOut, String fileName, String input) throws IOException {
        if (fileName.compareTo("/login") == 0) {
            if (input.compareTo("userName=admin&passWord=admin") == 0) {
                redirectRespone(out, dataOut, INFO_FILE);
            } else
                redirectRespone(out, dataOut, FILE_NOT_FOUND);
        } else {
            if (input != null)
                input = input.substring(input.indexOf("=") + 1);
            input = input.replaceAll("%2F", "/");
            redirectRespone(out, dataOut, input.substring(1));
        }
    }

    private static void fileNotFound(PrintWriter out, OutputStream dataOut) throws IOException {
        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileData = readFileData(file, fileLength);

        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server: WebServer of Linh and Loc");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
        out.println(); // blank line between headers and content
        out.flush(); // flush character output stream buffer

        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
    }

    private static String getByteSize(long size, String fileName)
    {
        String result = "";
        String[] post = {"B", "KB", "MB", "GB", "TB"};
        int i = 0;
        while(size > 1024)
        {
            size /= 1024;
            i++;
        }
        if (i > 4)
            return "";
        return String.valueOf(size) + post[i];
    }

    private static String toHtml(String fileName) {
        String Json = "";
        String[] list = null;
        File file = new File(WEB_ROOT, fileName);
        list = file.list();
        String parent = file.getParent();
        parent = parent.replaceAll("\\\\", "/");

        if (fileName.equals("/files") || fileName.equals("\\files")){
            parent = "/files";
        }
        else {
            parent = parent.substring(10);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm aaa");
        Json += "<hr><a href=\"" + parent + "\">[To parent Directory]</a><br><table><thead><tr><th width=\"50%\">Name</th>" +
                "<th width=\"30%\">Last Modified</th>" +
                "<th>Size</th>" +
                "<th>Download</th></thead><tbody>";
        if (list != null)
            for (String pathname : list){
                pathname = fileName +"/"+ pathname;
                file = new File(WEB_ROOT, pathname);
                Json += "<tr><td><a href=\""+ pathname +"\"";
                if(file.isFile())
                    Json += "download=\""+pathname.substring(pathname.lastIndexOf("/") + 1)+"\"";
                Json += ">"+ pathname.substring(pathname.lastIndexOf("/") + 1) +"</td>" +
                        "<td>"+dateFormat.format(file.lastModified())+"</td><td>";
                if(file.isFile())
                    Json += getByteSize(file.length(), fileName)+"</td><td><form method=\"post\" action=\"/files\">" +
                            "<input type=\"hidden\" name=\"fileName\" value=\"" + pathname + "\">" +
                            "<input type=\"submit\" value=\"Download\"></form>";
                else
                    Json += "dir";
                Json += "</td></tr>";
            }

        Json = "<html><head><title>" + fileName + "</title></head><body><h1>"+fileName+"</h1>" + Json + "</tbody></table><hr>" +
                "<script src=\"https://code.jquery.com/jquery-3.5.1.min.js\" " +
                "integrity=\"sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0=\" crossorigin=\"anonymous\"></script>"+
                "<script type=\"text/javascript\" src=\"static/script.js\"></script>"+
                "</body></html>";
        return Json;
    }

    private static void getRespone(PrintWriter out, OutputStream dataOut, String fileName) throws IOException {
        if (fileName.equals("/404.html")) {
            fileNotFound(out, dataOut);
            return;
        }
        if(fileName.equals("/files.html"))
            fileName = "/files";
        String content = getContentType(fileName);
        int fileLength = 0;
        byte[] fileData = new byte[1];

        File file = new File(WEB_ROOT, fileName);
        if (file.isFile()){
            fileLength = (int) file.length();
            fileData = readFileData(file, fileLength);
        }
        else {
            String html = toHtml(fileName);
            fileData = html.getBytes();
            fileLength = fileData.length;
        }

        out.println("HTTP/1.1 200 OK");
        out.println("Server: WebServer of Linh and Loc");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);

        out.println(); // blank line between headers and content
        out.flush(); // flush character output stream buffer

        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
    }

    private static void redirectRespone(PrintWriter out, OutputStream dataOut, String fileName) throws IOException {
        String content = getContentType(fileName);
        int fileLength = 0;
        byte[] fileData = new byte[1];

        out.println("HTTP/1.1 301 Found Move Permanently");
        out.println("Server: WebServer of Linh and Loc");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Location: /" + fileName);
        out.println("Content-length: " + fileLength);
        out.println(); // blank line between headers and content
        out.flush();

        dataOut.flush();
        dataOut.write(fileData, 0, fileLength);
    }
}