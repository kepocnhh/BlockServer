package stan.block.server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import stan.*;
import stan.api.*;
import stan.block.api.*;

public class Jabber
        extends Thread
{
//Поля////////////////////////////////////////////////////////////////////////////////////
    private Socket socket;
    
//Конструкторы/////////////////////////////////////////////////////////////////////////////
    public Jabber(Socket s) throws IOException
    {
        socket = s;
        start(); // вызываем run()
    }
    
//Методы//////////////////////////////////////////////////////////////////////////////////
    public void run()
    {
        Messaging();
        try
        {
            socket.close();
        }
        catch (IOException ex)
        {
            StanLog.add_log(1,"run","Socket error"+"\n" +ex.getMessage());
        }
    }
    //Реализация обработки сообщений и бизнес-логика
    //ДО АВТОРИЗАЦИИ
    private void Messaging()
    {
            StanLog.add_log(0,"Messaging","going in ->");
        ObjectInputStream inputStream;
        ObjectOutputStream outputStream;
        try
        {
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException ex)
        {
            StanLog.add_log(1,"Messaging","Messaging error"+"\n" +ex.getMessage());
            return;//не позволяем программе дальше обрабатывать информацию
        }
            Object question = new Object();
            while (true)
            {
                try
                {
                    question = inputStream.readObject();
                }
                catch (IOException ex)
                {
                    StanLog.add_log(1,"Messaging","проблема с чтением объекта"+"\n" +ex.getMessage());
                    Server.Answer("Messaging",outputStream, (Object) new StanError("ReadObject"),"неудачная попытка ответить клиенту, что проблема с чтением объекта");//оповещаем клиента о том, что проблема с чтением объекта
                    return;//не позволяем программе дальше обрабатывать информацию
                }
                catch (ClassNotFoundException ex)
                {
                    StanLog.add_log(1,"Messaging","проблема с классами"+"\n" +ex.getMessage());
                    Server.Answer("Messaging", outputStream, (Object) new StanError("ClassNotFoundError"),"неудачная попытка ответить клиенту, что класс который получили не тот BaseMessage");//оповещаем клиента о том, что класс который получили не тот BaseMessage
                    return;//не позволяем программе дальше обрабатывать информацию
                }
                Class c = question.getClass();
                if (c == LastMessage.class)//принятый объект является уведомлением об окончании связи между клиентом и сервером
                {
                    StanLog.add_log(3,"Messaging","LastMessage");
                    return;
                }
                //если вы дошли до сюда, значит вы хотите работать с данными пользователей...
                //или вы неведома зверушка
                List<String> loginslist = Server.G_S_L("Messaging",Server.logins, outputStream);//логины
                if(loginslist == null)//и если чтение прошло успешно то продолжаем
                {
                    StanLog.add_log(1,"Messaging","loginslist == null");
                    Server.Answer("Messaging",outputStream, (Object) new StanError("ReadListErrorLogins"),"неудачная попытка ответить клиенту, что проблема с чтением списка аккаунтов");//оповещаем клиента о том, что проблема с чтением списка логинов
                    return;//а если не успешно, то не позволяем программе дальше обрабатывать информацию
                }
                List<String> userlist = Server.G_S_L("Messaging",Server.accpath, outputStream);//читаем лист объектов из файла с аккаунтами
                if(userlist == null)//и если чтение прошло успешно то продолжаем
                {
                    StanLog.add_log(1,"Messaging","userlist == null");
                    Server.Answer("Messaging",outputStream, (Object) new StanError("ReadListErrorAcc"),"неудачная попытка ответить клиенту, что проблема с чтением списка аккаунтов");//оповещаем клиента о том, что проблема с чтением списка аккаунтов
                    return;//а если не успешно, то не позволяем программе дальше обрабатывать информацию
                }
                if (c == Registration.class)//Добавление заявки на регистрацию
                {
                    StanLog.add_log(3,"Messaging","Registration");
                    if(Server.New_User((Registration) question, userlist, loginslist, outputStream))
                    {
                        continue;
                    }
                    else
                    {
                        return;//не позволяем программе дальше обрабатывать информацию
                    }
                }
                StanLog.add_log(1,"Messaging","WTF O_o" + " IN");
                return;//не позволяем программе дальше обрабатывать информацию
            }
    }
    //ПОСЛЕ АВТОРИЗАЦИИ
    private void AuthMessaging(UserMoreInfo umi, ObjectOutputStream outputStream, ObjectInputStream inputStream)
    {
        StanLog.add_log(0,umi.GetMail(),"Auth Successful");
        String dir = BlockMain.CreateLogDirName(umi, BlockServer.logpath);//директория сегодняшних логов
        new File(dir).mkdirs();//создаём эти директории
        StanLog.add_log(1,umi.GetMail(),"WTF O_o" + " OUT");
        return;//не позволяем программе дальше обрабатывать информацию
    }
}