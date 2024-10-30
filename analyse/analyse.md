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
| <ins>**accountUuid**</ins>: TEXT |
| servername: TEXT |
| systemname: TEXT |
| presence_name: TEXT |
| <ins>**jid**</ins>: TEXT |
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
| id: (jid, accountUuid) |
| ref: accountUuid |


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

##### Visualisation

#### Message
---

##### Création

##### Alteration

##### Visualisation

#### Identity
---

##### Création

##### Alteration

##### Visualisation

#### Session
---

##### Création

##### Alteration

##### Visualisation