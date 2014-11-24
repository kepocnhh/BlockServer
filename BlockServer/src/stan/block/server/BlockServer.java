package stan.block.server;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class BlockServer
{
    static public String accpath;
    static public String logins;
    static public String bugreportmail;
    static public String version;
    static public String logpath;
    static public String port;
    static public Date actual_date;
    //
    static public String[] results = (
            "[ >OK< ]"//0
            +"\t"+
            "[ WTF O_o ]"//1
            +"\t"+
            "[ warning ]"//2
            +"\t"+
            "[ info ]"//3
            ).split("\t");
    //
    static public void linux_debug()
    {
        accpath = "/home/toha/StanleySpace/accounts";
        logins = "/home/toha/StanleySpace/accounts";
        version = "/home/toha/StanleySpace/accounts";
        bugreportmail = "/home/toha/StanleySpace/accounts";
        logpath = "/home/toha/StanleySpace/accounts";
        port = "5555";
        actual_date = new Date();
    }
    static public void SetProp(String a,String lo,String v,String brm,String l,String p,Date d)
    {
        accpath=a;
        logins = lo;
        version=v;
        bugreportmail = brm;
        logpath = l;
        port = p;
        actual_date = d;
    }
    //
    //добавление строки с подписью в отладочный лог
    static public  void add_log(int n, String u, String s)
    {
        String text = "[" + date_to_string(new Date())  + "]" + " " +results[n]+ " " + "[" +u+ "]" + " "  + s;
        System.out.println(text);
    }
    //конвертация даты
    static public String date_to_string(Date d)
    {
        return "" + (d.getYear()+1900) + "." + (d.getMonth()+1) + "." + d.getDate()
                + "|" + 
                BlockServer.minutes(d.getHours()+"")+ ":" +BlockServer.minutes(d.getMinutes()+"")+ ":" +BlockServer.minutes(d.getSeconds()+"");
    }
    static public String minutes(String s)
    {
        if (s.length() == 1)
        {
            s = "0" + s;
        }
        return s;
    }
    //
    public static void main(String[] args) throws FileNotFoundException, IOException
    {
        System.out.println("BlockServer |m/_");
        linux_debug();
            String debugpath = logpath + "DEBUG_" + (actual_date.getYear()+1900) + "." + (actual_date.getMonth()+1) + "." + actual_date.getDate()+ ".txt";
            System.out.println("Log will write to [ " + debugpath + " ]");
        PrintStream st = new PrintStream(new FileOutputStream(debugpath,true));
        System.setErr(st);
        System.setOut(st);
        ServerSocket s = new ServerSocket(Integer.parseInt(port));
        BlockServer.add_log(3, "BlockServer", "\n\n-\tServer Started");
        try
        {
            while (true)
            {
                Socket socket = s.accept();
                try
                {
                    BlockServer.add_log(3, "BlockServer", "Goto --> Jabber");
                    new Jabber(socket);
                }
                catch (IOException e)
                {
                    socket.close();
                }
            }
        }
        finally
        {
            s.close();
        }
    }
}