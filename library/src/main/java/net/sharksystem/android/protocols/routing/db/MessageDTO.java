package net.sharksystem.android.protocols.routing.db;

import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SemanticTag;
import net.sharkfw.knowledgeBase.SpatialSemanticTag;
import net.sharkfw.knowledgeBase.TimeSemanticTag;
import net.sharksystem.android.protocols.routing.Utils;

public class MessageDTO {
    private long id;
    private String version;
    private String format;
    private boolean encrypted;
    private String encryptedSessionKey;
    private boolean signed;
    private String signature;
    private long ttl;
    private int sentCopies;
    private int command;
    private SemanticTag topic;
    private SemanticTag type;
    private PeerSemanticTag sender;
    private STSet receivers;
    private PeerSemanticTag receiverPeer;
    private SpatialSemanticTag receiverSpatial;
    private TimeSemanticTag receiverTime;
    private String content;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    public void setEncryptedSessionKey(String encryptedSessionKey) {
        this.encryptedSessionKey = encryptedSessionKey;
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public int getSentCopies() {
        return sentCopies;
    }

    public void setSentCopies(long sentCopies) {
        this.sentCopies = sentCopies;
    }

    public int getCommand() {
        return command;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public SemanticTag getTopic() {
        return this.topic;
    }

    public void setTopic(SemanticTag topic) {
        this.topic = topic;
    }

    public SemanticTag getType() {
        return this.type;
    }

    public void setType(SemanticTag type) {
        this.type = type;
    }

    public PeerSemanticTag getSender() {
        return sender;
    }

    public void setSender(PeerSemanticTag sender) {
        this.sender = sender;
    }

    public STSet getReceivers() {
        return receivers;
    }

    public void setReceivers(STSet receivers) {
        this.receivers = receivers;
    }

    public PeerSemanticTag getReceiverPeer() {
        return receiverPeer;
    }

    public void setReceiverPeer(PeerSemanticTag receiverPeer) {
        this.receiverPeer = receiverPeer;
    }

    public SpatialSemanticTag getReceiverSpatial() {
        return receiverSpatial;
    }

    public void setReceiverSpatial(SpatialSemanticTag receiverSpatial) {
        this.receiverSpatial = receiverSpatial;
    }

    public TimeSemanticTag getReceiverTime() {
        return receiverTime;
    }

    public void setReceiverTime(TimeSemanticTag receiverTime) {
        this.receiverTime = receiverTime;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // TODO Receivers
    public boolean contentEquals(ASIPInMessage other) {
        if (this.sender == null && other.getSender() == null || this.sender != null && this.sender.identical(other.getSender()))
            return true;

        if (this.receiverPeer == null && other.getReceiverPeer() == null || this.receiverPeer != null && this.receiverPeer.identical(other.getReceiverPeer()))
            return true;

        if (this.receiverSpatial == null && other.getReceiverSpatial() == null || this.receiverSpatial != null && this.receiverSpatial.identical(other.getReceiverSpatial()))
            return true;

        if (this.receiverTime == null && other.getReceiverTime() == null || this.receiverTime != null && this.receiverTime.identical(other.getReceiverTime()))
            return true;

        if (this.topic == null && other.getTopic() == null || this.topic != null && this.topic.identical(other.getTopic()))
            return true;

        if (this.type == null && other.getType() == null || this.type != null && this.type.identical(other.getType()))
            return true;

        String otherContent = Utils.getContent(other);
        if (this.content == null && otherContent == null || this.content != null && this.content.equals(otherContent))
            return true;

        return false;
    }
}
