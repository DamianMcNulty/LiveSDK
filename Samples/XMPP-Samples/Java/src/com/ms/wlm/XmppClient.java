package com.ms.wlm;

import java.net.URLDecoder;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

/**
 * XmppClient class has all the XMPP specific logic. This class uses smack
 * library to connect to windows live messenger service. For Windows Live Xmpp
 * documentation see <link>
 */
public class XmppClient {

	public static final String Host = "xmpp.messenger.live.com";
	public static final int Port = 5222;
	public static final String Service = "messenger.live.com";

	private String accessToken;
	private XMPPConnection connection;

	/**
	 * This block initializes smack with SASL mechanism used by Windows Live.
	 */
	static {
		SASLAuthentication.registerSASLMechanism("X-MESSENGER-OAUTH2",
				XMessengerOAuth2.class);
		SASLAuthentication.supportSASLMechanism("X-MESSENGER-OAUTH2");
	}

	/**
	 * Constructor
	 * 
	 * @param accessToken
	 *            The OAuth2.0 access token to be used for login.
	 */
	public XmppClient(String accessToken) {
		this.accessToken = accessToken;
	}

	/**
	 * Get the Roster for this client instance.
	 * 
	 * @return The full Roster for the client.
	 */
	public Roster getRoster() {
		return this.connection.getRoster();
	}

	/**
	 * Get the Jid for this client instance.
	 */
	public String getLocalJid() {
		return StringUtils.parseBareAddress(this.connection.getUser());
	}

	/**
	 * Log in the client to the messenger service.
	 */
	public void logIn() {

		// Create a connection. We use service name in config and asmack will do
		// SRV look up locate the xmpp server.
		ConnectionConfiguration connConfig = new ConnectionConfiguration(
				XmppClient.Service);
		connConfig.setRosterLoadedAtLogin(true);
		this.connection = new XMPPConnection(connConfig);

		try {
			this.connection.connect();

			// We do not need user name in this case.
			this.connection.login("", this.accessToken);
		} catch (XMPPException ex) {
			this.connection = null;
			return;
		}

		System.out.println(String.format("Logged in as %s",
				this.connection.getUser()));
		// set the message and presence handlers
		this.setPacketFilters();

		// Set the status to available
		Presence presence = new Presence(Presence.Type.available);
		this.connection.sendPacket(presence);
	}

	/**
	 * Send a text message to the buddy.
	 * 
	 * @param to
	 *            The Buddy Jid.
	 * @param text
	 *            The text message to be sent.
	 */
	public void sendMessage(String to, String text) {
		Message msg = new Message(to, Message.Type.chat);
		msg.setBody(text);
		this.connection.sendPacket(msg);
	}

	/**
	 * Set the packet filters for handling incoming stanzas.
	 */
	private void setPacketFilters() {
		if (this.connection != null) {
			PacketFilter presenceFilter = new PacketTypeFilter(Presence.class);
			this.connection.addPacketListener(new PacketListener() {
				public void processPacket(Packet packet) {
					Presence presence = (Presence) packet;
					handlePresenceReceived(presence);
				}
			}, presenceFilter);

			PacketFilter messageFilter = new MessageTypeFilter(
					Message.Type.chat);
			this.connection.addPacketListener(new PacketListener() {
				public void processPacket(Packet packet) {
					Message message = (Message) packet;
					if (message.getBody() != null) {
						handleMessageReceived(message);
					}
				}
			}, messageFilter);
		}
	}

	/**
	 * Handle the presence stanza received.
	 * 
	 * @param presence
	 *            The received presence stanza.
	 */
	private void handlePresenceReceived(Presence presence) {
		String from = StringUtils.parseBareAddress(presence.getFrom());
		System.out.println(String.format(
				"Presence received from Jid: %s, Name: %s", from,
				this.getContactName(from)));
	}

	/**
	 * Handle the message stanza received.
	 * 
	 * @param message
	 *            The received message stanza.
	 */
	private void handleMessageReceived(Message message) {
		String from = StringUtils.parseBareAddress(message.getFrom());
		System.out.println(String.format(
				"Message received from Jid: %s, Name: %s", from,
				this.getContactName(from)));
	}

	/**
	 * Get friendly name of a contact given the jid.
	 * 
	 * @param jid
	 *            Jid for the target contact.
	 * 
	 * @return The friendly name by looking up roster.
	 */
	private String getContactName(String jid) {
		Roster roster = this.connection.getRoster();
		RosterEntry entry = roster.getEntry(jid);
		return entry.getName();
	}
}
