
/**
 * ComLink listens, interprets, parses and sends input
 *  and output to and from a player.
 * 
 * @author Andrew Hood 
 * @version 0.1
 */

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ComLink
{
    /**
     * Instance variables
     */
    private BufferedReader in;
    private Gson gson;
    private PrintWriter out;
    private Socket socket;
    private Player player;
    
    public ComLink(Socket socket, Player player)
    {
        this.socket = socket;
        this.player = player;
        try
        {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("New player connected from IP: " + socket.getInetAddress());
        } catch(IOException ex) {
            System.err.println(ex.getMessage());
            JsonObject jo = new JsonObject();
            jo.addProperty("ACTION", "LOG");
            jo.addProperty("LEVEL", "ERROR");
            jo.addProperty("PAYLOAD", "I caught an IOException in the ComLink constructor, this is what we know: " + ex.getMessage());
            this.send(jo);
        }
        gson = new Gson();
    }
    
    public void close() throws IOException
    {
        out.close();
        in.close();
        socket.close();
    } 
    
    public void parseCommunication(JsonObject jo) throws IllegalStateException
    {
        JsonElement action = jo.get("ACTION");
        switch(action.getAsString())
        {
            case "HELLO":
            if(!player.isInitialized())
            {
                JsonElement bluetooth = jo.get("BLUETOOTH");
                player.setMyBluetoothMac(bluetooth.getAsString());
                JsonElement username = jo.get("USERNAME");
                player.setUsername(username.getAsString());
            } else {
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "INFO");
                jobj.addProperty("PAYLOAD", "..hi.");
                send(jobj);
            }
            break;

            case "CREATE": 
            if(!player.isInitialized())
            {
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "ERROR");
                jobj.addProperty("PAYLOAD", "Need to greet first.");
                send(jobj);
            } else if(!player.isInLobby()) {
                JsonElement location = jo.get("LOCATION");
                try 
                {
                    player.setPoint(location.getAsString());
                } catch(PointException ex) {
                    System.err.println(ex.getMessage());     
                }
                JsonElement arenaSize = jo.get("SIZE");
                double newLobbySize = arenaSize.getAsDouble();
                player.setLobby(new Lobby(player, newLobbySize));
                
                JsonObject job = new JsonObject();
                job.addProperty("ACTION", "CREATE");
                job.addProperty("ID", player.getLobby().getLobbyID());
                job.addProperty("SUCCESS", "TRUE");
                send(job);
            } else if(player.isInLobby()) {
                JsonObject job = new JsonObject();
                job.addProperty("ACTION", "LOG");
                job.addProperty("LEVEL", "ERROR");
                job.addProperty("PAYLOAD", "You are already in a lobby.");
                send(job);
            } else {
                System.out.println("ERROR: Something went wrong but I don't know what.");
            }
            break;

            case "START":
            if(!player.isInLobby())
            {
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "ERROR");
                jobj.addProperty("PAYLOAD", "Need to greet first.");
                send(jobj);
            } else if(!player.isInLobby()) {
                JsonObject job = new JsonObject();
                job.addProperty("ACTION", "LOG");
                job.addProperty("LEVEL", "ERROR");
                job.addProperty("PAYLOAD", "Need to be in lobby.");
                send(job);
            } else if(!player.getLobby().isLobbyLeader(player)) {
                JsonObject job = new JsonObject();
                job.addProperty("ACTION", "LOG");
                job.addProperty("LEVEL", "ERROR");
                job.addProperty("PAYLOAD", "Only the lobby leader can start the game.");
                send(job);
            } else {
                player.getLobby().startGame();
            }
            break;
            case "GPS":
            // GPS is used to accept location updates from clients
            if(!player.isInitialized())
            {
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "ERROR");
                jobj.addProperty("PAYLOAD", "Need to greet first.");
                send(jobj);
            } else if(!player.isInLobby()) {
                JsonObject job = new JsonObject();
                job.addProperty("ACTION", "LOG");
                job.addProperty("LEVEL", "ERROR");
                job.addProperty("PAYLOAD", "Need to be in lobby.");
                send(job);
            } else if(player.getLobby().getGameState()!= Lobby.IN_PROGRESS) {
                JsonObject job = new JsonObject();
                job.addProperty("ACTION", "LOG");
                job.addProperty("LEVEL", "ERROR");
                job.addProperty("PAYLOAD", "The game must be in progress.");
                send(job);
            } else {
                JsonElement location = jo.get("LOCATION");
                try 
                {
                    player.setPoint(location.getAsString());
                } catch(PointException ex) {
                    System.err.println(ex.getMessage());     
                }
                player.getLobby().playerUpdate(player);
            }
            break;
            case "JOIN":
            if(!player.isInitialized())
            {
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "ERROR");
                jobj.addProperty("PAYLOAD", "Need to greet first.");
                send(jobj);
            } else if(!player.isInLobby()) {
                JsonElement location = jo.get("LOCATION");
                try 
                {
                    player.setPoint(location.getAsString());
                } catch(PointException ex) {
                    System.err.println(ex.getMessage());     
                }
                
                JsonElement id = jo.get("ID");
                if(Lobby.isJoinable(id.getAsString()))
                {
                    // Success!
                    player.setLobby(Lobby.addPlayerToLobby(player, id.getAsString()));
                    JsonObject temp = new JsonObject();
                    temp.addProperty("ACTION","JOIN");
                    temp.addProperty("SUCCESS","TRUE");
                    send(temp);
                } else {
                    JsonObject jobj = new JsonObject();
                    jobj.addProperty("ACTION", "LOG");
                    jobj.addProperty("LEVEL", "ERROR");
                    jobj.addProperty("PAYLOAD", "Lobby not found.");
                    send(jobj);
                }
            } else if (player.isInLobby()){
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "ERROR");
                jobj.addProperty("PAYLOAD", "You are already in a lobby.");
                send(jobj);
            } else {
                System.err.println("ERROR: Something went wrong but I don't know what.");
            }
            break;

            case "LOBBY": 
            if(!player.isInitialized())
            {
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "ERROR");
                jobj.addProperty("PAYLOAD", "Need to greet first.");
                send(jobj);
            } else if(!player.isInLobby()) {
                send(Lobby.listLobbies());
            } else if(player.isInLobby()) {
                out.println(player.getLobby().toString());
            }
            break;

            case "LEAVE":
            if(!player.isInitialized())
            {
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "ERROR");
                jobj.addProperty("PAYLOAD", "Need to greet first.");
                send(jobj);
            } else if (!player.isInLobby()) {
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "ERROR");
                jobj.addProperty("PAYLOAD", "You're not in a lobby.");
                send(jobj);
            } else if(player.isInLobby()) {
                Lobby.removePlayerFromLobby(player, player.getLobby());
            } else {
                System.err.println("ERROR: Something went wrong but I don't know what.");
            }
            break;
            
            case "DROP":
            if(!player.isInitialized())
            {
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "ERROR");
                jobj.addProperty("PAYLOAD", "Need to greet first.");
                send(jobj);
            } else if (!player.isInLobby()) {
                JsonObject jobj = new JsonObject();
                jobj.addProperty("ACTION", "LOG");
                jobj.addProperty("LEVEL", "ERROR");
                jobj.addProperty("PAYLOAD", "You're not in a lobby.");
                send(jobj);
            } else if(player.isInLobby()) {
                JsonElement bluetooth = jo.get("BLUETOOTH");
                player.setObservedBluetoothMac(bluetooth.getAsString());
                player.getLobby().playerUpdate(player);
            } else {
                System.err.println("ERROR: Something went wrong but I don't know what.");
            } 

            default: JsonObject jobj = new JsonObject();
                        jobj.addProperty("ACTION", "LOG");
                        jobj.addProperty("LEVEL", "ERROR");
                        jobj.addProperty("PAYLOAD", "Command not understood.");
                        send(jobj);
        }
    }
    /*
    private void readBluetoothMAC()
    {
        try 
        {
            String tempBtMac = in.readLine();
            tempBtMac.toUpperCase();
            if(tempBtMac.matches(Player.MACPAT))
            {
                System.out.println(this.toString() + " BT MAC: " + tempBtMac);
                player.setMyBluetoothMac(tempBtMac);
            }else
            {
                readBluetoothMAC();
            }

        } catch(Exception ex) {
            System.err.println(ex.getMessage());
            System.exit(10);
        }
    }*/
    
    public JsonObject readLine() throws IOException
    {
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(in.readLine());
        JsonObject jo = je.getAsJsonObject();
        return jo;
    }
    
    public void send(JsonObject obj)
    {
        out.println(gson.toJson(obj));
    }
}
