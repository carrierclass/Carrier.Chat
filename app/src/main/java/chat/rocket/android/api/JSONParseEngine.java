package chat.rocket.android.api;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import chat.rocket.android.content.RocketChatDatabaseHelper;
import chat.rocket.android.model.Message;
import chat.rocket.android.model.Room;
import chat.rocket.android.model.User;

public class JSONParseEngine {
    protected final Context mContext;

    public JSONParseEngine(Context context) {
        mContext = context;
    }

    public void parseMessage(JSONObject message) throws JSONException {
        parseMessage(message, null);
    }

    public void parseMessage(JSONObject message, SQLiteDatabase db) throws JSONException {
        //"_id":"X6TG3j4pNGA6HBu8Q","rid":"PTKTpXLoo9XTF62ij","msg":"hogehoge","ts":{"$date":1448132287372},"u":{"_id":"vdsT864GD3CkZPr5K","username":"test.user.2-1"}}
        final String messageId = message.getString("_id");
        Message m;
        if (db != null) {
            m = Message.getById(db, messageId);
        } else {
            m = RocketChatDatabaseHelper.read(mContext, new RocketChatDatabaseHelper.DBCallback<Message>() {
                @Override
                public Message process(SQLiteDatabase db) throws Exception {
                    return Message.getById(db, messageId);
                }
            });
        }

        if (m == null) {
            m = new Message();
            m.id = messageId;
        }
        m.roomId = message.getString("rid");
        if (!message.isNull("msg")) m.content = message.getString("msg");
        m.timestamp = message.getJSONObject("ts").getLong("$date");
        m.type = Message.Type.getType(message.isNull("t") ? "" : message.getString("t"));

        if (!message.isNull("groupable")) {
            if (message.getBoolean("groupable")) m.flags |= Message.FLAG_GROUPABLE;
            else m.flags &= ~Message.FLAG_GROUPABLE;
        }

        final JSONObject user = message.getJSONObject("u");
        final String userId = user.getString("_id");
        final String userName = user.getString("username");
        User _u;
        if (db != null) {
            _u = User.getById(db, userId);
        } else {
            _u = RocketChatDatabaseHelper.read(mContext, new RocketChatDatabaseHelper.DBCallback<User>() {
                @Override
                public User process(SQLiteDatabase db) throws Exception {
                    return User.getById(db, userId);
                }
            });
        }

        if (_u == null) {
            final User u = new User();
            u.id = userId;
            u.name = userName;

            if (db != null) {
                u.put(db);
            } else {
                RocketChatDatabaseHelper.write(mContext, new RocketChatDatabaseHelper.DBCallback<Object>() {
                    @Override
                    public Object process(SQLiteDatabase db) throws Exception {
                        u.put(db);
                        return null;
                    }
                });
            }
        }

        m.userId = userId;

        if (!message.isNull("urls")) {
            m.urls = message.getJSONArray("urls").toString();
        } else m.urls = "[]";
        if (!message.isNull("attachments")) {
            m.attachments = message.getJSONArray("attachments").toString();
        } else m.attachments = "[]";
        m.extras = "{}";

        if (db != null) {
            m.put(db);
        } else {
            m.putByContentProvider(mContext);
        }
    }

    public void parseRoom(JSONObject room) throws JSONException {
        final String roomID = room.getString("_id");

        Room r = RocketChatDatabaseHelper.read(mContext, new RocketChatDatabaseHelper.DBCallback<Room>() {
            @Override
            public Room process(SQLiteDatabase db) throws Exception {
                return Room.getById(db, roomID);
            }
        });

        if (r == null) {
            r = new Room();
            r.id = roomID;
        }

        if (!room.isNull("rid")) r.rid = room.getString("rid");
        if (!room.isNull("name")) r.name = room.getString("name");
        if (!room.isNull("ts")) r.timestamp = room.getJSONObject("ts").getLong("$date");
        if (!room.isNull("t")) r.type = Room.Type.getType(room.getString("t"));
        if (!room.isNull("alert")) r.alert = room.getBoolean("alert");
        if (!room.isNull("unread")) r.unread = room.getInt("unread");
        r.putByContentProvider(mContext);
    }

    public void parseUser(final String userID, JSONObject user) throws JSONException {
        User u = RocketChatDatabaseHelper.read(mContext, new RocketChatDatabaseHelper.DBCallback<User>() {
            @Override
            public User process(SQLiteDatabase db) throws Exception {
                return User.getById(db, userID);
            }
        });
        if (u == null) {
            u = new User();
            u.id = userID;
        }

        if (!user.isNull("username")) u.name = user.getString("username");
        if (!user.isNull("status")) u.status = User.Status.getType(user.getString("status"));
        if (!user.isNull("name")) u.displayName = user.getString("name");

        u.putByContentProvider(mContext);
    }
}
