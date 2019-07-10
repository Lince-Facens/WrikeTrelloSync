package com.guichaguri.wriketrellosync;

import com.guichaguri.wriketrellosync.history.History;
import com.guichaguri.wriketrellosync.history.HistoryCard;
import fi.iki.elonen.NanoHTTPD;
import kong.unirest.Unirest;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class WebHookSync extends NanoHTTPD {

    public static void main(String[] args) throws IOException {
        Unirest.config().enableCookieManagement(false);

        File config = new File(Utils.CONFIG_FILE);
        File database = new File(Utils.DATABASE_FILE);

        ISyncManager[] managers = null;

        if (config.exists()) {
            managers = Config.load(config);
        }

        if (managers == null || managers.length == 0) {
            managers = Config.wizard(config);
        }

        History history = new History();

        if (database.exists()) {
            history.load(database);
        }

        String hostname = System.getProperty("webhook.hostname");
        String portStr = System.getProperty("webhook.port");

        if (hostname == null) {
            System.out.println("The hostname is not set. The server will listen to any hostname.");
            System.out.println("Set a hostname with -Dwebhook.hostname=127.0.0.1");
        }

        if (portStr == null) {
            System.out.println("The port is not set. The server will listen to the port 8091.");
            System.out.println("Set a port with -Dwebhook.port=8091");
            portStr = "8091";
        }

        WebHookSync sync = new WebHookSync(hostname, Integer.parseInt(portStr), managers, history, database);
        sync.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

        if (hostname != null) {
            System.out.println("The server is listening " + hostname + ":" + portStr);
        } else {
            System.out.println("The server is listening the port " + portStr);
        }

        DiffSync.processTimer(managers, history, database);
    }

    private final History history;
    private final ISyncManager[] managers;
    private final File database;

    private WebHookSync(String hostname, int port, ISyncManager[] managers, History history, File database) {
        super(hostname, port);
        this.history = history;
        this.managers = managers;
        this.database = database;
    }

    private Response newResponse(Response.Status status) {
        return newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, status.getDescription());
    }

    private void processHook(ISyncManager manager, String cardId) {
        String slug = manager.getSlug();
        Card card = manager.getCard(cardId);
        HistoryCard historyCard = null;

        for (HistoryCard hc : history.cards) {
            String id = hc.ids.get(slug);

            if (id != null && id.equals(cardId)) {
                historyCard = hc;
                break;
            }
        }

        if (card == null) {

            if (historyCard != null) {
                // Card Removed
                DiffSync.removeCard(managers, manager, historyCard);
            }

        } else if (historyCard == null) {

            // Card added
            DiffSync.addCard(managers, manager, card);

        } else {

            // Card updated
            DiffSync.updateCard(managers, manager, card, historyCard);

        }

        try {
            // Saves the history file with the updated data
            history.save(database);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();

        // The method needs to be either HEAD or POST
        // We don't care about anything else
        if (method != Method.HEAD && method != Method.POST) {
            return newResponse(Response.Status.BAD_REQUEST);
        }

        // Remove any trailing slash from the url
        String uri = session.getUri().toLowerCase().replace("/", "");

        for (ISyncManager manager : managers) {
            String slug = manager.getSlug().toLowerCase();
            if (!uri.equals(slug)) continue;

            if (method == Method.HEAD) {
                // The head method is used only for checking whether the server exists
                return newResponse(Response.Status.OK);
            }

            try {
                HashMap<String, String> bodyData = new HashMap<>();
                session.parseBody(bodyData);
                String cardId = manager.handleWebhook(bodyData.get("postData"));

                if (cardId == null) {
                    return newResponse(Response.Status.BAD_REQUEST);
                }

                processHook(manager, cardId);

                return newResponse(Response.Status.OK);
            } catch(Exception ex) {
                ex.printStackTrace();
                return newResponse(Response.Status.INTERNAL_ERROR);
            }
        }

        // If none of the managers slug match, we'll return a 404
        return newResponse(Response.Status.NOT_FOUND);
    }


}
