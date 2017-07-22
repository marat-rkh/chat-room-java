package chatroom.client;

public class ChatRoomClient {
    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: program_name host_name");
            return;
        }
        Client client = new Client();
        client.run(args[0]);
    }
}
