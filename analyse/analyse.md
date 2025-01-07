# Analyse de l'application *Conversation*

## Analyse des DB

**Index**
- [History](#history)
    - [Contact](#contact)
    - [Account](#account)
    - [Conversation](#conversation)
    - [Message](#message)
    - [Identity](#identity)
    - [Session](#session)
    - [Prekey](#prekey)
    - [Signed Prekey](#signed-prekey)
    - [Resolver Result](#resolver-result)
    - [Discovery Result](#discovery-result)
    - [Presence Template](#presence-template)
- [Unified Push Distributor](#unified-push-distributor)
    - [Push](#push)
- [Other Construction](#other-construction)

L'application possède deux bases de donnée différentes, *history* et *unified-push-distributor*. La première contient toutes les tables importantes. La deuxième ne contient qu'une seule table.

Les bases de donnée sont implémenter dans le code au travers d'une classe héritant de la classe `SQLiteOpenHelper`. Ceci permet de simplifier la gestion d'une base de données, et premet aussi une gestion de version. La descritpion des tables devra donc tenir compte des requêtes liés à la création de la table dans `SQLiteOpenHelper.onCreate()`, et celles liés à la modification de celle-ci dans `SQLiteOpenHelper.onUpgrade()`

### History
---

Cette base de donnée est liée à la classe DatabaseBackend trouvable dans `conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java ligne 64`.

```java
public class DatabaseBackend extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "history";
    private static final int DATABASE_VERSION = 52;
    // ...
}
```

On y apprend que la base de donnée se nomme `history`, et qu'elle est utilisé dans sa version *52*. 

La version de la base de données est 52

Cette base de données est liée aux features de communication. Elle contient des données relative aux contactes, messages, conversation, etc.

#### Contact
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 71`
```java
import eu.siacs.conversations.entities.Account; //Line 46
import eu.siacs.conversations.entities.Contact; //Line 47
// ...
private static final String CREATE_CONTATCS_STATEMENT = "create table "
    + Contact.TABLENAME + "(" + Contact.ACCOUNT + " TEXT, "
    + Contact.SERVERNAME + " TEXT, " + Contact.SYSTEMNAME + " TEXT,"
    + Contact.PRESENCE_NAME + " TEXT,"
    + Contact.JID + " TEXT," + Contact.KEYS + " TEXT,"
    + Contact.PHOTOURI + " TEXT," + Contact.OPTIONS + " NUMBER,"
    + Contact.SYSTEMACCOUNT + " NUMBER, " + Contact.AVATAR + " TEXT, "
    + Contact.LAST_PRESENCE + " TEXT, " + Contact.LAST_TIME + " NUMBER, "
    + Contact.RTP_CAPABILITY + " TEXT,"
    + Contact.GROUPS + " TEXT, FOREIGN KEY(" + Contact.ACCOUNT + ") REFERENCES "
    + Account.TABLENAME + "(" + Account.UUID
    + ") ON DELETE CASCADE, UNIQUE(" + Contact.ACCOUNT + ", "
    + Contact.JID + ") ON CONFLICT REPLACE);";
// ...
public void onCreate(SQLiteDatabase db) { //Line 219
    // ...
    db.execSQL(CREATE_CONTATCS_STATEMENT); //Line 276
    // ...
}
//...
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/entities/Contact.java`, on peut reconstituer la requête SQL (DDL).

```sql
create table contacts(
    accountUuid TEXT,
    servername TEXT, 
    systemname TEXT,
    presence_name TEXT,
    jid TEXT,
    pgpkey TEXT,
    photouri TEXT,
    options NUMBER,
    systemaccount NUMBER,
    avatar TEXT,
    last_presence TEXT,
    last_time NUMBER, 
    rtpCapability TEXT,
    groups TEXT, 
    
    FOREIGN KEY(accountUuid) REFERENCES accounts(uuid) ON DELETE CASCADE,
    UNIQUE(accountUuid, jid) ON CONFLICT REPLACE
);
```

##### Alteration

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 313`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Contact.TABLENAME + " ADD COLUMN "
            + Contact.AVATAR + " TEXT");
    // ...
}
```

```sql
ALTER TABLE contacts ADD COLUMN avatar TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 323`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Contact.TABLENAME + " ADD COLUMN "
            + Contact.LAST_TIME + " NUMBER");
    db.execSQL("ALTER TABLE " + Contact.TABLENAME + " ADD COLUMN "
            + Contact.LAST_PRESENCE + " TEXT");
    // ...
}
```

```sql
ALTER TABLE contacts ADD COLUMN last_time NUMBER
ALTER TABLE contacts ADD COLUMN last_presence TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 333`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Contact.TABLENAME + " ADD COLUMN "
            + Contact.GROUPS + " TEXT");
    // ...
}
```

```sql
ALTER TABLE contacts ADD COLUMN groups TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 573`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Contact.TABLENAME + " ADD COLUMN " + Contact.PRESENCE_NAME + " TEXT");
    // ...
}
```

```sql
ALTER TABLE contacts ADD COLUMN presence_name TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 576`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Contact.TABLENAME + " ADD COLUMN " + Contact.RTP_CAPABILITY + " TEXT");
    // ...
}
```

```sql
ALTER TABLE contacts ADD COLUMN rtpCapability TEXT
```

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">CONTACTS</p> |
| - |
| accountUuid: TEXT |
| servername: TEXT |
| systemname: TEXT |
| presence_name: TEXT |
| jid: TEXT |
| pgpkey: TEXT |
| photouri: TEXT |
| options: NUMERIC |
| systemaccount: NUMERIC |
| avatar: TEXT |
| last_presence: TEXT |
| last_time: NUMERIC |
| rtpCapability: TEXT |
| groups: TEXT |
| |
| unique: (jid, accountUuid) |
| ref: accountUuid -> accounts(uuid) |


#### Account
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 220`

```java
import eu.siacs.conversations.entities.Account; //Line 46
// ...
db.execSQL("create table " + Account.TABLENAME + "(" + Account.UUID + " TEXT PRIMARY KEY,"
    + Account.USERNAME + " TEXT,"
    + Account.SERVER + " TEXT,"
    + Account.PASSWORD + " TEXT,"
    + Account.DISPLAY_NAME + " TEXT, "
    + Account.STATUS + " TEXT,"
    + Account.STATUS_MESSAGE + " TEXT,"
    + Account.ROSTERVERSION + " TEXT,"
    + Account.OPTIONS + " NUMBER, "
    + Account.AVATAR + " TEXT, "
    + Account.KEYS + " TEXT, "
    + Account.HOSTNAME + " TEXT, "
    + Account.RESOURCE + " TEXT,"
    + Account.PINNED_MECHANISM + " TEXT,"
    + Account.PINNED_CHANNEL_BINDING + " TEXT,"
    + Account.FAST_MECHANISM + " TEXT,"
    + Account.FAST_TOKEN + " TEXT,"
    + Account.PORT + " NUMBER DEFAULT 5222)"
);
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/entities/Account.java` on peut reconstituer la requête SQL (DDL).

```sql
create table accounts(
    uuid TEXT PRIMARY KEY,
    username TEXT,
    server TEXT,
    password TEXT,
    display_name TEXT,
    status TEXT,
    status_message TEXT,
    rosterversion TEXT,
    options NUMBER,
    avatar TEXT,
    keys TEXT,
    hostname TEXT,
    resource TEXT,
    pinned_mechanism TEXT,
    pinned_channel_binding TEXT,
    fast_mechanism TEXT,
    fast_token TEXT,
    port NUMBER DEFAULT 5222
)
```

##### Alteration

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 315`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN "
            + Account.AVATAR + " TEXT");
    // ...
}
```

```sql
ALTER TABLE accounts ADD COLUMN avatar TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 362`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN " + Account.HOSTNAME + " TEXT");
    db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN " + Account.PORT + " NUMBER DEFAULT 5222");
    // ...
}
```

```sql
ALTER TABLE accounts ADD COLUMN hostname TEXT
ALTER TABLE accounts ADD COLUMN port NUMBER DEFAULT 5222
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 366`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN " + Account.STATUS + " TEXT");
    db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN " + Account.STATUS_MESSAGE + " TEXT");
    // ...
}
```

```sql
ALTER TABLE accounts ADD COLUMN status TEXT
ALTER TABLE accounts ADD COLUMN status_message TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 370`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN " + Account.RESOURCE + " TEXT");
    // ...
}
```

```sql
ALTER TABLE accounts ADD COLUMN resource TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 599`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN " + Account.PINNED_MECHANISM + " TEXT");
    db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN " + Account.PINNED_CHANNEL_BINDING + " TEXT");
    // ...
}
```

```sql
ALTER TABLE accounts ADD COLUMN pinned_mechanism TEXT
ALTER TABLE accounts ADD COLUMN pinned_channel_binding TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 603`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN " + Account.FAST_MECHANISM + " TEXT");
    db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN " + Account.FAST_TOKEN + " TEXT");
    // ...
}
```

```sql
ALTER TABLE accounts ADD COLUMN fast_mechanism TEXT
ALTER TABLE accounts ADD COLUMN fast_token TEXT
```

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">ACCOUNT</p> |
| - |
| <ins>**uuid**</ins>: TEXT |
| username: TEXT |
| server: TEXT |
| password: TEXT |
| display_name: TEXT |
| status: TEXT |
| rosterversion: TEXT |
| options: NUMERIC |
| avatar: TEXT |
| keys: TEXT |
| hostname: TEXT |
| resource: TEXT |
| pinned_mechanism: TEXT |
| pinned_channel_binding: TEXT |
| fast_mechanism: TEXT |
| fast_token: TEXT |
| port: NUMERIC |
| |
| id: uuid |

#### Conversation
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 71`
```java
import eu.siacs.conversations.entities.Account; //Line 46
import eu.siacs.conversations.entities.Conversation; //Line 48
// ...
public void onCreate(SQLiteDatabase db) { //Line 219
    // ...
    db.execSQL("create table " + Conversation.TABLENAME + " (" //Line 238
        + Conversation.UUID + " TEXT PRIMARY KEY, " + Conversation.NAME
        + " TEXT, " + Conversation.CONTACT + " TEXT, "
        + Conversation.ACCOUNT + " TEXT, " + Conversation.CONTACTJID
        + " TEXT, " + Conversation.CREATED + " NUMBER, "
        + Conversation.STATUS + " NUMBER, " + Conversation.MODE
        + " NUMBER, " + Conversation.ATTRIBUTES + " TEXT, FOREIGN KEY("
        + Conversation.ACCOUNT + ") REFERENCES " + Account.TABLENAME
        + "(" + Account.UUID + ") ON DELETE CASCADE);");
    // ...
}
//...
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/entities/Conversation.java`, on peut reconstituer la requête SQL (DDL).

```sql
create table conversations (
    uuid TEXT PRIMARY KEY,
    name TEXT,
    contactUuid TEXT,
    accountUuid TEXT,
    contactJid TEXT,
    created NUMBER,
    status NUMBER,
    mode NUMBER,
    attributes TEXT, 
    
    FOREIGN KEY(accountUuid) REFERENCES accounts(uuid) ON DELETE CASCADE
);

```

##### Alteration

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 319`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Conversation.TABLENAME + " ADD COLUMN "
        + Conversation.ATTRIBUTES + " TEXT");
    // ...
}
```

```sql
ALTER TABLE conversations ADD COLUMN attributes TEXT
```

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">CONVERSATION</p> |
| - |
| <ins>**uuid**</ins>: TEXT |
| name: TEXT |
| contactUuid: TEXT |
| accountUuid: TEXT |
| contactJid: TEXT |
| created: NUMERIC |
| status: NUMERIC |
| mode: NUMERIC |
| attributes: TEXT |
| |
| id: uuid |
| ref: accountUuid -> accounts(uuid) |

#### Message
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 247`

```java
import eu.siacs.conversations.entities.Conversation;
import eu.siacs.conversations.entities.Message;      //Line 49
// ...
db.execSQL("create table " + Message.TABLENAME + "( " + Message.UUID
    + " TEXT PRIMARY KEY, " + Message.CONVERSATION + " TEXT, "
    + Message.TIME_SENT + " NUMBER, " + Message.COUNTERPART
    + " TEXT, " + Message.TRUE_COUNTERPART + " TEXT,"
    + Message.BODY + " TEXT, " + Message.ENCRYPTION + " NUMBER, "
    + Message.STATUS + " NUMBER," + Message.TYPE + " NUMBER, "
    + Message.RELATIVE_FILE_PATH + " TEXT, "
    + Message.SERVER_MSG_ID + " TEXT, "
    + Message.FINGERPRINT + " TEXT, "
    + Message.CARBON + " INTEGER, "
    + Message.EDITED + " TEXT, "
    + Message.READ + " NUMBER DEFAULT 1, "
    + Message.OOB + " INTEGER, "
    + Message.ERROR_MESSAGE + " TEXT,"
    + Message.READ_BY_MARKERS + " TEXT,"
    + Message.MARKABLE + " NUMBER DEFAULT 0,"
    + Message.DELETED + " NUMBER DEFAULT 0,"
    + Message.BODY_LANGUAGE + " TEXT,"
    + Message.OCCUPANT_ID + " TEXT,"
    + Message.REACTIONS + " TEXT,"
    + Message.REMOTE_MSG_ID + " TEXT, FOREIGN KEY("
    + Message.CONVERSATION + ") REFERENCES "
    + Conversation.TABLENAME + "(" + Conversation.UUID
    + ") ON DELETE CASCADE);"
);
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/entities/Message.java` on peut reconstituer la requête SQL (DDL).

```sql
create table messages(
    uuid TEXT PRIMARY KEY,
    conversationUuid TEXT,
    timeSent NUMBER,
    counterpart TEXT,
    trueCounterpart TEXT,
    body TEXT,
    encryption NUMBER,
    status NUMBER,
    type NUMBER,
    relativeFilePath TEXT,
    serverMsgId TEXT,
    axolotl_fingerprint TEXT,
    carbon INTEGER,
    edited TEXT,
    read NUMBER DEFAULT 1,
    oob INTEGER,
    errorMsg TEXT,
    readByMarkers TEXT,
    markable NUMBER DEFAULT 0,
    deleted NUMBER DEFAULT 0,
    bodyLanguage TEXT,
    occupantId TEXT,
    reactions TEXT,
    remoteMsgId TEXT, 
    
    FOREIGN KEY(conversationUuid) REFERENCES conversations(uuid) ON DELETE CASCADE);
```

##### Alteration

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 297`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
        + Message.TYPE + " NUMBER");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN type NUMBER
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 307`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
        + Message.TRUE_COUNTERPART + " TEXT");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN trueCounterpart TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 329`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
        + Message.RELATIVE_FILE_PATH + " TEXT");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN relativeFilePath TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 339`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
        + Message.SERVER_MSG_ID + " TEXT");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN serverMsgId TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 351`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
        + Message.FINGERPRINT + " TEXT");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN axolotl_fingerprint TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 355`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
        + Message.CARBON + " INTEGER");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN carbon INTEGER
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 403`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN " + Message.READ + " NUMBER DEFAULT 1");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN read NUMBER DEFAULT 1
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 424`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN " + Message.EDITED + " TEXT");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN edited TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 428`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN " + Message.OOB + " INTEGER");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN oob INTEGER
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 444`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN " + Message.ERROR_MESSAGE + " TEXT");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN errorMsg TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 533`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN " + Message.READ_BY_MARKERS + " TEXT");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN readByMarkers TEXT
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 537`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN " + Message.MARKABLE + " NUMBER DEFAULT 0");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN markable NUMBER DEFAULT 0
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 554`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN " + Message.DELETED + " NUMBER DEFAULT 0");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN deleted NUMBER DEFAULT 0
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 561`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN " + Message.BODY_LANGUAGE);
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN bodyLanguage
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 607`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN " + Message.OCCUPANT_ID + " TEXT");
    db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN " + Message.REACTIONS + " TEXT");
    // ...
}
```

```sql
ALTER TABLE messages ADD COLUMN occupantId TEXT
ALTER TABLE messages ADD COLUMN reactions TEXT
```
##### Index

De multiple index sont créer pour cette table.

```java
private static final String CREATE_MESSAGE_TIME_INDEX = "CREATE INDEX message_time_index ON " + Message.TABLENAME + "(" + Message.TIME_SENT + ")";
```

```sql
CREATE INDEX message_time_index ON messages(timeSent)
```

```java
private static final String CREATE_MESSAGE_CONVERSATION_INDEX = "CREATE INDEX message_conversation_index ON " + Message.TABLENAME + "(" + Message.CONVERSATION + ")";
```

```sql
CREATE INDEX message_conversation_index ON messages(conversationUuid)
```

```java
private static final String CREATE_MESSAGE_DELETED_INDEX = "CREATE INDEX message_deleted_index ON " + Message.TABLENAME + "(" + Message.DELETED + ")";
```

```sql
CREATE INDEX message_deleted_index ON messages(deleted)
```

```java
private static final String CREATE_MESSAGE_RELATIVE_FILE_PATH_INDEX = "CREATE INDEX message_file_path_index ON " + Message.TABLENAME + "(" + Message.RELATIVE_FILE_PATH + ")";
```

```sql
CREATE INDEX message_file_path_index ON messages(relativeFilePath)
```

```java
private static final String CREATE_MESSAGE_TYPE_INDEX = "CREATE INDEX message_type_index ON " + Message.TABLENAME + "(" + Message.TYPE + ")";
```

```sql
CREATE INDEX message_type_index ON messages(type)
```

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">MESSAGE</p> |
| - |
| <ins>**uuid**</ins>: TEXT |
| conversationUuid: TEXT |
| timeSent: NUMBER |
| counterpart: TEXT |
| trueCounterpart: TEXT |
| body: TEXT |
| encryption: NUMBER |
| status: NUMBER |
| type: NUMBER |
| relativeFilePath: TEXT |
| serverMsgId: TEXT |
| axolotl_fingerprint: TEXT |
| carbon: INTEGER |
| edited: TEXT |
| read: NUMBER |
| oob: INTEGER |
| errorMsg: TEXT |
| readByMarkers: TEXT |
| markable: NUMBER |
| deleted: NUMBER |
| bodyLanguage: TEXT |
| occupantId: TEXT |
| reactions: TEXT |
| remoteMsgId: TEXT |
| |
| id: uuid |
| ref: conversationUuid -> conversations(uuid) |

#### Identity
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 139`
```java
import eu.siacs.conversations.crypto.axolotl.SQLiteAxolotlStore; //Line 45
import eu.siacs.conversations.entities.Account; //Line 46
// ...
private static final String CREATE_IDENTITIES_STATEMENT = "CREATE TABLE "
    + SQLiteAxolotlStore.IDENTITIES_TABLENAME + "("
    + SQLiteAxolotlStore.ACCOUNT + " TEXT,  "
    + SQLiteAxolotlStore.NAME + " TEXT, "
    + SQLiteAxolotlStore.OWN + " INTEGER, "
    + SQLiteAxolotlStore.FINGERPRINT + " TEXT, "
    + SQLiteAxolotlStore.CERTIFICATE + " BLOB, "
    + SQLiteAxolotlStore.TRUST + " TEXT, "
    + SQLiteAxolotlStore.ACTIVE + " NUMBER, "
    + SQLiteAxolotlStore.LAST_ACTIVATION + " NUMBER,"
    + SQLiteAxolotlStore.KEY + " TEXT, FOREIGN KEY("
    + SQLiteAxolotlStore.ACCOUNT
    + ") REFERENCES " + Account.TABLENAME + "(" + Account.UUID + ") ON DELETE CASCADE, "
    + "UNIQUE( " + SQLiteAxolotlStore.ACCOUNT + ", "
    + SQLiteAxolotlStore.NAME + ", "
    + SQLiteAxolotlStore.FINGERPRINT
    + ") ON CONFLICT IGNORE"
    + ");";
// ...
public void onCreate(SQLiteDatabase db) { //Line 219
    // ...
    db.execSQL(CREATE_IDENTITIES_STATEMENT); //Line 281
    // ...
}
//...
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/crypto/axolotl/SQLiteAxolotlStore.java`, on peut reconstituer la requête SQL (DDL).

```sql
CREATE TABLE identities(
    account TEXT,
    name TEXT,
    ownkey INTEGER,
    fingerprint TEXT,
    certificate BLOB,
    trust TEXT,
    active NUMBER,
    last_activation NUMBER,
    key TEXT,
    
    FOREIGN KEY(account) REFERENCES accounts(uuid) ON DELETE CASCADE
    UNIQUE( account, name, fingerprint) ON CONFLICT IGNORE
);
```

##### Alteration

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 416`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + SQLiteAxolotlStore.IDENTITIES_TABLENAME + " ADD COLUMN " + SQLiteAxolotlStore.CERTIFICATE);
    // ...
}
```

```sql
ALTER TABLE identities ADD COLUMN certificate
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 447`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
    db.execSQL("ALTER TABLE " + SQLiteAxolotlStore.IDENTITIES_TABLENAME + " ADD COLUMN " + SQLiteAxolotlStore.TRUST + " TEXT");
    db.execSQL("ALTER TABLE " + SQLiteAxolotlStore.IDENTITIES_TABLENAME + " ADD COLUMN " + SQLiteAxolotlStore.ACTIVE + " NUMBER");
    // ...
}
```

```sql
ALTER TABLE identities ADD COLUMN trust TEXT
ALTER TABLE identities ADD COLUMN active NUMBER
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 467`

```java
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //Line 291
    // ...
   db.execSQL("ALTER TABLE " + SQLiteAxolotlStore.IDENTITIES_TABLENAME + " ADD COLUMN " + SQLiteAxolotlStore.LAST_ACTIVATION + " NUMBER");
    // ...
}
```

```sql
ALTER TABLE identities ADD COLUMN last_activation NUMBER
```

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">IDENTITY</p> |
| - |
| account: TEXT |
| name: TEXT |
| ownkey: INTEGER |
| fingerprint: TEXT |
| certificate: BLOB |
| trust: TEXT |
| active: NUMBER |	
| last_activation: NUMBER |
| key: TEXT |
| |
| unique: (account, name, fingerprint) |
| ref: account -> accounts(uuid) |

#### Session
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 125`
```java
import eu.siacs.conversations.crypto.axolotl.SQLiteAxolotlStore; //Line 45
import eu.siacs.conversations.entities.Account; //Line 46
// ...
private static final String CREATE_SESSIONS_STATEMENT = "CREATE TABLE "
    + SQLiteAxolotlStore.SESSION_TABLENAME + "("
    + SQLiteAxolotlStore.ACCOUNT + " TEXT,  "
    + SQLiteAxolotlStore.NAME + " TEXT, "
    + SQLiteAxolotlStore.DEVICE_ID + " INTEGER, "
    + SQLiteAxolotlStore.KEY + " TEXT, FOREIGN KEY("
    + SQLiteAxolotlStore.ACCOUNT
    + ") REFERENCES " + Account.TABLENAME + "(" + Account.UUID + ") ON DELETE CASCADE, "
    + "UNIQUE( " + SQLiteAxolotlStore.ACCOUNT + ", "
    + SQLiteAxolotlStore.NAME + ", "
    + SQLiteAxolotlStore.DEVICE_ID
    + ") ON CONFLICT REPLACE"
    + ");";
// ...
public void onCreate(SQLiteDatabase db) { //Line 219
    // ...
    db.execSQL(CREATE_SESSIONS_STATEMENT); //Line 278
    // ...
}
//...
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/crypto/axolotl/SQLiteAxolotlStore.java`, on peut reconstituer la requête SQL (DDL).

```sql
CREATE TABLE sessions(
    account TEXT,
    name TEXT,
    device_id INTEGER,
    key TEXT,
    
    FOREIGN KEY(account) REFERENCES accounts(uuid) ON DELETE CASCADE,
    UNIQUE( account, name, device_id) ON CONFLICT REPLACE
);
```

##### Alteration

La table ne subit pas d'alteration.

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">SESSION</p> |
| - |
| account: TEXT |
| name: TEXT |
| device_id: TEXT |
| key: TEXT |
| |
| unique: (account, name, device_id) |
| ref: account -> accounts(uuid) |

#### Prekey
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 101`
```java
import eu.siacs.conversations.crypto.axolotl.SQLiteAxolotlStore; //Line 45
import eu.siacs.conversations.entities.Account; //Line 46
// ...
private static final String CREATE_PREKEYS_STATEMENT = "CREATE TABLE "
    + SQLiteAxolotlStore.PREKEY_TABLENAME + "("
    + SQLiteAxolotlStore.ACCOUNT + " TEXT,  "
    + SQLiteAxolotlStore.ID + " INTEGER, "
    + SQLiteAxolotlStore.KEY + " TEXT, FOREIGN KEY("
    + SQLiteAxolotlStore.ACCOUNT
    + ") REFERENCES " + Account.TABLENAME + "(" + Account.UUID + ") ON DELETE CASCADE, "
    + "UNIQUE( " + SQLiteAxolotlStore.ACCOUNT + ", "
    + SQLiteAxolotlStore.ID
    + ") ON CONFLICT REPLACE"
    + ");";
// ...
public void onCreate(SQLiteDatabase db) { //Line 219
    // ...
    db.execSQL(CREATE_PREKEYS_STATEMENT); //Line 279
    // ...
}
//...
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/crypto/axolotl/SQLiteAxolotlStore.java`, on peut reconstituer la requête SQL (DDL).

```sql
CREATE TABLE prekeys(
    account TEXT,
    id INTEGER,
    key TEXT,
    FOREIGN KEY(account) REFERENCES accounts(uuid) ON DELETE CASCADE,
    UNIQUE( account, id) ON CONFLICT REPLACE
);
```

##### Alteration

La table ne subit pas d'alteration.

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">PREKEY</p> |
| - |
| account: TEXT |
| id: INTEGER |
| key: TEXT |
| |
| unique: (account, id) |
| ref: account -> accounts(uuid) |

#### Signed Prekey
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 113`
```java
import eu.siacs.conversations.crypto.axolotl.SQLiteAxolotlStore; //Line 45
import eu.siacs.conversations.entities.Account; //Line 46
// ...
private static final String CREATE_SIGNED_PREKEYS_STATEMENT = "CREATE TABLE "
    + SQLiteAxolotlStore.SIGNED_PREKEY_TABLENAME + "("
    + SQLiteAxolotlStore.ACCOUNT + " TEXT,  "
    + SQLiteAxolotlStore.ID + " INTEGER, "
    + SQLiteAxolotlStore.KEY + " TEXT, FOREIGN KEY("
    + SQLiteAxolotlStore.ACCOUNT
    + ") REFERENCES " + Account.TABLENAME + "(" + Account.UUID + ") ON DELETE CASCADE, "
    + "UNIQUE( " + SQLiteAxolotlStore.ACCOUNT + ", "
    + SQLiteAxolotlStore.ID
    + ") ON CONFLICT REPLACE" +
    ");";
// ...
public void onCreate(SQLiteDatabase db) { //Line 219
    // ...
    db.execSQL(CREATE_SIGNED_PREKEYS_STATEMENT); //Line 280
    // ...
}
//...
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/crypto/axolotl/SQLiteAxolotlStore.java`, on peut reconstituer la requête SQL (DDL).

```sql
CREATE TABLE signed_prekeys(
    account TEXT,
    id INTEGER,
    key TEXT,
    
    FOREIGN KEY(account) REFERENCES accounts(uuid) ON DELETE CASCADE,
    UNIQUE( account, id) ON CONFLICT REPLACE
);

```

##### Alteration

La table ne subit pas d'alteration.

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">SIGNED_PREKEY</p> |
| - |
| account: TEXT |
| id: INTEGER |
| key: TEXT |
| |
| unique: (account, id) |
| ref: account -> accounts(uuid)	|

#### Resolver Result
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 158`
```java
import eu.siacs.conversations.utils.Resolver; // Line 59
// ...
private static final String RESOLVER_RESULTS_TABLENAME = "resolver_results";

private static final String CREATE_RESOLVER_RESULTS_TABLE = "create table " + RESOLVER_RESULTS_TABLENAME + "("
        + Resolver.Result.DOMAIN + " TEXT,"
        + Resolver.Result.HOSTNAME + " TEXT,"
        + Resolver.Result.IP + " BLOB,"
        + Resolver.Result.PRIORITY + " NUMBER,"
        + Resolver.Result.DIRECT_TLS + " NUMBER,"
        + Resolver.Result.AUTHENTICATED + " NUMBER,"
        + Resolver.Result.PORT + " NUMBER,"
        + "UNIQUE(" + Resolver.Result.DOMAIN + ") ON CONFLICT REPLACE"
        + ");";
// ...
public void onCreate(SQLiteDatabase db) { //Line 219
    // ...
    db.execSQL(CREATE_RESOLVER_RESULTS_TABLE); //Line 283
    // ...
}
//...
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/utils/Resolver.java`, on peut reconstituer la requête SQL (DDL).

```sql
create table resolver_results(
    domain TEXT,
    hostname TEXT,
    ip BLOB,
    priority NUMBER,
    directTls NUMBER,
    authenticated NUMBER,
    port NUMBER,
    
    UNIQUE(domain) ON CONFLICT REPLACE
);
```

##### Alteration

La table ne subit pas d'alteration.

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">RESOLVER_RESULT</p> |
| - |
| domain: TEXT |
| hostname: TEXT |
| ip: BLOB |
| priority: NUMBER |
| directTls: NUMBER |
| authenticated: NUMBER |
| port: NUMBER |
| |
| unique: domain |

#### Discovery Result
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 85`
```java
import eu.siacs.conversations.entities.ServiceDiscoveryResult; // Line 52
// ...
private static final String CREATE_DISCOVERY_RESULTS_STATEMENT = "create table "
    + ServiceDiscoveryResult.TABLENAME + "("
    + ServiceDiscoveryResult.HASH + " TEXT, "
    + ServiceDiscoveryResult.VER + " TEXT, "
    + ServiceDiscoveryResult.RESULT + " TEXT, "
    + "UNIQUE(" + ServiceDiscoveryResult.HASH + ", "
    + ServiceDiscoveryResult.VER + ") ON CONFLICT REPLACE);";
// ...
public void onCreate(SQLiteDatabase db) { //Line 219
    // ...
    db.execSQL(CREATE_DISCOVERY_RESULTS_STATEMENT); //Line 277
    // ...
}
//...
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/utils/Resolver.java`, on peut reconstituer la requête SQL (DDL).

```sql
create table discovery_results(
    hash TEXT,
    ver TEXT,
    result TEXT,
    
    UNIQUE(hash, ver) ON CONFLICT REPLACE
);

```

##### Alteration

La table ne subit pas d'alteration.

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">DISCOVERY_RESULT</p> |
| - |
| hash: TEXT |
| ver: TEXT |
| result: TEXT |
| |
| unique: (hash, ver) |

#### Presence Template
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 93`
```java
import eu.siacs.conversations.entities.PresenceTemplate; // Line 50
// ...
private static final String CREATE_PRESENCE_TEMPLATES_STATEMENT = "CREATE TABLE "
    + PresenceTemplate.TABELNAME + "("
    + PresenceTemplate.UUID + " TEXT, "
    + PresenceTemplate.LAST_USED + " NUMBER,"
    + PresenceTemplate.MESSAGE + " TEXT,"
    + PresenceTemplate.STATUS + " TEXT,"
    + "UNIQUE(" + PresenceTemplate.MESSAGE + "," + PresenceTemplate.STATUS + ") ON CONFLICT REPLACE);";
// ...
public void onCreate(SQLiteDatabase db) { //Line 219
    // ...
    db.execSQL(CREATE_PRESENCE_TEMPLATES_STATEMENT); //Line 282
    // ...
}
//...
```

Grâce à `conversations/src/main/java/eu/siacs/conversations/entities/PresenceTemplate.java`, on peut reconstituer la requête SQL (DDL).

```sql
CREATE TABLE presence_templates(
    uuid TEXT,
    last_used NUMBER,
    message TEXT,
    status TEXT,
    
    UNIQUE(message,status) ON CONFLICT REPLACE
);
```

##### Alteration

La table ne subit pas d'alteration.

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">PRESENCE_TEMPLATE</p> |
| - |
| uuid: TEXT |
| last_used: NUMBER |
| message TEXT |
| status: TEXT |
| |
| unique: (message, status) |

### Unified Push Distributor

Cette base de donnée est liée à la classe UnifiedPushDatabase trouvable dans `conversations/src/main/java/eu/siacs/conversations/persistance/UnifiedPushDatabase.java ligne 23`.

```java
public class UnifiedPushDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "unified-push-distributor";
    private static final int DATABASE_VERSION = 1;
    // ...
}
```

On y apprend que la base de donnée se nomme `unified-push-distributor`, et qu'elle est utilisé dans sa version *1*. 

Cette base de données est liée au système de notification. Sa seule table contient les notifications.

#### Push
---

##### Création

`conversations/src/main/java/eu/siacs/conversations/persistance/UnifiedPushDatabase.java line 45`
```java
// ...
public void onCreate(final SQLiteDatabase sqLiteDatabase) {
    sqLiteDatabase.execSQL(
        "CREATE TABLE push (account TEXT, transport TEXT, application TEXT NOT NULL, instance TEXT NOT NULL UNIQUE, endpoint TEXT, expiration NUMBER DEFAULT 0)");
}
//...
```

Plus clairement, ci-dessous l'instruction DDL.

```sql
CREATE TABLE push (
    account TEXT,
    transport TEXT,
    application TEXT NOT NULL,
    instance TEXT NOT NULL UNIQUE,
    endpoint TEXT,
    expiration NUMBER DEFAULT 0
)
```

##### Visualisation

| <p style="text-align: center; padding: 0; margin: 0;">PUSH</p> |
| - |
| account: TEXT |
| transport: TEXT |
| application: TEXT |
| instance: TEXT |
| endpoint: TEXT |
| expiration: TEXT |
| |
| unique: instance |

### Other Construction

Il y a d'autre construction lier à la base de donnée.

#### Virtual Table

Les tables virtuelles sont des constructions propres à SQLite. Elles permettent l'accès à des fonctions de l'application en utilisant l'interface SQL. [Documentation VTables](https://www.sqlite.org/vtab.html)

Dans notre cas, cette table permet de faire de la recherche de texte dans les messages. FTS4 est un module de recherche de texte intégrale, qui intègre un colonne par défaut `rowid`.
[Documentation FTS4](https://www.sqlite.org/fts3.html)

```java
private static final String CREATE_MESSAGE_INDEX_TABLE = "CREATE VIRTUAL TABLE messages_index USING fts4 (uuid,body,notindexed=\"uuid\",content=\"" + Message.TABLENAME + "\",tokenize='unicode61')";
```

```sql
CREATE VIRTUAL TABLE messages_index USING fts4 (uuid,body,notindexed="uuid",content="messages",tokenize='unicode61')
```

#### Trigger

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 178`

```java
private static final String CREATE_MESSAGE_INSERT_TRIGGER = "CREATE TRIGGER after_message_insert AFTER INSERT ON " + Message.TABLENAME + " BEGIN INSERT INTO messages_index(rowid,uuid,body) VALUES(NEW.rowid,NEW.uuid,NEW.body); END;";
```

```sql
CREATE TRIGGER after_message_insert AFTER INSERT ON messages BEGIN INSERT INTO messages_index(rowid,uuid,body) VALUES(NEW.rowid,NEW.uuid,NEW.body); END;
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 179`

```java
private static final String CREATE_MESSAGE_UPDATE_TRIGGER = "CREATE TRIGGER after_message_update UPDATE OF uuid,body ON " + Message.TABLENAME + " BEGIN UPDATE messages_index SET body=NEW.body,uuid=NEW.uuid WHERE rowid=OLD.rowid; END;";
```

```sql
CREATE TRIGGER after_message_update UPDATE OF uuid,body ON messages BEGIN UPDATE messages_index SET body=NEW.body,uuid=NEW.uuid WHERE rowid=OLD.rowid; END;
```

`conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java line 180`

```java
private static final String CREATE_MESSAGE_DELETE_TRIGGER = "CREATE TRIGGER after_message_delete AFTER DELETE ON " + Message.TABLENAME + " BEGIN DELETE FROM messages_index WHERE rowid=OLD.rowid; END;";
```

```sql
CREATE TRIGGER after_message_delete AFTER DELETE ON messages BEGIN DELETE FROM messages_index WHERE rowid=OLD.rowid; END;
```
