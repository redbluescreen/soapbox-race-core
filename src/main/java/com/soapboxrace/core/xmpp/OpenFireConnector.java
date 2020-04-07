package com.soapboxrace.core.xmpp;

import com.soapboxrace.core.bo.ParameterBO;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * Interacts to OpenFire XMPP server using the Smack library.
 * Capable of sending messages.
 * TODO add presence monitoring + chat logging
 */
@Singleton
public class OpenFireConnector {

    private String ipAddress;

    private Integer port;

    private String engineToken;

    @EJB
    private ParameterBO parameterBO;

    private XMPPTCPConnection connection;

    private static X509TrustManager getX509TrustManager() {
        return new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {

            }

            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {

            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    /**
     * Initializes the {@link OpenFireConnector}.
     */
    @PostConstruct
    public void init() {
        this.ipAddress = parameterBO.getStrParam("XMPP_IP", "127.0.0.1");
        this.port = parameterBO.getIntParam("XMPP_PORT", 5222);
        this.engineToken = parameterBO.getStrParam("OPENFIRE_TOKEN");
    }

    /**
     * Connects to the XMPP server.
     */
    public void connect() {
        this.connection = new XMPPTCPConnection(getConnectionConfiguration());
        try {
            this.connection.connect();
            this.connection.login();

            System.out.println("Logged in to Openfire server!");
        } catch (IOException | SmackException | XMPPException | InterruptedException e) {
            throw new RuntimeException("Failed to connect to Openfire server", e);
        }
    }

    /**
     * Sends a message to a persona.
     *
     * @param msg       The message to send.
     * @param personaId The ID of the persona to send the message to.
     */
    public void send(String msg, Long personaId) {
        Message message = new Message();
        message.setSubject("1337733113377331");
        message.setStanzaId("JN_1234567");
        message.setBody(msg);
        try {
            message.setTo(JidCreate.entityBareFrom("sbrw." + personaId + "@" + ipAddress));
            connection.sendStanza(message);
        } catch (SmackException.NotConnectedException | InterruptedException | XmppStringprepException e) {
            throw new RuntimeException("Failed to send XMPP message", e);
        }
    }

    private XMPPTCPConnectionConfiguration getConnectionConfiguration() {
        try {
            XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword("sbrw.engine.engine", this.engineToken)
                    .setResource("EA_Chat")
                    .setXmppDomain(this.ipAddress)
                    .setPort(this.port)
                    .setCustomX509TrustManager(getX509TrustManager())
                    .setHostnameVerifier((hostname, session) -> true);
            return configBuilder.build();
        } catch (XmppStringprepException exception) {
            throw new RuntimeException("Failed to build XMPP connection configuration");
        }
    }
}
