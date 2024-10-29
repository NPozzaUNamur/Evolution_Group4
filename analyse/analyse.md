# Analyse de l'application *Conversation*

---

## Analyse des tables et des relations

## Fichier contenant les conf

https://github.com/NPozzaUNamur/Evolution_Group4/blob/main/conversations/src/main/java/eu/siacs/conversations/persistance/DatabaseBackend.java

https://github.com/NPozzaUNamur/Evolution_Group4/blob/main/conversations/src/main/java/eu/siacs/conversations/entities/Account.java

https://github.com/NPozzaUNamur/Evolution_Group4/blob/main/conversations/src/main/java/eu/siacs/conversations/entities/Contact.java

https://github.com/NPozzaUNamur/Evolution_Group4/blob/main/conversations/src/main/java/eu/siacs/conversations/entities/Conversation.java

https://github.com/NPozzaUNamur/Evolution_Group4/blob/main/conversations/src/main/java/eu/siacs/conversations/persistance/UnifiedPushDatabase.java

### Schéma des Tables et Relations

```sql
-- Table `accounts`
CREATE TABLE accounts (
    uuid TEXT PRIMARY KEY,
    username TEXT,
    server TEXT,
    password TEXT,
    display_name TEXT,
    status TEXT,
    status_message TEXT,
    rosterversion TEXT,
    options NUMERIC,
    avatar TEXT,
    keys TEXT,
    hostname TEXT,
    resource TEXT,
    pinned_mechanism TEXT,
    pinned_channel_binding TEXT,
    fast_mechanism TEXT,
    fast_token TEXT,
    port NUMERIC
);

-- Table `conversations`
CREATE TABLE conversations (
    uuid TEXT PRIMARY KEY,
    name TEXT,
    contactUuid TEXT,
    accountUuid TEXT,
    contactJid TEXT,
    created NUMERIC,
    status NUMERIC,
    mode NUMERIC,
    attributes TEXT,
    FOREIGN KEY (accountUuid) REFERENCES accounts(uuid)
);

-- Table `messages`
CREATE TABLE messages (
    uuid TEXT PRIMARY KEY,
    conversationUuid TEXT,
    timeSent NUMERIC,
    counterpart TEXT,
    trueCounterpart TEXT,
    body TEXT,
    encryption NUMERIC,
    status NUMERIC,
    type NUMERIC,
    relativeFilePath TEXT,
    serverMsgId TEXT,
    axolotl_fingerprint TEXT,
    carbon INT,
    edited TEXT,
    read NUMERIC,
    oob INT,
    errorMsg TEXT,
    readByMarkers TEXT,
    markable NUMERIC,
    deleted NUMERIC,
    bodyLanguage TEXT,
    occupantId TEXT,
    reactions TEXT,
    remoteMsgId TEXT,
    FOREIGN KEY (conversationUuid) REFERENCES conversations(uuid)
);

-- Table `contacts`
CREATE TABLE contacts (
    accountUuid TEXT,
    servername TEXT,
    systemname TEXT,
    presence_name TEXT,
    jid TEXT,
    pgpkey TEXT,
    photouri TEXT,
    options NUMERIC,
    systemaccount NUMERIC,
    avatar TEXT,
    last_presence TEXT,
    last_time NUMERIC,
    rtpCapability TEXT,
    groups TEXT,
    PRIMARY KEY (jid, accountUuid),
    FOREIGN KEY (accountUuid) REFERENCES accounts(uuid)
);

-- Table `discovery_results`
CREATE TABLE discovery_results (
    hash TEXT PRIMARY KEY,
    ver TEXT,
    result TEXT
);

-- Table `sessions`
CREATE TABLE sessions (
    account TEXT,
    name TEXT,
    device_id INT,
    key TEXT,
    PRIMARY KEY (account, name, device_id)
);

-- Table `prekeys`
CREATE TABLE prekeys (
    account TEXT,
    id INT PRIMARY KEY,
    key TEXT
);

-- Table `signed_prekeys`
CREATE TABLE signed_prekeys (
    account TEXT,
    id INT PRIMARY KEY,
    key TEXT
);

-- Table `identities`
CREATE TABLE identities (
    account TEXT,
    name TEXT,
    ownkey INT,
    fingerprint TEXT,
    certificate BLOB,
    trust TEXT,
    active NUMERIC,
    last_activation NUMERIC,
    key TEXT,
    PRIMARY KEY (account, name)
);

-- Table `presence_templates`
CREATE TABLE presence_templates (
    uuid TEXT PRIMARY KEY,
    last_used NUMERIC,
    message TEXT,
    status TEXT
);

-- Table `resolver_results`
CREATE TABLE resolver_results (
    domain TEXT,
    hostname TEXT,
    ip BLOB,
    priority NUMERIC,
    directTls NUMERIC,
    authenticated NUMERIC,
    port NUMERIC,
    PRIMARY KEY (domain, hostname)
);

-- Table `push`
CREATE TABLE push (
    account TEXT,
    transport TEXT,
    application TEXT NOT NULL,
    instance TEXT NOT NULL UNIQUE,
    endpoint TEXT,
    expiration NUMBER DEFAULT 0,
    FOREIGN KEY (account) REFERENCES accounts(uuid)
);