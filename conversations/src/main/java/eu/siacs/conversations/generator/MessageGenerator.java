package eu.siacs.conversations.generator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.crypto.axolotl.AxolotlService;
import eu.siacs.conversations.crypto.axolotl.XmppAxolotlMessage;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.entities.Conversation;
import eu.siacs.conversations.entities.Conversational;
import eu.siacs.conversations.entities.Message;
import eu.siacs.conversations.services.XmppConnectionService;
import eu.siacs.conversations.xml.Element;
import eu.siacs.conversations.xml.Namespace;
import eu.siacs.conversations.xmpp.Jid;
import eu.siacs.conversations.xmpp.chatstate.ChatState;
import eu.siacs.conversations.xmpp.jingle.JingleConnectionManager;
import eu.siacs.conversations.xmpp.jingle.JingleRtpConnection;
import eu.siacs.conversations.xmpp.jingle.Media;
import eu.siacs.conversations.xmpp.jingle.stanzas.Reason;
import im.conversations.android.xmpp.model.reactions.Reaction;
import im.conversations.android.xmpp.model.reactions.Reactions;

public class MessageGenerator extends AbstractGenerator {
    private static final String OMEMO_FALLBACK_MESSAGE = "I sent you an OMEMO encrypted message but your client doesn’t seem to support that. Find more information on https://conversations.im/omemo";
    private static final String PGP_FALLBACK_MESSAGE = "I sent you a PGP encrypted message but your client doesn’t seem to support that.";

    public MessageGenerator(XmppConnectionService service) {
        super(service);
    }

    private im.conversations.android.xmpp.model.stanza.Message preparePacket(Message message) {
        Conversation conversation = (Conversation) message.getConversation();
        Account account = conversation.getAccount();
        im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        final boolean isWithSelf = conversation.getContact().isSelf();
        if (conversation.getMode() == Conversation.MODE_SINGLE) {
            packet.setTo(message.getCounterpart());
            packet.setType(im.conversations.android.xmpp.model.stanza.Message.Type.CHAT);
            if (!isWithSelf) {
                packet.addChild("request", "urn:xmpp:receipts");
            }
        } else if (message.isPrivateMessage()) {
            packet.setTo(message.getCounterpart());
            packet.setType(im.conversations.android.xmpp.model.stanza.Message.Type.CHAT);
            packet.addChild("x", "http://jabber.org/protocol/muc#user");
            packet.addChild("request", "urn:xmpp:receipts");
        } else {
            packet.setTo(message.getCounterpart().asBareJid());
            packet.setType(im.conversations.android.xmpp.model.stanza.Message.Type.GROUPCHAT);
        }
        if (conversation.isSingleOrPrivateAndNonAnonymous() && !message.isPrivateMessage()) {
            packet.addChild("markable", "urn:xmpp:chat-markers:0");
        }
        packet.setFrom(account.getJid());
        packet.setId(message.getUuid());
        if (conversation.getMode() == Conversational.MODE_SINGLE || message.isPrivateMessage() || !conversation.getMucOptions().stableId()) {
            packet.addChild("origin-id", Namespace.STANZA_IDS).setAttribute("id", message.getUuid());
        }
        if (message.edited()) {
            packet.addChild("replace", "urn:xmpp:message-correct:0").setAttribute("id", message.getEditedIdWireFormat());
        }
        return packet;
    }

    public void addDelay(im.conversations.android.xmpp.model.stanza.Message packet, long timestamp) {
        final SimpleDateFormat mDateFormat = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Element delay = packet.addChild("delay", "urn:xmpp:delay");
        Date date = new Date(timestamp);
        delay.setAttribute("stamp", mDateFormat.format(date));
    }

    public im.conversations.android.xmpp.model.stanza.Message generateAxolotlChat(Message message, XmppAxolotlMessage axolotlMessage) {
        im.conversations.android.xmpp.model.stanza.Message packet = preparePacket(message);
        if (axolotlMessage == null) {
            return null;
        }
        packet.setAxolotlMessage(axolotlMessage.toElement());
        packet.setBody(OMEMO_FALLBACK_MESSAGE);
        packet.addChild("store", "urn:xmpp:hints");
        packet.addChild("encryption", "urn:xmpp:eme:0")
                .setAttribute("name", "OMEMO")
                .setAttribute("namespace", AxolotlService.PEP_PREFIX);
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message generateKeyTransportMessage(Jid to, XmppAxolotlMessage axolotlMessage) {
        im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setType(im.conversations.android.xmpp.model.stanza.Message.Type.CHAT);
        packet.setTo(to);
        packet.setAxolotlMessage(axolotlMessage.toElement());
        packet.addChild("store", "urn:xmpp:hints");
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message generateChat(Message message) {
        im.conversations.android.xmpp.model.stanza.Message packet = preparePacket(message);
        String content;
        if (message.hasFileOnRemoteHost()) {
            final Message.FileParams fileParams = message.getFileParams();
            content = fileParams.url;
            packet.addChild("x", Namespace.OOB).addChild("url").setContent(content);
        } else {
            content = message.getBody();
        }
        packet.setBody(content);
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message generatePgpChat(Message message) {
        final im.conversations.android.xmpp.model.stanza.Message packet = preparePacket(message);
        if (message.hasFileOnRemoteHost()) {
            Message.FileParams fileParams = message.getFileParams();
            final String url = fileParams.url;
            packet.setBody(url);
            packet.addChild("x", Namespace.OOB).addChild("url").setContent(url);
        } else {
            if (Config.supportUnencrypted()) {
                packet.setBody(PGP_FALLBACK_MESSAGE);
            }
            if (message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
                packet.addChild("x", "jabber:x:encrypted").setContent(message.getEncryptedBody());
            } else if (message.getEncryption() == Message.ENCRYPTION_PGP) {
                packet.addChild("x", "jabber:x:encrypted").setContent(message.getBody());
            }
            packet.addChild("encryption", "urn:xmpp:eme:0")
                    .setAttribute("namespace", "jabber:x:encrypted");
        }
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message generateChatState(Conversation conversation) {
        final Account account = conversation.getAccount();
        final im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setType(conversation.getMode() == Conversation.MODE_MULTI ? im.conversations.android.xmpp.model.stanza.Message.Type.GROUPCHAT : im.conversations.android.xmpp.model.stanza.Message.Type.CHAT);
        packet.setTo(conversation.getJid().asBareJid());
        packet.setFrom(account.getJid());
        packet.addChild(ChatState.toElement(conversation.getOutgoingChatState()));
        packet.addChild("no-store", "urn:xmpp:hints");
        packet.addChild("no-storage", "urn:xmpp:hints"); //wrong! don't copy this. Its *store*
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message confirm(final Message message) {
        final boolean groupChat = message.getConversation().getMode() == Conversational.MODE_MULTI;
        final Jid to = message.getCounterpart();
        final im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setType(groupChat ? im.conversations.android.xmpp.model.stanza.Message.Type.GROUPCHAT : im.conversations.android.xmpp.model.stanza.Message.Type.CHAT);
        packet.setTo(groupChat ? to.asBareJid() : to);
        final Element displayed = packet.addChild("displayed", "urn:xmpp:chat-markers:0");
        if (groupChat) {
            final String stanzaId = message.getServerMsgId();
            if (stanzaId != null) {
                displayed.setAttribute("id", stanzaId);
            } else {
                displayed.setAttribute("sender", to.toString());
                displayed.setAttribute("id", message.getRemoteMsgId());
            }
        } else {
            displayed.setAttribute("id", message.getRemoteMsgId());
        }
        packet.addChild("store", "urn:xmpp:hints");
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message reaction(final Conversational conversation, final String reactingTo, final Collection<String> ourReactions) {
        final boolean groupChat = conversation.getMode() == Conversational.MODE_MULTI;
        final Jid to = conversation.getJid().asBareJid();
        final im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setType(groupChat ? im.conversations.android.xmpp.model.stanza.Message.Type.GROUPCHAT : im.conversations.android.xmpp.model.stanza.Message.Type.CHAT);
        packet.setTo(to);
        final var reactions = packet.addExtension(new Reactions());
        reactions.setId(reactingTo);
        for(final String ourReaction : ourReactions) {
            reactions.addExtension(new Reaction(ourReaction));
        }
        packet.addChild("store", "urn:xmpp:hints");
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message conferenceSubject(Conversation conversation, String subject) {
        im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setType(im.conversations.android.xmpp.model.stanza.Message.Type.GROUPCHAT);
        packet.setTo(conversation.getJid().asBareJid());
        packet.addChild("subject").setContent(subject);
        packet.setFrom(conversation.getAccount().getJid().asBareJid());
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message directInvite(final Conversation conversation, final Jid contact) {
        im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setType(im.conversations.android.xmpp.model.stanza.Message.Type.NORMAL);
        packet.setTo(contact);
        packet.setFrom(conversation.getAccount().getJid());
        Element x = packet.addChild("x", "jabber:x:conference");
        x.setAttribute("jid", conversation.getJid().asBareJid());
        String password = conversation.getMucOptions().getPassword();
        if (password != null) {
            x.setAttribute("password", password);
        }
        if (contact.isFullJid()) {
            packet.addChild("no-store", "urn:xmpp:hints");
            packet.addChild("no-copy", "urn:xmpp:hints");
        }
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message invite(final Conversation conversation, final Jid contact) {
        final var packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setTo(conversation.getJid().asBareJid());
        packet.setFrom(conversation.getAccount().getJid());
        Element x = new Element("x");
        x.setAttribute("xmlns", "http://jabber.org/protocol/muc#user");
        Element invite = new Element("invite");
        invite.setAttribute("to", contact.asBareJid());
        x.addChild(invite);
        packet.addChild(x);
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message received(Account account, final Jid from, final String id, ArrayList<String> namespaces, im.conversations.android.xmpp.model.stanza.Message.Type type) {
        final var receivedPacket =
                new im.conversations.android.xmpp.model.stanza.Message();
        receivedPacket.setType(type);
        receivedPacket.setTo(from);
        receivedPacket.setFrom(account.getJid());
        for (final String namespace : namespaces) {
            receivedPacket.addChild("received", namespace).setAttribute("id", id);
        }
        receivedPacket.addChild("store", "urn:xmpp:hints");
        return receivedPacket;
    }

    public im.conversations.android.xmpp.model.stanza.Message received(Account account, Jid to, String id) {
        im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setFrom(account.getJid());
        packet.setTo(to);
        packet.addChild("received", "urn:xmpp:receipts").setAttribute("id", id);
        packet.addChild("store", "urn:xmpp:hints");
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message sessionFinish(
            final Jid with, final String sessionId, final Reason reason) {
        final im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setType(im.conversations.android.xmpp.model.stanza.Message.Type.CHAT);
        packet.setTo(with);
        final Element finish = packet.addChild("finish", Namespace.JINGLE_MESSAGE);
        finish.setAttribute("id", sessionId);
        final Element reasonElement = finish.addChild("reason", Namespace.JINGLE);
        reasonElement.addChild(reason.toString());
        packet.addChild("store", "urn:xmpp:hints");
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message sessionProposal(final JingleConnectionManager.RtpSessionProposal proposal) {
        final im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setType(im.conversations.android.xmpp.model.stanza.Message.Type.CHAT); //we want to carbon copy those
        packet.setTo(proposal.with);
        packet.setId(JingleRtpConnection.JINGLE_MESSAGE_PROPOSE_ID_PREFIX + proposal.sessionId);
        final Element propose = packet.addChild("propose", Namespace.JINGLE_MESSAGE);
        propose.setAttribute("id", proposal.sessionId);
        for (final Media media : proposal.media) {
            propose.addChild("description", Namespace.JINGLE_APPS_RTP).setAttribute("media", media.toString());
        }
        packet.addChild("request", "urn:xmpp:receipts");
        packet.addChild("store", "urn:xmpp:hints");
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message sessionRetract(final JingleConnectionManager.RtpSessionProposal proposal) {
        final im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setType(im.conversations.android.xmpp.model.stanza.Message.Type.CHAT); //we want to carbon copy those
        packet.setTo(proposal.with);
        final Element propose = packet.addChild("retract", Namespace.JINGLE_MESSAGE);
        propose.setAttribute("id", proposal.sessionId);
        propose.addChild("description", Namespace.JINGLE_APPS_RTP);
        packet.addChild("store", "urn:xmpp:hints");
        return packet;
    }

    public im.conversations.android.xmpp.model.stanza.Message sessionReject(final Jid with, final String sessionId) {
        final im.conversations.android.xmpp.model.stanza.Message packet = new im.conversations.android.xmpp.model.stanza.Message();
        packet.setType(im.conversations.android.xmpp.model.stanza.Message.Type.CHAT); //we want to carbon copy those
        packet.setTo(with);
        final Element propose = packet.addChild("reject", Namespace.JINGLE_MESSAGE);
        propose.setAttribute("id", sessionId);
        propose.addChild("description", Namespace.JINGLE_APPS_RTP);
        packet.addChild("store", "urn:xmpp:hints");
        return packet;
    }
}
