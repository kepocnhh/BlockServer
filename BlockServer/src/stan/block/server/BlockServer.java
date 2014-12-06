package stan.block.server;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import stan.api.*;

public class BlockServer
{
//Поля
    static public String bugreportmail;
    static public String version;
    static public String logpath;
    static public String port;
    static public Date actual_date;
    
//Методы
    static public void linux_debug()
    {
        Server.SetProp("/home/toha/StanleySpace/Accounts",
            "/home/toha/StanleySpace/Logs");
        version = "0.0.1";
        bugreportmail = "bugreport.sp@gmail.com";
        logpath = "/home/toha/StanleySpace/BlockNote/";
        port = "5555";
        actual_date = new Date();
    }
    static public void SetProp(String v,String brm,String l,String p,Date d)
    {
        version=v;
        bugreportmail = brm;
        logpath = l;
        port = p;
        actual_date = d;
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
        StanLog.add_log(3, "BlockServer", "\n\n-\tServer Started");
        try
        {
            while (true)
            {
                Socket socket = s.accept();
                try
                {
                    StanLog.add_log(3, "BlockServer", "Goto --> Jabber");
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