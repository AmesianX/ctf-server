/**
 * Author: Andrew Hood <andrewhood125@gmail.com>
 * Description: Store everything that a single player will need.
 * Copyright (c) 2014 Andrew Hood. All rights reserved.
 */
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;


class Player extends Locate implements Runnable
{
  int id;
  PrintWriter out;
  BufferedReader in;
  String btMAC;
  String username;
  Socket socket;
  boolean greeted, inLobby;

  Player(Socket socket)
  {
    this.socket = socket;
    try
    {
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      System.out.println("New player connected from IP: " + socket.getInetAddress());
      out.println("New player connected from IP: " + socket.getInetAddress());
    } catch(IOException ex) {
      System.err.println(ex.getMessage());
      System.exit(4);
    }
  }

  public double getLatitude()
  {
    return latitude;
  }

  public double getLongitude()
  {
    return longitude;
  }

  public void run()
  {
    System.out.println(this.toString() + "'s thread was started.");
    out.println(this.toString() + "'s thread was started.");

    try
    {
      String incomingCommunication;
      while(!(incomingCommunication = in.readLine()).equals("QUIT"))
        processCommand(incomingCommunication);
    } catch(IOException ex) {
      System.err.println(ex.getMessage());
      System.exit(5);
    } catch(Exception ex) {
      System.err.println(ex.getMessage());
      System.exit(6);
    }

    try
    {
      out.close();
      in.close();
      socket.close();
    } catch(IOException ex) {
      System.err.println(ex.getMessage());
      System.exit(6);
    }
  }

  private void readBluetoothMAC()
  {
    try 
    {
      String tempBtMac = in.readLine();
      System.out.println(this.toString() + " BT MAC: " + tempBtMac);
      btMAC = tempBtMac;
    } catch(Exception ex) {
      System.err.println(ex.getMessage());
      System.exit(10);
    }
  }

  private void readUsername()
  {
    try 
    {
      String tempUsername = in.readLine();
      System.out.println(this.toString() + " username: " + tempUsername);
      username = tempUsername;
    } catch(Exception ex) {
      System.err.println(ex.getMessage());
      System.exit(11);
    }
  } 

  public void readLocation()
  {
    try
    {
      String location = in.readLine();
      System.out.println(this.toString() + " location: " + location);
      String[] coordinates = location.split(",");
      if(coordinates.length != 2)
      {
        out.println("ERROR: GPS improperly formatted.");
        readLocation();
      } else {
        try 
        {
          latitude = Double.parseDouble(coordinates[0]);
          longitude = Double.parseDouble(coordinates[1]);
        } catch(NumberFormatException ex) {
          System.err.println(ex.getMessage());
          System.exit(20);
        }
      }
    } catch(Exception ex) {
      System.err.println(ex.getMessage());
      System.exit(13);
    }
  }
  
  private void processCommand(String com)
  {
    switch(com)
    {
      case "HELLO":
        if(!greeted)
        {
          greeted = true;
          out.println("Proceed with blutooth MAC.");
          readBluetoothMAC();
          out.println("Proceed with username.");
          readUsername();
          out.println("Proceed with location.");
          readLocation();
          out.println("Welcome " + username + ".");
        } else {
          out.println("..hi.");
        }
        break;

      case "CREATE": 
        if(!greeted)
        {
          out.println("ERROR: Need to greet first.");
        } else if(!inLobby) {
          double newLobbySize = 0;
          String newLobbyID = "";
          try 
          {
            out.println("Proceed with lobbyID.");
            newLobbyID = in.readLine();
            out.println("Proceed with arena size.");
            newLobbySize  = Double.parseDouble(in.readLine());
          } catch(NumberFormatException ex) {
            System.err.println(ex.getMessage());
            processCommand("CREATE");
          } catch(IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(25);
          }
          // Create a lobby with this player as the host
          CTFServer.createLobby(this, newLobbyID, newLobbySize);
          inLobby = true;
          out.println("Establishing a lobby.");
        } else if(!inLobby) {
          out.println("You are already in a lobby.");
        } else {
          out.println("ERROR: Something went wrong but I don't know what.");
          System.exit(17);
        }
        break;

      case "JOIN":
        if(!greeted)
        {
          out.println("ERROR: Need to greet first.");
        } else if(!inLobby) {
         try
         {
           String lobbyID;
           out.println("Proceed with lobby ID.");
           if(CTFServer.lobbyExists(lobbyID = in.readLine()))
           {
             CTFServer.joinLobby(this, lobbyID);
             inLobby = true;
             out.println("Joining lobby " + lobbyID + "...");
           } else {
             out.println("ERROR: Lobby not found.");
           }
         } catch(IOException ex) {
           System.err.println(ex.getMessage());
           System.exit(7);
         }
       } else if (inLobby){
         out.println("ERROR: You are already in a lobby.");
       } else {
         out.println("ERROR: Something went wrong but I don't know what.");
         System.exit(18);
       }
       break;


      default: out.println("Command not understood.");
    }
  }
}
