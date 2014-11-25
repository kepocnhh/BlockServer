package stan.block.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import stan.*;

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
            BlockServer.add_log(1,"run","Socket error"+"\n" +ex.getMessage());
        }
    }
    private boolean Answer(String from, ObjectOutputStream os, Object answer, String submessage)
    {
        try
        {
            os.writeObject(answer);//и ответить соответственно клиенту
            return false;
        }
        catch (IOException ex)
        {
            BlockServer.add_log(1,from,"(Answer) проблема с записью объекта" +"\n" +
                    submessage+"\n" +ex.toString());
        }
        return true;//не позволяем программе дальше обрабатывать информацию
    }
    //Реализация обработки сообщений и бизнес-логика
    //ДО АВТОРИЗАЦИИ
    private void Messaging()
    {
            BlockServer.add_log(0,"Messaging","going in ->");
        ObjectInputStream inputStream;
        ObjectOutputStream outputStream;
        try
        {
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException ex)
        {
            BlockServer.add_log(1,"Messaging","Messaging error"+"\n" +ex.getMessage());
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
                    BlockServer.add_log(1,"Messaging","проблема с чтением объекта"+"\n" +ex.getMessage());
                    Answer("Messaging",outputStream, (Object) new StanError("ReadObject"),"неудачная попытка ответить клиенту, что проблема с чтением объекта");//оповещаем клиента о том, что проблема с чтением объекта
                    return;//не позволяем программе дальше обрабатывать информацию
                }
                catch (ClassNotFoundException ex)
                {
                    BlockServer.add_log(1,"Messaging","проблема с классами"+"\n" +ex.getMessage());
                    Answer("Messaging", outputStream, (Object) new StanError("ClassNotFoundError"),"неудачная попытка ответить клиенту, что класс который получили не тот BaseMessage");//оповещаем клиента о том, что класс который получили не тот BaseMessage
                    return;//не позволяем программе дальше обрабатывать информацию
                }
                Class c = question.getClass();
                if (c == LastMessage.class)//принятый объект является уведомлением об окончании связи между клиентом и сервером
                {
                    BlockServer.add_log(3,"Messaging","LastMessage");
                    return;
                }
                //если вы дошли до сюда, значит вы хотите работать с данными пользователей...
                //или вы неведома зверушка
                List<String> loginslist = G_S_L("Messaging",BlockServer.logins, outputStream);//логины
                if(loginslist == null)//и если чтение прошло успешно то продолжаем
                {
                    BlockServer.add_log(1,"Messaging","loginslist == null");
                    Answer("Messaging",outputStream, (Object) new StanError("ReadListErrorLogins"),"неудачная попытка ответить клиенту, что проблема с чтением списка аккаунтов");//оповещаем клиента о том, что проблема с чтением списка логинов
                    return;//а если не успешно, то не позволяем программе дальше обрабатывать информацию
                }
                List<String> userlist = G_S_L("Messaging",BlockServer.accpath, outputStream);//читаем лист объектов из файла с аккаунтами
                if(userlist == null)//и если чтение прошло успешно то продолжаем
                {
                    BlockServer.add_log(1,"Messaging","userlist == null");
                    Answer("Messaging",outputStream, (Object) new StanError("ReadListErrorAcc"),"неудачная попытка ответить клиенту, что проблема с чтением списка аккаунтов");//оповещаем клиента о том, что проблема с чтением списка аккаунтов
                    return;//а если не успешно, то не позволяем программе дальше обрабатывать информацию
                }
                if (c == Registration.class)//Добавление заявки на регистрацию
                {
                    BlockServer.add_log(3,"Messaging","Registration");
                    Registration reg = (Registration) question;
                    Login lgn = Get_user(reg.GetNewLogin().GetMail(), loginslist);//попытка добыть объект данных пользователя по заданному логину
                    if(lgn == null)//если не добыли (это хорошо, потому что мыло не занято)
                    {
                        
                        lgn = reg.GetNewLogin();
                        UserMoreInfo umi = reg.GetNewUMI();
                        userlist.add(umi.toString());//добавляем в список новобранца
                        loginslist.add(lgn.toString());//добавляем в список новобранца
                        //и записываем в файл
                        if(A_S_L("Messaging",userlist, BlockServer.accpath, outputStream))//и если запись прошла успешно то продолжаем
                        {
                            BlockServer.add_log(1,"Messaging","Add userlist failed");
                            Answer("Messaging",outputStream, (Object) new StanError("WriteListErrorAcc"),"неудачная попытка ответить клиенту, что проблема с записью списка аккаунтов");//оповещаем клиента о том, что проблема с записью списка аккаунтов
                            return;//а если не успешно, то не позволяем программе дальше обрабатывать информацию
                        }
                        if(A_S_L("Messaging",loginslist, BlockServer.logins, outputStream))//и если запись прошла успешно то продолжаем
                        {
                            BlockServer.add_log(1,"Messaging","Add loginslist failed");
                            Answer("Messaging",outputStream, (Object) new StanError("WriteListErrorLogins"),"неудачная попытка ответить клиенту, что проблема с записью списка логинов");//оповещаем клиента о том, что проблема с записью списка логинов
                            return;//а если не успешно, то не позволяем программе дальше обрабатывать информацию
                        }
                        BlockServer.add_log(0,"Messaging","Registration successful "+lgn.GetMail());
                        if(Answer("Messaging",outputStream, (Object) new Message("RegistrationSuccessful"),"неудачная попытка ответить клиенту что всё прошло успешно"))//оповещаем клиента о том, что всё прошло успешно
                        {
                            return;//не позволяем программе дальше обрабатывать информацию
                        }
                        BlockServer.add_log(0,"Messaging","Registration request send");
                    }
                    else//а если достали
                    {
                        BlockServer.add_log(2,"Messaging","Mail is used "+lgn.GetMail());//такой электронный адресс уже используется
                        if(Answer("Messaging",outputStream, (Object) new StanError("MailIsUsed"),"неудачная попытка ответить клиенту что такой электронный адресс уже используется"))//оповещаем клиента о том, что такой электронный адресс уже используется
                        {
                            return;//не позволяем программе дальше обрабатывать информацию
                        }
                        BlockServer.add_log(0,"Messaging","Mail is used send");
                    }
                    continue;
                }
                BlockServer.add_log(1,"Messaging","WTF O_o" + " IN");
                return;//не позволяем программе дальше обрабатывать информацию
            }
    }
    private List<String> G_S_L(String from,String path, ObjectOutputStream os)
    {
        List<String> bmlist;
        try
        {
            bmlist = Get_String_List(path); //список с данными пользователей
            if(bmlist == null)//если списка не существует
            {
                bmlist = new ArrayList();//его нужно создать
                File f = new File(path);
                if(!f.exists())
                {
                    f.createNewFile();
                }
                Add_String_List(bmlist, path);//и записать в файл
            }
            return bmlist;//всё круто
        }
        catch (IOException ex)
        {
            BlockServer.add_log(1,from,"(G_S_L) проблема с чтением из файла" +"\n" +
                    " неудачная попытка получить список пользователей"+"\n" +ex.toString());
            Answer(from, os, (Object) new StanError("ReadAllObjectsError"),"неудачная попытка ответить клиенту что чтение из файла не удалось");//оповещаем клиента о том, что неудачная попытка получить список пользователей
        }
        catch (ClassNotFoundException ex)
        {
            BlockServer.add_log(1,from,"(G_S_L) проблема с классами" +"\n" +
                    " класс который достаём не тот BaseMessage"+"\n" +ex.toString());
            Answer(from, os, (Object) new StanError("ClassNotFoundError"),"неудачная попытка ответить клиенту что класс который получили не тот BaseMessage");//оповещаем клиента о том, что класс который получили не тот BaseMessage
        }
        return null;//не позволяем программе дальше обрабатывать информацию
    }
    private boolean A_S_L(String from,List<String> bmlist, String path, ObjectOutputStream os)
    {
        try
        {
            Add_String_List(bmlist, path);//и записать в файл
            return false;
        }
        catch (IOException ex)
        {
            BlockServer.add_log(1,from,"(Add_String_List) проблема с записью в файл для регистрации" +"\n" +
                    "неудачная попытка записать список регистрирующихся"+"\n" +ex.toString());
            Answer(from, os, (Object) new StanError("WriteAllObjectsError"),"попытка ответить клиенту что запись в файл не удалось");//оповещаем клиента о том, что неудачная попытка записать список объектов лога
        }
        catch (ClassNotFoundException ex)
        {
            BlockServer.add_log(1,from,"(Add_String_List) проблема с классами" +"\n" +
                    "класс который получили не тот String"+"\n" +ex.toString());
            Answer(from, os, (Object) new StanError("ClassNotFoundError"),"неудачная попытка ответить клиенту что класс который получили не тот String");//оповещаем клиента о том, что класс который получили не тот BaseMessage
        }
        return true;//не позволяем программе дальше обрабатывать информацию
    }
    //String/////////////////////////////////////////////////////////////////////////
    static public List<String> Get_String_List(String file) throws IOException, ClassNotFoundException
    {
        List<String> loglist = null;
            try
            {
                FileInputStream fis = new FileInputStream(file);
                    ObjectInputStream read = new ObjectInputStream(fis);
                            loglist = (List) read.readObject();
                    read.close();
                fis.close();
            }
            catch (IOException ex)
            {
            }
        return loglist;
    }
    //Файл нужно перезаписывать новым листом/////////////////////////////////////////////
    static public void Add_String_List(List<String> loglist, String path) throws IOException, FileNotFoundException, ClassNotFoundException
    {
            FileOutputStream fos = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(loglist);
            oos.close();
            fos.close();
    }
    //user/////////////////////////////////////////////////////////////////////////////////
    static public Login Get_user(String email, List<String> accounts)
    {
        Login tmp;
        for (String string : accounts)
        {
            tmp = new Login(string);
            if (tmp.GetMail().equalsIgnoreCase(email))    //mail is used
            {
                return tmp;
            }
        }
        return null;
    }
}